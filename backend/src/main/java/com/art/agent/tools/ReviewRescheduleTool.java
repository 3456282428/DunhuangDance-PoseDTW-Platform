package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.service.ScheduleService;
import com.art.vo.RescheduleRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具：管理端审批调课申请（通过 / 拒绝）。
 * 通过即触发排课时间回写与教师通知；该工具本身承载用户的审批决定，不再二次确认。
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ReviewRescheduleTool implements AgentTool {

    private final ScheduleService scheduleService;

    @Override
    public String name() { return "review_reschedule"; }

    @Override
    public String description() {
        return "审批一条调课申请：approve 通过（自动回写课表并通知教师）或 reject 拒绝（通知申请人）"
                + "；申请编号来自 adjust_schedule 的返回结果";
    }

    @Override
    public String parametersSchema() {
        return "{\"requestId\":{\"type\":\"integer\",\"desc\":\"调课申请编号\",\"required\":true},"
                + "\"decision\":{\"type\":\"string\",\"desc\":\"审批决定：approve=通过，reject=拒绝\",\"required\":true},"
                + "\"remark\":{\"type\":\"string\",\"desc\":\"审批意见（拒绝时建议填写原因）\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer requestId = ToolArgs.integer(args, "requestId");
        String decision = ToolArgs.str(args, "decision");
        if (requestId == null || decision == null) {
            return ToolResult.fail("缺少必填参数（requestId/decision）",
                    "请告知申请编号以及是「通过」还是「拒绝」");
        }
        boolean approve;
        if ("approve".equalsIgnoreCase(decision) || "通过".equals(decision)) {
            approve = true;
        } else if ("reject".equalsIgnoreCase(decision) || "拒绝".equals(decision)
                || "驳回".equals(decision) || "不通过".equals(decision)) {
            approve = false;
        } else {
            return ToolResult.fail("decision 只能是 approve（通过）或 reject（拒绝）",
                    "请明确审批结论");
        }
        String remark = ToolArgs.str(args, "remark");

        if (approve) {
            scheduleService.approveReschedule(requestId, remark);
        } else {
            scheduleService.rejectReschedule(requestId, remark);
        }

        RescheduleRequestVO info = findRequest(requestId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", requestId);
        data.put("decision", approve ? "approve" : "reject");
        data.put("status", approve ? "已通过" : "已拒绝");
        data.put("remark", remark);
        if (info != null) {
            data.put("scheduleId", info.getScheduleId());
            data.put("className", info.getClassName());
            data.put("teacherName", info.getTeacherName());
            data.put("original", info.getOriginalDate() + " " + info.getOriginalStartTime()
                    + "-" + info.getOriginalEndTime());
            data.put("adjustedTo", info.getNewDate() + " " + info.getNewStartTime()
                    + "-" + info.getNewEndTime());
        }
        String tail = approve
                ? "课表时间已回写，授课教师将收到通知"
                : "申请已驳回" + (remark != null ? "（原因：" + remark + "）" : "，授课教师将收到通知");
        String scope = info != null
                ? "（排课#" + info.getScheduleId()
                        + (info.getClassName() != null ? " " + info.getClassName() : "") + "）"
                : "";
        return ToolResult.ok("调课申请 " + requestId + scope + " 已" + (approve ? "通过" : "拒绝") + "，" + tail,
                data);
    }

    /** 审批后反查申请详情用于结果展示；查不到不影响主流程结果 */
    private RescheduleRequestVO findRequest(Integer requestId) {
        try {
            for (RescheduleRequestVO vo : scheduleService.listRescheduleRequests(null)) {
                if (requestId.equals(vo.getRequestId())) return vo;
            }
        } catch (Exception ignore) {
            // 列表查询失败仅影响展示详情，审批本身已成功
        }
        return null;
    }
}
