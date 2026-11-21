package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.service.StudentHomeworkService;
import com.art.util.SecurityUtil;
import com.art.vo.StudentHomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具：查询当前学生本人某份作业详情与成绩反馈（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class GetMyHomeworkDetailTool implements AgentTool {

    private final StudentHomeworkService homeworkService;

    @Override
    public String name() { return "get_my_homework_detail"; }

    @Override
    public String description() { return "查询我的某份作业详情（题目要求/提交时间/教师评分与评语/AI评分与状态），需提供作业ID"; }

    @Override
    public String parametersSchema() {
        return "{\"homeworkId\":{\"type\":\"int\",\"desc\":\"作业ID（可用list_my_homework_tasks获取），必填\",\"required\":true}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer homeworkId = ToolArgs.integer(args, "homeworkId");
        if (homeworkId == null) return ToolResult.fail("缺少作业ID", "请提供 homeworkId 参数");
        StudentHomeworkVO vo = homeworkService.getHomeworkDetail(homeworkId);
        if (vo == null) return ToolResult.fail("未找到该作业或无权查看", "请先用 list_my_homework_tasks 确认作业ID");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("homeworkId", vo.getHomeworkId());
        data.put("title", vo.getTitle());
        data.put("className", vo.getClassName());
        data.put("status", vo.getStatus());
        data.put("submitTime", vo.getSubmitTime() == null ? null : String.valueOf(vo.getSubmitTime()));
        data.put("score", vo.getScore());
        data.put("teacherComment", vo.getTeacherComment());
        data.put("aiScore", vo.getAiScore());
        data.put("aiStatus", vo.getAiStatus());

        StringBuilder sb = new StringBuilder("作业【").append(vo.getTitle()).append("】（").append(vo.getClassName()).append("）");
        sb.append("\n· 状态：").append(vo.getStatus());
        if (vo.getSubmitTime() != null) sb.append("，提交时间：").append(vo.getSubmitTime());
        if (vo.getScore() != null) sb.append("\n· 教师评分：").append(vo.getScore()).append(" 分");
        if (vo.getTeacherComment() != null && !vo.getTeacherComment().isBlank()) {
            sb.append("\n· 教师评语：").append(vo.getTeacherComment());
        }
        if (vo.getAiScore() != null) sb.append("\n· AI智能评分：").append(vo.getAiScore()).append(" 分");
        if (vo.getAiStatus() != null) sb.append("（").append(vo.getAiStatus()).append("）");
        if ("未提交".equals(vo.getStatus())) {
            sb.append("\n· 提醒：作业尚未提交，请到「我的作业」页面上传练习视频完成提交。");
        }
        return ToolResult.ok(sb.toString(), data);
    }
}
