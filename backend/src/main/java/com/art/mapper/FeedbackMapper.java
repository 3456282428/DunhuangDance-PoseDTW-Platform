package com.art.mapper;

import com.art.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface FeedbackMapper {
    Feedback selectById(@Param("feedbackId") Integer feedbackId);
    int insert(Feedback feedback);
    int update(Feedback feedback);
    List<Feedback> selectByStudentId(@Param("studentId") Integer studentId);
    List<Feedback> selectByCondition(@Param("studentId") Integer studentId,
                                     @Param("status") String status);
    List<Feedback> selectAll();
}
