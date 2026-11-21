package com.art.mapper;

import com.art.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface SystemConfigMapper {
    SystemConfig selectByKey(@Param("configKey") String configKey);
    /** 全量遍历（知识库全量归集用） */
    List<SystemConfig> selectAll();
    int upsert(SystemConfig config);
}
