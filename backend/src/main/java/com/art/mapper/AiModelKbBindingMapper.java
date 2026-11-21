package com.art.mapper;

import com.art.entity.AiModelKbBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI模型-知识库关联约定 Mapper（预留）
  * @author liukang&&heyan
 */
@Mapper
public interface AiModelKbBindingMapper {
    List<AiModelKbBinding> selectAll();
    List<AiModelKbBinding> selectEnabledByModel(@Param("modelId") Integer modelId);
    int insert(AiModelKbBinding binding);
    int updateEnabled(@Param("bindingId") Integer bindingId, @Param("enabled") Integer enabled);
    int deleteById(@Param("bindingId") Integer bindingId);
    int deleteByModelId(@Param("modelId") Integer modelId);
}
