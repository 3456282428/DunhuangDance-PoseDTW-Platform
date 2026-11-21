package com.art.mapper;

import com.art.entity.AgentLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent 智能体操作审计 Mapper
 * @author liukang&&heyan
 */
@Mapper
public interface AgentLogMapper {

    int insert(AgentLog log);

    List<AgentLog> selectRecent(@Param("limit") int limit);

    /** 按操作者隔离查询（教师端仅可见本人审计） */
    List<AgentLog> selectRecentByUser(@Param("userId") Integer userId, @Param("limit") int limit);
}
