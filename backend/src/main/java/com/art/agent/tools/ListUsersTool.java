package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.SysUser;
import com.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询系统用户列表（管理端用户与角色管理）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListUsersTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() { return "list_users"; }

    @Override
    public String description() { return "查询系统用户（账号/姓名/角色/状态），支持按关键词与角色筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"keyword\":{\"type\":\"string\",\"desc\":\"用户名/姓名/手机号关键词\",\"required\":false},"
                + "\"role\":{\"type\":\"string\",\"desc\":\"角色：ADMIN/TEACHER/STUDENT/FINANCE/ACADEMIC\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        PageResult<SysUser> page = userService.pageUsers(1, size,
                ToolArgs.str(args, "keyword"), ToolArgs.str(args, "role"), null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole());
            m.put("status", u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("共匹配 ").append(page.getTotal()).append(" 个用户，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("userId")).append("] ").append(r.get("realName"))
                    .append("（").append(r.get("username")).append("，").append(r.get("role"))
                    .append("，").append(r.get("status")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
