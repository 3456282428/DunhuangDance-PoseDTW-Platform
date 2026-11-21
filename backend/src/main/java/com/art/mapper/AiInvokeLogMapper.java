package com.art.mapper;

import com.art.entity.AiInvokeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI调用日志 Mapper（预留）
  * @author liukang&&heyan
 */
@Mapper
public interface AiInvokeLogMapper {
    int insert(AiInvokeLog log);
    List<AiInvokeLog> selectAll();
    int updateResult(AiInvokeLog log);
}
