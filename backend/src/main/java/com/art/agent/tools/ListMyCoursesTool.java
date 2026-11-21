package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolResult;
import com.art.dto.StudentCourseDTO;
import com.art.service.StudentCourseService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人在学课程（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyCoursesTool implements AgentTool {

    private final StudentCourseService courseService;

    @Override
    public String name() { return "list_my_courses"; }

    @Override
    public String description() { return "查询我的在学课程（班级/课程类别/教师/学期/出勤进度）"; }

    @Override
    public String parametersSchema() { return "{}"; }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        if (SecurityUtil.getCurrentStudentId() == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        List<StudentCourseDTO> courses = courseService.getMyCourses();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StudentCourseDTO c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getClassId());
            m.put("className", c.getClassName());
            m.put("categoryName", c.getCategoryName());
            m.put("teacherName", c.getTeacherName());
            m.put("semester", c.getSemester());
            m.put("attendance", (c.getAttendedCount() == null ? 0 : c.getAttendedCount()) + "/" + (c.getTotalHours() == null ? 0 : c.getTotalHours()) + " 课时");
            m.put("status", c.getStatus());
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("你目前没有报名任何课程。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("你共报名 ").append(rows.size()).append(" 门课程：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("className")).append("（").append(r.get("categoryName"))
                    .append("，教师：").append(r.get("teacherName")).append("，").append(r.get("semester"))
                    .append("，出勤：").append(r.get("attendance")).append("，").append(r.get("status")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", rows.size(), "rows", rows));
    }
}
