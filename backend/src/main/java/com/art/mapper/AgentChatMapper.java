package com.art.mapper;

import com.art.entity.AgentMessageRecord;
import com.art.entity.AgentSessionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 会话与消息持久化 Mapper（agent_session / agent_message）
 * @author liukang&&heyan
 */
@Mapper
public interface AgentChatMapper {

    int insertSession(AgentSessionRecord session);

    AgentSessionRecord selectSession(@Param("sessionId") String sessionId);

    /** 某用户的会话列表（按最后活跃倒序） */
    List<AgentSessionRecord> selectSessionsByUser(@Param("userId") Integer userId, @Param("limit") Integer limit);

    int updateSessionTitle(@Param("sessionId") String sessionId, @Param("title") String title);

    /** 消息条数 +1 并刷新活跃时间 */
    int touchSession(@Param("sessionId") String sessionId);

    int deleteSession(@Param("sessionId") String sessionId);

    int insertMessage(AgentMessageRecord message);

    /** 会话历史（按时间正序，取最近 limit 条） */
    List<AgentMessageRecord> selectMessages(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    int countMessages(@Param("sessionId") String sessionId);

    int deleteMessages(@Param("sessionId") String sessionId);
}
