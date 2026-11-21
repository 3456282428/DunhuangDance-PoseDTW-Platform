package com.art.mapper;

import com.art.entity.OpenComponentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 开源组件版本迭代 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface OpenComponentVersionMapper {

    List<OpenComponentVersion> selectByComponent(@Param("componentId") Integer componentId);

    int countByComponentAndVersion(@Param("componentId") Integer componentId, @Param("version") String version);

    int insert(OpenComponentVersion version);

    /** 清理组件下非当前版本且来源为 API 的旧记录（重复同步时防止堆积） */
    int deleteAutoByComponent(@Param("componentId") Integer componentId);

    int countAll();
}
