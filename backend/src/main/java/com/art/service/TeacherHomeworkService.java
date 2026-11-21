package com.art.service;

import com.art.common.PageResult;
import com.art.dto.AssignHomeworkDTO;
import com.art.dto.GradeHomeworkDTO;
import com.art.vo.HomeworkVO;
import com.art.vo.HomeworkSubmissionVO;

import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherHomeworkService {
    void assignHomework(AssignHomeworkDTO assignDTO);
    PageResult<HomeworkVO> pageMyHomework(Integer pageNum, Integer pageSize, Integer classId, String status);
    List<HomeworkSubmissionVO> getSubmissions(Integer homeworkId);
    void gradeHomework(Integer submissionId, GradeHomeworkDTO gradeDTO);
    HomeworkSubmissionVO getStudentHomework(Integer homeworkId, Integer studentId);
}
