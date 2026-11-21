package com.art.mapper;

import com.art.entity.KbModelResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 3D模型资源 Mapper（kb_model_resource）
 * @author liukang&&heyan
 */
@Mapper
public interface KbModelResourceMapper {

    /** 启用的模型列表（推荐候选集） */
    List<KbModelResource> selectEnabled();

    List<KbModelResource> selectAll();

    KbModelResource selectById(Integer modelId);

    int countAll();
}
