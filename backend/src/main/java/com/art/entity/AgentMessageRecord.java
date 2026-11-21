package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 会话消息（上下文记忆持久化，重启可续聊）
 * @author liukang&&heyan
 */
@Data
public class AgentMessageRecord implements Serializable {

    private Long msgId;
    private String sessionId;
    /** 角色：user / assistant */
    private String role;
    private String content;
    private LocalDateTime createTime;
}
