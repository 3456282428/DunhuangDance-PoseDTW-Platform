package com.art.mapper;

import com.art.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI模型注册 Mapper（预留）
  * @author liukang&&heyan
 */
@Mapper
public interface AiModelMapper {
    List<AiModel> selectAll();
    AiModel selectById(@Param("modelId") Integer modelId);
    int insert(AiModel model);
    int update(AiModel model);
    int deleteById(@Param("modelId") Integer modelId);

    /** 仪表盘：回写模型版本信息（升级登记） */
    int updateVersionInfo(@Param("modelId") Integer modelId,
                          @Param("currentVersion") String currentVersion,
                          @Param("latestVersion") String latestVersion,
                          @Param("updateStatus") String updateStatus);
}
