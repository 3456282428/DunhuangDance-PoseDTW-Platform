package com.art.mapper;

import com.art.entity.AiModelMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI模型性能指标时序 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface AiModelMetricMapper {

    /** 取最近 N 条指标（按时间升序返回，便于直接绘图） */
    List<AiModelMetric> selectRecent(@Param("modelId") Integer modelId, @Param("limit") Integer limit);

    int insert(AiModelMetric metric);

    int deleteOlderThan(@Param("time") LocalDateTime time);

    int countAll();
}
