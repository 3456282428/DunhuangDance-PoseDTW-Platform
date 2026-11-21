package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.service.TeacherHomeworkService;
import com.art.vo.HomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前教师布置的作业（教师端，服务层已限定本人班级数据）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyHomeworkTool implements AgentTool {

    private final TeacherHomeworkService homeworkService;

    @Override
    public String name() { return "list_my_homework"; }

    @Override
    public String description() { return "查询我（当前教师）布置的作业列表（标题/班级/截止时间/提交进度），可按班级ID筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"classId\":{\"type\":\"int\",\"desc\":\"班级ID（可用list_my_classes获取），不填则查全部\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        Integer classId = ToolArgs.integer(args, "classId");
        PageResult<HomeworkVO> page = homeworkService.pageMyHomework(1, size, classId, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HomeworkVO h : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("homeworkId", h.getHomeworkId());
            m.put("title", h.getTitle());
            m.put("className", h.getClassName());
            m.put("deadline", h.getDeadline() == null ? null : String.valueOf(h.getDeadline()));
            m.put("deadlineStatus", h.getDeadlineStatus());
            m.put("progress", h.getSubmittedCount() + "/" + h.getTotalStudents());
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("您还没有布置符合条件的作业。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("共 ").append(page.getTotal()).append(" 份作业，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("homeworkId")).append("] ").append(r.get("title"))
                    .append("（").append(r.get("className")).append("，提交 ").append(r.get("progress"))
                    .append("，截止 ").append(r.get("deadline")).append(" ").append(r.get("deadlineStatus")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
