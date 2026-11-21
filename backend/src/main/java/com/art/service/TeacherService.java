package com.art.service;

import com.art.common.PageResult;
import com.art.entity.Teacher;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherService {
    List<Teacher> getAllTeachers();
    Teacher getTeacherById(Integer id);
    PageResult<Teacher> pageTeachers(Integer pageNum, Integer pageSize, String keyword);
    void createTeacher(Teacher teacher);
    void updateTeacher(Teacher teacher);
    void deleteTeacher(Integer id);
}