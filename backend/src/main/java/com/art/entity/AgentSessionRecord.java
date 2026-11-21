package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 会话（多段对话管理，持久化于 agent_session）
 * @author liukang&&heyan
 */
@Data
public class AgentSessionRecord implements Serializable {

    private String sessionId;
    private Integer userId;
    private String username;
    /** 会话标题（取首条用户消息摘要） */
    private String title;
    private Integer msgCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
