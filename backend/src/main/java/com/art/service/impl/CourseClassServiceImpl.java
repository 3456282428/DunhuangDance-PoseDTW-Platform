package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.dto.ClassDTO;
import com.art.entity.Course;
import com.art.entity.CourseClass;
import com.art.entity.CourseCategory;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseMapper;
import com.art.service.CourseClassService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class CourseClassServiceImpl implements CourseClassService {

    private final CourseClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseCategoryMapper categoryMapper;

    @Override
    public PageResult<CourseClass> pageClasses(Integer pageNum, Integer pageSize, String className, Integer categoryId) {
        PageHelper.startPage(pageNum, pageSize);
        List<CourseClass> list = classMapper.selectByCondition(className, categoryId, null);
        PageInfo<CourseClass> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public void createClass(ClassDTO dto) {
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) throw new BusinessException("所选课程不存在");

        CourseClass cc = new CourseClass();
        BeanUtils.copyProperties(dto, cc);
        applyCourse(cc, course);
        if (cc.getStatus() == null) cc.setStatus(1);
        classMapper.insert(cc);
    }

    @Override
    public void updateClass(Integer classId, ClassDTO dto) {
        CourseClass existing = classMapper.selectById(classId);
        if (existing == null) throw new BusinessException("班级不存在");
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) throw new BusinessException("所选课程不存在");

        BeanUtils.copyProperties(dto, existing, "classId");
        applyCourse(existing, course);
        classMapper.update(existing);
    }

    /** 班级归属一门课程，产品信息从课程同步 */
    private void applyCourse(CourseClass cc, Course course) {
        if (!StringUtils.hasText(cc.getClassName())) cc.setClassName(course.getCourseName());
        // 类别优先取课程的权威 category_id，缺失时再按类别名兜底匹配
        cc.setCategoryId(course.getCategoryId() != null
                ? course.getCategoryId() : matchCategoryId(course.getCategory()));
        cc.setTotalHours(course.getTotalHours());
        cc.setLessonDuration(course.getLessonDuration() != null ? course.getLessonDuration() : 45);
        cc.setTuition(course.getPrice());
        cc.setAgeGroup(course.getAgeGroup());
        cc.setLevel(course.getLevel());
        cc.setDescription(course.getIntro());
    }

    private Integer matchCategoryId(String categoryName) {
        if (!StringUtils.hasText(categoryName)) return null;
        for (CourseCategory cat : categoryMapper.selectAll()) {
            if (categoryName.equals(cat.getCategoryName())) return cat.getCategoryId();
        }
        return null;
    }

    @Override
    public void deleteClass(Integer classId) {
        classMapper.deleteById(classId);
    }

    @Override
    public CourseClass getById(Integer classId) {
        return classMapper.selectById(classId);
    }
}
