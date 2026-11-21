package com.art.service.impl;

import com.art.common.PageResult;
import com.art.entity.Student;
import com.art.mapper.StudentMapper;
import com.art.service.StudentService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    @Override
    public Student getStudentById(Integer id) {
        return studentMapper.selectById(id);
    }

    @Override
    public List<Student> listAll() {
        return studentMapper.selectAll();
    }

    @Override
    public PageResult<Student> pageStudents(Integer pageNum, Integer pageSize, String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        List<Student> list = studentMapper.selectByCondition(keyword);
        PageInfo<Student> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public void createStudent(Student student) {
        student.setStatus(1);
        studentMapper.insert(student);
    }

    @Override
    public void updateStudent(Student student) {
        studentMapper.update(student);
    }

    @Override
    public void deleteStudent(Integer id) {
        studentMapper.deleteById(id);
    }
}