package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.CourseClass;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.service.DanceAiService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具：对学生作业提交触发AI智能评分（教师端敏感操作，需用户确认；
 * 强制校验提交归属本人班级，评分结果留存复用）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class TriggerAiScoreTool implements AgentTool {

    private final DanceAiService danceAiService;
    private final HomeworkSubmissionMapper submissionMapper;
    private final HomeworkMapper homeworkMapper;
    private final CourseClassMapper classMapper;

    @Override
    public String name() { return "trigger_ai_score"; }

    @Override
    public String description() { return "对指定学生作业提交发起AI智能评分（约1分钟出结果）；已有评分时默认复用，可强制重评"; }

    @Override
    public String parametersSchema() {
        return "{\"submissionId\":{\"type\":\"int\",\"desc\":\"作业提交ID（可用list_homework_submissions获取），必填\",\"required\":true},"
                + "\"force\":{\"type\":\"boolean\",\"desc\":\"是否强制重评并覆盖已有AI评分，默认false\",\"required\":false}}";
    }

    @Override
    public boolean requiresConfirmation() { return true; }

    @Override
    public String riskHint() { return "AI评分将调用评分服务对视频做完整分析（约1分钟）；强制重评会覆盖已留存的AI评分结果。"; }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    /** 校验提交归属当前教师班级，返回错误结果或null（通过） */
    private ToolResult checkOwnership(Integer submissionId) {
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return ToolResult.fail("作业提交 " + submissionId + " 不存在", "请先用 list_homework_submissions 获取提交ID");
        }
        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        CourseClass cc = homework == null ? null : classMapper.selectById(homework.getClassId());
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (cc == null || teacherId == null || !teacherId.equals(cc.getTeacherId())) {
            return ToolResult.fail("该提交不属于您的授课班级", "仅可对本人班级的学生提交发起AI评分");
        }
        return null;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer submissionId = ToolArgs.integer(args, "submissionId");
        if (submissionId == null) return ToolResult.fail("缺少作业提交ID", "请提供 submissionId 参数");
        ToolResult denied = checkOwnership(submissionId);
        if (denied != null) return denied;

        boolean force = "true".equalsIgnoreCase(String.valueOf(args.getOrDefault("force", "false")));
        Map<String, Object> result = danceAiService.triggerScore(submissionId, force);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("submissionId", submissionId);
        data.put("aiStatus", result.get("aiStatus"));
        if (result.get("aiScore") != null) data.put("aiScore", result.get("aiScore"));

        if ("已完成".equals(result.get("aiStatus"))) {
            return ToolResult.ok("该提交已有留存的AI评分结果：" + result.get("aiScore")
                    + " 分（复用结果，未重复评分）。如需重新评分请以 force=true 发起。", data);
        }
        return ToolResult.ok("AI评分任务已提交，正在评分中（约1分钟）。稍后可用 get_ai_score_result 查询提交 "
                + submissionId + " 的评分结果。", data);
    }
}
