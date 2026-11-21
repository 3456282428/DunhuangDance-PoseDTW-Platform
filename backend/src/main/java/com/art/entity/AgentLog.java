package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 智能体操作审计日志（意图/工具/参数/结果/确认留痕）
 * @author liukang&&heyan
 */
@Data
public class AgentLog implements Serializable {

    private Long logId;
    private String sessionId;
    private Integer userId;
    private String username;
    /** 用户原始输入 */
    private String userMessage;
    /** 触发的工具名（纯对话为NULL） */
    private String toolName;
    /** 工具参数JSON */
    private String toolArgs;
    /** 是否经用户确认：0无需确认 1已确认 2已拒绝 */
    private Integer confirmed;
    /** 执行状态：SUCCESS/FAILED/REJECTED/CHAT */
    private String execStatus;
    /** 结果摘要 */
    private String resultSummary;
    private LocalDateTime createTime;
}
