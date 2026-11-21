package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.Course;
import com.art.entity.CourseCategory;
import com.art.entity.CourseClass;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseMapper;
import com.art.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseClassMapper classMapper;
    private final CourseCategoryMapper categoryMapper;

    /** 按类别名称匹配 category_id，匹配不上返回 null（保持与历史行为一致） */
    private Integer matchCategoryId(String categoryName) {
        if (!StringUtils.hasText(categoryName)) return null;
        for (CourseCategory cat : categoryMapper.selectAll()) {
            if (categoryName.equals(cat.getCategoryName())) return cat.getCategoryId();
        }
        return null;
    }

    @Override
    public List<Course> list(String courseName, String category, Integer status) {
        return courseMapper.selectByCondition(courseName, category, status);
    }

    @Override
    public List<Course> listOnSale() {
        return courseMapper.selectAllOnSale();
    }

    @Override
    public Course getById(Integer courseId) {
        return courseMapper.selectById(courseId);
    }

    @Override
    public void create(Course course) {
        validate(course);
        course.setCategoryId(matchCategoryId(course.getCategory()));
        Course dup = courseMapper.selectByName(course.getCourseName().trim());
        if (dup != null) throw new BusinessException("课程名称已存在，不可重复");
        if (course.getLessonDuration() == null) course.setLessonDuration(45);
        if (course.getStatus() == null) course.setStatus(1);
        if (course.getSortOrder() == null) course.setSortOrder(0);
        // 自动生成课程编号 001、002 ...
        int max = courseMapper.selectByCondition(null, null, null).stream()
                .map(Course::getCourseNo).filter(n -> n != null && n.matches("\\d+"))
                .mapToInt(Integer::parseInt).max().orElse(0);
        course.setCourseNo(String.format("%03d", max + 1));
        courseMapper.insert(course);
    }

    @Override
    public void update(Integer courseId, Course course) {
        Course existing = courseMapper.selectById(courseId);
        if (existing == null) throw new BusinessException("课程不存在");
        validate(course);
        Course dup = courseMapper.selectByName(course.getCourseName().trim());
        if (dup != null && !dup.getCourseId().equals(courseId))
            throw new BusinessException("课程名称已存在，不可重复");
        course.setCourseId(courseId);
        if (course.getCourseNo() == null || course.getCourseNo().isBlank())
            course.setCourseNo(existing.getCourseNo());
        course.setCategoryId(matchCategoryId(course.getCategory()));
        courseMapper.update(course);
        // 课程产品信息变更后，同步其名下全部正常班级的冗余快照，避免课程/班级价格课时不一致
        for (CourseClass cc : classMapper.selectByCondition(null, null, courseId)) {
            if (cc.getStatus() != null && cc.getStatus() == 0) continue;
            cc.setTotalHours(course.getTotalHours());
            cc.setLessonDuration(course.getLessonDuration() != null ? course.getLessonDuration() : cc.getLessonDuration());
            cc.setTuition(course.getPrice());
            cc.setAgeGroup(course.getAgeGroup());
            cc.setLevel(course.getLevel());
            cc.setDescription(course.getIntro());
            classMapper.update(cc);
        }
    }

    @Override
    public void delete(Integer courseId) {
        Course existing = courseMapper.selectById(courseId);
        if (existing == null) throw new BusinessException("课程不存在");
        int classes = courseMapper.countClasses(courseId);
        if (classes > 0) throw new BusinessException("该课程下已有 " + classes + " 个班级，无法删除；请先删除或调整班级");
        courseMapper.deleteById(courseId);
    }

    private void validate(Course c) {
        if (!StringUtils.hasText(c.getCourseName())) throw new BusinessException("课程名称不能为空");
        if (c.getPrice() != null && c.getOriginalPrice() != null
                && c.getPrice().compareTo(c.getOriginalPrice()) > 0)
            throw new BusinessException("优惠价不能高于原价");
    }
}
