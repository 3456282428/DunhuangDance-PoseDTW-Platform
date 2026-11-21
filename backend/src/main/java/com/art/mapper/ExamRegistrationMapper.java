package com.art.mapper;

import com.art.entity.ExamRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface ExamRegistrationMapper {
    int insert(ExamRegistration r);
    int update(ExamRegistration r);
    ExamRegistration selectById(@Param("regId") Integer regId);
    ExamRegistration selectByProjectStudent(@Param("projectId") Integer projectId, @Param("studentId") Integer studentId);
    /** 全量遍历（知识库全量归集用） */
    List<ExamRegistration> selectAll();
    List<Map<String,Object>> selectByCondition(@Param("projectId") Integer projectId,
                                               @Param("studentId") Integer studentId,
                                               @Param("status") String status,
                                               @Param("activityType") String activityType);
    List<com.art.vo.ExamAchievementVO> selectAchievementsByStudent(@Param("studentId") Integer studentId,
                                                                    @Param("activityType") String activityType);
    List<com.art.vo.ExamAchievementVO> selectAchievementsByClass(@Param("classId") Integer classId,
                                                                  @Param("activityType") String activityType);
}
