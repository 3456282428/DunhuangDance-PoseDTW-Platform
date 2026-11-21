package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.service.ScheduleService;
import com.art.util.SecurityUtil;
import com.art.vo.CourseScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前教师本人的排课（教师端，数据范围仅限本人）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMySchedulesTool implements AgentTool {

    private final ScheduleService scheduleService;

    @Override
    public String name() { return "list_my_schedules"; }

    @Override
    public String description() { return "查询我（当前教师）在某日期范围内的排课（日期/时间/班级/教室/状态），默认今天起7天"; }

    @Override
    public String parametersSchema() {
        return "{\"startDate\":{\"type\":\"string\",\"desc\":\"开始日期yyyy-MM-dd，默认今天\",\"required\":false},"
                + "\"endDate\":{\"type\":\"string\",\"desc\":\"结束日期yyyy-MM-dd，默认开始日期后7天\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return ToolResult.fail("未找到当前教师的档案信息", "请确认账号已关联教师档案");
        LocalDate start;
        LocalDate end;
        try {
            String s = ToolArgs.str(args, "startDate");
            String e = ToolArgs.str(args, "endDate");
            start = s == null ? LocalDate.now() : LocalDate.parse(s);
            end = e == null ? start.plusDays(7) : LocalDate.parse(e);
        } catch (Exception ex) {
            return ToolResult.fail("日期格式不正确", "请使用 yyyy-MM-dd 格式，例如 2026-10-16");
        }
        List<CourseScheduleVO> list = scheduleService.listSchedules(start, end, teacherId, null);
        int show = Math.min(list.size(), 10);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < show; i++) {
            CourseScheduleVO v = list.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", String.valueOf(v.getCourseDate()));
            m.put("time", v.getStartTime() + "-" + v.getEndTime());
            m.put("className", v.getClassName());
            m.put("classroomName", v.getClassroomName());
            m.put("status", v.getStatus());
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder(String.valueOf(start)).append(" 至 ").append(end)
                .append(" 您共 ").append(list.size()).append(" 节排课，前 ").append(show).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("date")).append(" ").append(r.get("time"))
                    .append(" ").append(r.get("className")).append(" @").append(r.get("classroomName"))
                    .append("（").append(r.get("status")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", list.size(), "rows", rows));
    }
}
