package com.art.service;

import com.art.vo.StudentProgressVO;
import com.art.vo.StudentProgressDetailVO;

import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherStudentProgressService {
    List<StudentProgressVO> getClassStudentsProgress(Integer classId);
    StudentProgressDetailVO getStudentProgressDetail(Integer studentId, Integer classId);
    void addProgressComment(Integer studentId, Integer classId, String comment);
}