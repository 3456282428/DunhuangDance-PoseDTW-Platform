package com.art.mapper;

import com.art.entity.KbOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库操作日志 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface KbOperationLogMapper {
    int insert(KbOperationLog log);
    List<KbOperationLog> selectPage(@Param("kbScope") String kbScope, @Param("opType") String opType);
}
