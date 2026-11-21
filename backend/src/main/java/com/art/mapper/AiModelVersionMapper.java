package com.art.mapper;

import com.art.entity.AiModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI模型版本迭代 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface AiModelVersionMapper {

    List<AiModelVersion> selectByModel(@Param("modelId") Integer modelId);

    int countByModelAndVersion(@Param("modelId") Integer modelId, @Param("version") String version);

    int insert(AiModelVersion version);

    /** 将指定版本标记为当前运行版本，其余置为非当前 */
    int markCurrent(@Param("modelId") Integer modelId, @Param("version") String version);

    /** 清理待接入的旧升级意向（重复登记时避免堆积） */
    int deletePendingByModel(@Param("modelId") Integer modelId);
}
