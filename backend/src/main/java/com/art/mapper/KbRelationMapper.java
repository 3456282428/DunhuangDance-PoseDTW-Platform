package com.art.mapper;

import com.art.entity.KbRelation;
import com.art.search.RefPair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库关联映射 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface KbRelationMapper {

    List<KbRelation> selectAll();

    /** 查询与某业务引用直接相关的全部关系（源或目标命中） */
    List<KbRelation> selectByRef(@Param("ref") String ref);

    /** 批量查询（多个 ref 一次取回，供关联召回使用） */
    List<KbRelation> selectByRefs(@Param("refs") java.util.List<String> refs);

    int upsertBatch(@Param("rows") List<KbRelation> rows);

    int deleteAll();

    int countAll();

    /** 由业务表外键推导关系对（数据来源，见 XML 中的 UNION ALL 查询） */
    List<RefPair> selectBusinessPairs();
}
