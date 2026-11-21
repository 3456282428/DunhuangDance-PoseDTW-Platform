package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.CourseSchedule;
import com.art.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具：管理端发起调课申请（写操作，需用户确认后执行）。
 * 仅生成「待审核」申请，不直接改排课；审批通过后由审批流程回写时间。
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class AdjustScheduleTool implements AgentTool {

    private final ScheduleService scheduleService;

    @Override
    public String name() { return "adjust_schedule"; }

    @Override
    public String description() {
        return "针对某一节已存在的排课发起调课申请（改到新日期/新时段），生成待审核申请，"
                + "不会立即改课表，需再用 review_reschedule 审批";
    }

    @Override
    public String parametersSchema() {
        return "{\"scheduleId\":{\"type\":\"integer\",\"desc\":\"要调整的排课ID（可用 list_schedules 查询）\",\"required\":true},"
                + "\"newDate\":{\"type\":\"string\",\"desc\":\"新上课日期 yyyy-MM-dd\",\"required\":true},"
                + "\"newStartTime\":{\"type\":\"string\",\"desc\":\"新开始时间 HH:mm\",\"required\":true},"
                + "\"newEndTime\":{\"type\":\"string\",\"desc\":\"新结束时间 HH:mm，必须晚于开始时间\",\"required\":true},"
                + "\"reason\":{\"type\":\"string\",\"desc\":\"调课原因\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "将生成一条待审核调课申请；审批通过前课程时间不变，通过后才会回写课表并通知教师。"; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer scheduleId = ToolArgs.integer(args, "scheduleId");
        String dateStr = ToolArgs.str(args, "newDate");
        String startStr = ToolArgs.str(args, "newStartTime");
        String endStr = ToolArgs.str(args, "newEndTime");
        if (scheduleId == null || dateStr == null || startStr == null || endStr == null) {
            return ToolResult.fail("缺少必填参数（scheduleId/newDate/newStartTime/newEndTime）",
                    "请向用户确认要调的是哪节课（可用 list_schedules 查ID）以及新日期与时段");
        }
        LocalDate newDate;
        LocalTime newStart;
        LocalTime newEnd;
        try {
            newDate = LocalDate.parse(dateStr);
            newStart = LocalTime.parse(normalizeTime(startStr));
            newEnd = LocalTime.parse(normalizeTime(endStr));
        } catch (Exception e) {
            return ToolResult.fail("日期或时间格式不正确",
                    "日期用 yyyy-MM-dd（如 2026-10-20），时间用 HH:mm（如 14:00）");
        }
        if (!newEnd.isAfter(newStart)) {
            return ToolResult.fail("结束时间必须晚于开始时间", "当前：" + newStart + " - " + newEnd);
        }
        String reason = ToolArgs.str(args, "reason", "管理员通过AI助手发起调课");

        CourseSchedule before = scheduleService.getById(scheduleId);
        Integer requestId = scheduleService.applyReschedule(scheduleId, newDate, newStart, newEnd, reason);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", requestId);
        data.put("scheduleId", scheduleId);
        data.put("originalDate", before != null ? String.valueOf(before.getCourseDate()) : null);
        data.put("originalTime", before != null ? before.getStartTime() + "-" + before.getEndTime() : null);
        data.put("newDate", String.valueOf(newDate));
        data.put("newTime", newStart + "-" + newEnd);
        data.put("status", "待审核");
        return ToolResult.ok("已为排课#" + scheduleId + "发起调课申请（申请编号 " + requestId
                + "）：" + (before != null ? before.getCourseDate() + " " + before.getStartTime() + "-" + before.getEndTime() : "")
                + " → " + newDate + " " + newStart + "-" + newEnd + "，当前状态【待审核】，"
                + "可回复「通过/拒绝申请" + requestId + "」完成审批", data);
    }

    /** 兼容 9:00 / 9:30 这类缺前导零的时间写法 */
    private static String normalizeTime(String t) {
        String s = t.trim();
        if (s.length() == 4 && s.charAt(1) == ':') s = "0" + s;
        return s;
    }
}
