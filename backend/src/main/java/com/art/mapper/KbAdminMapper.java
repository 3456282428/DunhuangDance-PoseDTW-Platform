package com.art.mapper;

import com.art.entity.KbDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员端知识库 Mapper（kb_admin_doc）
  * @author liukang&&heyan
 */
@Mapper
public interface KbAdminMapper {
    List<KbDoc> selectPage(@Param("keyword") String keyword, @Param("category") String category, @Param("status") Integer status);
    KbDoc selectById(@Param("kbId") Integer kbId);
    KbDoc selectBySourceRef(@Param("sourceType") String sourceType, @Param("sourceRef") String sourceRef);
    List<KbDoc> selectAllActive();
    /** 按业务引用（含 :t1/:s1 等端别后缀）批量定位条目，用于跨知识库关联召回 */
    List<KbDoc> selectByRefPrefix(@Param("ref") String ref);
    List<KbDoc> selectByKeywords(@Param("keywords") java.util.List<String> keywords, @Param("category") String category);
    int insert(KbDoc doc);
    int update(KbDoc doc);
    int deleteById(@Param("kbId") Integer kbId);
    int deleteAll();
    int countAll();
    int increaseViewCount(@Param("kbId") Integer kbId);
}
