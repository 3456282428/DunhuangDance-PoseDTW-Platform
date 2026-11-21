package com.art.mapper;

import com.art.entity.KbEntryModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识条目-3D模型推荐映射 Mapper（kb_entry_model）
 * @author liukang&&heyan
 */
@Mapper
public interface KbEntryModelMapper {

    KbEntryModel selectByDoc(@Param("kbScope") String kbScope, @Param("kbId") Integer kbId);

    List<KbEntryModel> selectByScope(@Param("kbScope") String kbScope);

    /** 批量幂等写入 */
    int upsertBatch(@Param("rows") List<KbEntryModel> rows);

    /** 清理已不存在的条目推荐（保留 alive 集合内的） */
    int deleteByScopeNotIn(@Param("kbScope") String kbScope, @Param("aliveIds") List<Integer> aliveIds);

    /** 清理某端全部推荐（条目全部失效时） */
    int deleteByScope(@Param("kbScope") String kbScope);

    int countAll();
}
