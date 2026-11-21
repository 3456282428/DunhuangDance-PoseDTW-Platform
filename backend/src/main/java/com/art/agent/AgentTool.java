package com.art.agent;

import java.util.Map;

/**
 * Agent 工具标准接口（借鉴 spring-ai-alibaba ToolCallback 设计）。
 * 新增能力只需实现本接口并标注 @Component，注册表自动发现，
 * 无需修改调度核心（开闭原则）。
 * @author liukang&&heyan
 */
public interface AgentTool {

    /** 工具名（LLM 调用标识，蛇形命名，全局唯一） */
    String name();

    /** 功能说明（注入系统提示词，供模型理解何时调用） */
    String description();

    /** 参数 JSON Schema 片段，如 {"keyword":{"type":"string","desc":"姓名或账号关键词","required":false}} */
    String parametersSchema();

    /** 是否需要用户确认后才执行（敏感/写操作置 true） */
    default boolean requiresConfirmation() {
        return false;
    }

    /** 风险提示文案（requiresConfirmation=true 时展示给用户） */
    default String riskHint() {
        return "";
    }

    /** 允许调用的角色（默认仅管理员） */
    default String[] allowedRoles() {
        return new String[]{"ADMIN"};
    }

    /** 执行工具。args 为模型给出的参数；实现层必须自行做参数校验与异常兜底 */
    ToolResult execute(Map<String, Object> args, AgentContext ctx);
}
