package com.art.service.impl;

import com.art.common.PageResult;
import com.art.entity.CourseClass;
import com.art.entity.Homework;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.HomeworkMapper;
import com.art.service.HomeworkService;
import com.art.vo.HomeworkVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class HomeworkServiceImpl implements HomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final CourseClassMapper classMapper;

    @Override
    public PageResult<HomeworkVO> pageHomeworks(Integer pageNum, Integer pageSize, Integer classId, String status) {
        PageHelper.startPage(pageNum, pageSize);
        List<Homework> list = homeworkMapper.selectByConditions(classId, null);
        List<HomeworkVO> vos = new ArrayList<>();
        for (Homework h : list) {
            HomeworkVO vo = new HomeworkVO();
            vo.setHomeworkId(h.getHomeworkId());
            vo.setClassId(h.getClassId());
            CourseClass cc = h.getClassId() != null ? classMapper.selectById(h.getClassId()) : null;
            vo.setClassName(cc != null ? cc.getClassName() : null);
            vo.setScheduleId(h.getScheduleId());
            vo.setTitle(h.getTitle());
            vo.setContent(h.getContent());
            vo.setCreateTime(h.getCreateTime());
            vos.add(vo);
        }
        PageInfo<Homework> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), vos, pageNum, pageSize);
    }

    @Override
    public void delete(Integer homeworkId) {
        homeworkMapper.deleteById(homeworkId);
    }
}
