package com.art.service;

import com.art.common.PageResult;
import com.art.entity.Student;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface StudentService {
    Student getStudentById(Integer id);
    List<Student> listAll();
    PageResult<Student> pageStudents(Integer pageNum, Integer pageSize, String keyword);
    void createStudent(Student student);
    void updateStudent(Student student);
    void deleteStudent(Integer id);
}