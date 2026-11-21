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
 * 工具：查询学生作业提交的AI评分结果与实时进度（教师端，强制校验归属本人班级）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class AiScoreResultTool implements AgentTool {

    private final DanceAiService danceAiService;
    private final HomeworkSubmissionMapper submissionMapper;
    private final HomeworkMapper homeworkMapper;
    private final CourseClassMapper classMapper;

    @Override
    public String name() { return "get_ai_score_result"; }

    @Override
    public String description() { return "查询某份学生作业提交的AI评分结果；评分中则返回实时进度与阶段"; }

    @Override
    public String parametersSchema() {
        return "{\"submissionId\":{\"type\":\"int\",\"desc\":\"作业提交ID，必填\",\"required\":true}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer submissionId = ToolArgs.integer(args, "submissionId");
        if (submissionId == null) return ToolResult.fail("缺少作业提交ID", "请提供 submissionId 参数");

        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return ToolResult.fail("作业提交 " + submissionId + " 不存在", "请核对提交ID");
        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        CourseClass cc = homework == null ? null : classMapper.selectById(homework.getClassId());
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (cc == null || teacherId == null || !teacherId.equals(cc.getTeacherId())) {
            return ToolResult.fail("该提交不属于您的授课班级", "仅可查询本人班级的AI评分结果");
        }

        Map<String, Object> result = danceAiService.getAiResult(submissionId);
        Object status = result.get("aiStatus");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("submissionId", submissionId);
        data.put("aiStatus", status);
        data.put("aiScore", result.get("aiScore"));
        data.put("aiProgress", result.get("aiProgress"));
        data.put("aiStage", result.get("aiStage"));
        data.put("aiScoredAt", result.get("aiScoredAt"));

        if (status == null) {
            return ToolResult.ok("该提交尚未发起AI评分，可用 trigger_ai_score 发起。", data);
        }
        return switch (String.valueOf(status)) {
            case "评分中" -> ToolResult.ok("AI评分进行中：进度 " + result.get("aiProgress") + "%（"
                    + result.get("aiStage") + "）。", data);
            case "已完成" -> ToolResult.ok("AI评分已完成：" + result.get("aiScore") + " 分（评分时间："
                    + result.get("aiScoredAt") + "）。详细报告可在教师端作业模块查看。", data);
            default -> ToolResult.ok("AI评分状态：" + status + "。", data);
        };
    }
}
