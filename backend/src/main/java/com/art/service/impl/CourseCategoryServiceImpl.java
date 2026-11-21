package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.CourseCategory;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseMapper;
import com.art.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class CourseCategoryServiceImpl implements CourseCategoryService {

    private final CourseCategoryMapper categoryMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper classMapper;

    @Override
    public List<CourseCategory> listAll() {
        return categoryMapper.selectAll();
    }

    @Override
    public void create(CourseCategory category) {
        categoryMapper.insert(category);
    }

    @Override
    public void update(CourseCategory category) {
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer categoryId) {
        CourseCategory cat = categoryMapper.selectById(categoryId);
        if (cat == null) throw new BusinessException("类别不存在");
        // 被课程（按类别名）或班级（按 category_id）引用时禁止删除，避免外键悬空与统计断链
        long courseCount = courseMapper.selectByCondition(null, cat.getCategoryName(), null).size();
        long classCount = classMapper.selectByCondition(null, categoryId, null).size();
        if (courseCount > 0 || classCount > 0)
            throw new BusinessException("该类别下仍有 " + (courseCount + classCount)
                    + " 个课程/班级在使用，无法删除；请先调整归属");
        categoryMapper.deleteById(categoryId);
    }
}
