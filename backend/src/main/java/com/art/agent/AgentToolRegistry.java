package com.art.agent;

import com.art.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 工具注册表：Spring 自动收集全部 AgentTool 实现，
 * 负责目录生成（注入提示词）、按名查找、角色准入校验。
 * @author liukang&&heyan
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public AgentToolRegistry(List<AgentTool> toolBeans) {
        for (AgentTool t : toolBeans) {
            if (tools.put(t.name(), t) != null) {
                throw new IllegalStateException("Agent工具名重复：" + t.name());
            }
        }
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public List<AgentTool> listFor(AgentContext ctx) {
        return tools.values().stream()
                .filter(t -> allowed(t, ctx))
                .collect(Collectors.toList());
    }

    public boolean allowed(AgentTool tool, AgentContext ctx) {
        for (String role : tool.allowedRoles()) {
            if (ctx.hasRole(role)) return true;
        }
        return false;
    }

    /** 工具目录（注入系统提示词） */
    public String catalogPrompt(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (AgentTool t : listFor(ctx)) {
            sb.append("- ").append(t.name()).append("：").append(t.description())
                    .append("；参数：").append(t.parametersSchema());
            if (t.requiresConfirmation()) sb.append("；【敏感操作，执行前需用户确认】");
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 执行入口：统一做存在性与权限校验、异常兜底 */
    public ToolResult execute(String name, Map<String, Object> args, AgentContext ctx) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.fail("未找到工具 " + name,
                    "可尝试换个说法，或输入「你能做什么」查看可用能力");
        }
        if (!allowed(tool, ctx)) {
            throw new BusinessException(403, "当前角色无权使用该工具");
        }
        try {
            return tool.execute(args == null ? Map.of() : args, ctx);
        } catch (BusinessException e) {
            return ToolResult.fail(e.getMessage(), "可修正参数后重试");
        } catch (Exception e) {
            return ToolResult.fail("系统内部错误（" + e.getClass().getSimpleName() + "）",
                    "请稍后重试；若持续失败请联系系统管理员");
        }
    }
}
