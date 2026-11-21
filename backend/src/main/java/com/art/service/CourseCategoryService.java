package com.art.service;

import com.art.entity.CourseCategory;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface CourseCategoryService {
    List<CourseCategory> listAll();
    void create(CourseCategory category);
    void update(CourseCategory category);
    void delete(Integer categoryId);
}