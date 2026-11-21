package com.art.mapper;

import com.art.entity.KbDocVector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识条目向量 Mapper（kb_doc_vector）
  * @author liukang&&heyan
 */
@Mapper
public interface KbVectorMapper {

    List<KbDocVector> selectAll();

    /** 按端别读取（增量刷新时对比 content_hash） */
    List<KbDocVector> selectByScope(@Param("kbScope") String kbScope);

    /** 按向量模型标识读取（冷启动装载索引） */
    List<KbDocVector> selectByModel(@Param("model") String model);

    int upsert(KbDocVector row);

    /** 批量幂等写入（单条 SQL 多行 VALUES + ON DUPLICATE KEY UPDATE） */
    int upsertBatch(@Param("rows") List<KbDocVector> rows);

    int deleteByDoc(@Param("kbScope") String kbScope, @Param("kbId") Integer kbId);

    int deleteByScope(@Param("kbScope") String kbScope);

    int countAll();
}
