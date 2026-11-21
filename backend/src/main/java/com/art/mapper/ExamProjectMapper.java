package com.art.mapper;

import com.art.entity.ExamProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface ExamProjectMapper {
    int insert(ExamProject p);
    int update(ExamProject p);
    int deleteById(@Param("projectId") Integer projectId);
    ExamProject selectById(@Param("projectId") Integer projectId);
    /** 全量遍历（知识库全量归集用） */
    List<ExamProject> selectAll();
    List<Map<String,Object>> selectByCondition(@Param("activityType") String activityType,
                                               @Param("category") String category,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword);
}
