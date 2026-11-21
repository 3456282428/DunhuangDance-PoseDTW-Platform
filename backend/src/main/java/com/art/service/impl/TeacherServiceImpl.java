package com.art.service.impl;

import com.art.common.PageResult;
import com.art.entity.Teacher;
import com.art.mapper.TeacherMapper;
import com.art.service.TeacherService;
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
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;

    @Override
    public List<Teacher> getAllTeachers() {
        return teacherMapper.selectAll();
    }

    @Override
    public Teacher getTeacherById(Integer id) {
        return teacherMapper.selectById(id);
    }

    @Override
    public PageResult<Teacher> pageTeachers(Integer pageNum, Integer pageSize, String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        List<Teacher> list = teacherMapper.selectByCondition(keyword, null);
        PageInfo<Teacher> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public void createTeacher(Teacher teacher) {
        teacher.setStatus(1);
        teacherMapper.insert(teacher);
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        teacherMapper.update(teacher);
    }

    @Override
    public void deleteTeacher(Integer id) {
        teacherMapper.deleteById(id);
    }
}