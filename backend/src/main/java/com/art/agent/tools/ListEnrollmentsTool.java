package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.Enrollment;
import com.art.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询报名记录（管理端报名管理）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListEnrollmentsTool implements AgentTool {

    private final EnrollmentService enrollmentService;

    @Override
    public String name() { return "list_enrollments"; }

    @Override
    public String description() { return "查询学员报名记录（学员/班级/报名日期/学费/缴费/状态），支持按学员姓名与状态筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"studentName\":{\"type\":\"string\",\"desc\":\"学员姓名关键词\",\"required\":false},"
                + "\"status\":{\"type\":\"string\",\"desc\":\"状态：在读/退学/结课\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        PageResult<Enrollment> page = enrollmentService.pageEnrollments(1, size,
                ToolArgs.str(args, "studentName"), null, ToolArgs.str(args, "status"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Enrollment e : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getEnrollmentId());
            m.put("studentName", e.getStudentName());
            m.put("className", e.getClassName());
            m.put("enrollDate", String.valueOf(e.getEnrollDate()));
            m.put("tuition", e.getTuitionAmount());
            m.put("paid", e.getTotalPaid());
            m.put("status", e.getStatus());
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("共匹配 ").append(page.getTotal()).append(" 条报名记录，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("studentName")).append(" → ").append(r.get("className"))
                    .append("（").append(r.get("enrollDate")).append("，学费").append(r.get("tuition"))
                    .append("，已缴").append(r.get("paid")).append("，").append(r.get("status")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
