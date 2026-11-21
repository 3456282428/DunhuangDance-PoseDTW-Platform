package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.entity.CourseClass;
import com.art.entity.Homework;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.HomeworkMapper;
import com.art.service.TeacherHomeworkService;
import com.art.util.SecurityUtil;
import com.art.vo.HomeworkSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询某份作业的学生提交明细（教师端，强制校验作业归属本人班级）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListHomeworkSubmissionsTool implements AgentTool {

    private final TeacherHomeworkService homeworkService;
    private final HomeworkMapper homeworkMapper;
    private final CourseClassMapper classMapper;

    @Override
    public String name() { return "list_homework_submissions"; }

    @Override
    public String description() { return "查询某份作业的学生提交明细（学员/提交时间/教师评分/AI评分/状态），需提供作业ID"; }

    @Override
    public String parametersSchema() {
        return "{\"homeworkId\":{\"type\":\"int\",\"desc\":\"作业ID（可用list_my_homework获取），必填\",\"required\":true}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    /** 校验作业归属当前教师，返回错误结果或null（通过） */
    private ToolResult checkOwnership(Integer homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) return ToolResult.fail("作业 " + homeworkId + " 不存在", "请先用 list_my_homework 查看您的作业ID");
        CourseClass cc = classMapper.selectById(homework.getClassId());
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (cc == null || teacherId == null || !teacherId.equals(cc.getTeacherId())) {
            return ToolResult.fail("作业 " + homeworkId + " 不属于您的授课班级", "仅可查询本人布置的作业");
        }
        return null;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer homeworkId = ToolArgs.integer(args, "homeworkId");
        if (homeworkId == null) return ToolResult.fail("缺少作业ID", "请提供 homeworkId 参数");
        ToolResult denied = checkOwnership(homeworkId);
        if (denied != null) return denied;

        List<HomeworkSubmissionVO> subs = homeworkService.getSubmissions(homeworkId);
        List<Map<String, Object>> rows = new ArrayList<>();
        int submitted = 0;
        int graded = 0;
        for (HomeworkSubmissionVO s : subs) {
            if (!"未提交".equals(s.getStatus())) submitted++;
            if ("已批改".equals(s.getStatus())) graded++;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("submissionId", s.getSubmissionId());
            m.put("studentName", s.getStudentName());
            m.put("status", s.getStatus());
            m.put("submitTime", s.getSubmitTime() == null ? null : String.valueOf(s.getSubmitTime()));
            m.put("score", s.getScore());
            m.put("aiScore", s.getAiScore());
            m.put("aiStatus", s.getAiStatus());
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("该作业共 ").append(subs.size()).append(" 名学生，已提交 ")
                .append(submitted).append(" 份，已批改 ").append(graded).append(" 份：");
        int show = Math.min(rows.size(), 10);
        for (int i = 0; i < show; i++) {
            Map<String, Object> r = rows.get(i);
            sb.append("\n· [提交").append(r.get("submissionId")).append("] ").append(r.get("studentName"))
                    .append("（").append(r.get("status"));
            if (r.get("score") != null) sb.append("，教师评分：").append(r.get("score"));
            if (r.get("aiScore") != null) sb.append("，AI评分：").append(r.get("aiScore"));
            sb.append("）");
        }
        if (rows.size() > show) sb.append("\n…其余 ").append(rows.size() - show).append(" 条略");
        return ToolResult.ok(sb.toString(), Map.of("total", subs.size(), "submitted", submitted, "graded", graded, "rows", rows));
    }
}
