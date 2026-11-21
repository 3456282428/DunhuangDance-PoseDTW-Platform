package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolResult;
import com.art.entity.CourseClass;
import com.art.mapper.CourseClassMapper;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前教师本人授课的班级（教师端，数据范围仅限本人）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyClassesTool implements AgentTool {

    private final CourseClassMapper classMapper;

    @Override
    public String name() { return "list_my_classes"; }

    @Override
    public String description() { return "查询我（当前教师）授课的班级列表（班级名/课程/学期/在读人数）"; }

    @Override
    public String parametersSchema() { return "{}"; }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return ToolResult.fail("未找到当前教师的档案信息", "请确认账号已关联教师档案");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseClass c : classMapper.selectAll()) {
            if (!teacherId.equals(c.getTeacherId())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", c.getClassId());
            m.put("className", c.getClassName());
            m.put("courseName", c.getCourseName());
            m.put("semester", c.getSemester());
            m.put("students", (c.getCurrentStudents() == null ? 0 : c.getCurrentStudents()) + "/" + c.getMaxStudents());
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("您目前没有授课中的班级。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("您共授课 ").append(rows.size()).append(" 个班级：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("classId")).append("] ").append(r.get("className"))
                    .append("（").append(r.get("courseName")).append("，").append(r.get("semester"))
                    .append("，人数：").append(r.get("students")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", rows.size(), "rows", rows));
    }
}
