package com.art.agent;

/**
 * 工具执行结果：summary 为面向 LLM 与用户的结构化文本摘要，data 为裁剪后的结构化数据。
 * @author liukang&&heyan
 */
public record ToolResult(boolean success, String summary, Object data, String suggestion) {

    public static ToolResult ok(String summary, Object data) {
        return new ToolResult(true, summary, data, null);
    }

    public static ToolResult fail(String message, String suggestion) {
        return new ToolResult(false, "执行失败：" + message, null, suggestion);
    }
}
