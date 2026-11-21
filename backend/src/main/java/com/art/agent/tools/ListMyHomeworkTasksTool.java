package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.service.StudentHomeworkService;
import com.art.util.SecurityUtil;
import com.art.vo.StudentHomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人的作业列表与完成状态（学生端只读，不提供提交服务）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyHomeworkTasksTool implements AgentTool {

    private final StudentHomeworkService homeworkService;

    @Override
    public String name() { return "list_my_homework_tasks"; }

    @Override
    public String description() { return "查询我的作业列表（标题/班级/截止时间/提交状态/教师评分/AI评分），可按状态筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"status\":{\"type\":\"string\",\"desc\":\"状态筛选：未提交/已提交/已批改，不填查全部\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        if (SecurityUtil.getCurrentStudentId() == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        String status = ToolArgs.str(args, "status");
        if (status != null && !List.of("未提交", "已提交", "已批改").contains(status)) {
            return ToolResult.fail("状态参数不合法", "可选值：未提交 / 已提交 / 已批改");
        }
        PageResult<StudentHomeworkVO> page = homeworkService.pageMyHomeworks(1, size, status);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StudentHomeworkVO h : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("homeworkId", h.getHomeworkId());
            m.put("title", h.getTitle());
            m.put("className", h.getClassName());
            m.put("createTime", h.getCreateTime() == null ? null : String.valueOf(h.getCreateTime()));
            m.put("status", h.getStatus());
            m.put("score", h.getScore());
            m.put("aiScore", h.getAiScore());
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("目前没有符合条件的作业。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("共 ").append(page.getTotal()).append(" 份作业，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("homeworkId")).append("] ").append(r.get("title"))
                    .append("（").append(r.get("className")).append("，").append(r.get("status"));
            if (r.get("score") != null) sb.append("，教师评分：").append(r.get("score"));
            if (r.get("aiScore") != null) sb.append("，AI评分：").append(r.get("aiScore"));
            sb.append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
