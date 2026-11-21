package com.art.mapper;

import com.art.entity.HomeworkSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface HomeworkSubmissionMapper {
    HomeworkSubmission selectById(@Param("submissionId") Integer submissionId);
    HomeworkSubmission selectByHomeworkAndStudent(@Param("homeworkId") Integer homeworkId,
                                                  @Param("studentId") Integer studentId);
    int insert(HomeworkSubmission submission);
    int update(HomeworkSubmission submission);
    int updateAiFields(HomeworkSubmission submission);
    List<HomeworkSubmission> selectByHomeworkId(@Param("homeworkId") Integer homeworkId);
    List<HomeworkSubmission> selectByStudentId(@Param("studentId") Integer studentId);
    List<HomeworkSubmission> selectByStatus(@Param("status") String status);
    /** 全量遍历（知识库全量归集用） */
    List<HomeworkSubmission> selectAll();
    int countByClassAndStudent(@Param("classId") Integer classId, @Param("studentId") Integer studentId);
}
