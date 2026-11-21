package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.CourseClass;
import com.art.service.CourseClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询班级列表（管理端班级管理）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListClassesTool implements AgentTool {

    private final CourseClassService classService;

    @Override
    public String name() { return "list_classes"; }

    @Override
    public String description() { return "查询班级（班级名/课程/授课教师/教室/学期/在读人数），支持按班级名筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"className\":{\"type\":\"string\",\"desc\":\"班级名关键词\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        PageResult<CourseClass> page = classService.pageClasses(1, size, ToolArgs.str(args, "className"), null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseClass c : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getClassId());
            m.put("className", c.getClassName());
            m.put("courseName", c.getCourseName());
            m.put("teacherName", c.getTeacherName());
            m.put("semester", c.getSemester());
            m.put("students", (c.getCurrentStudents() == null ? 0 : c.getCurrentStudents()) + "/" + c.getMaxStudents());
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("共匹配 ").append(page.getTotal()).append(" 个班级，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("className")).append("（").append(r.get("courseName"))
                    .append("，教师：").append(r.get("teacherName")).append("，人数：").append(r.get("students")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
