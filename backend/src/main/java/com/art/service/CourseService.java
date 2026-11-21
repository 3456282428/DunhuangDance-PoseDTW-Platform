package com.art.service;

import com.art.entity.Course;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface CourseService {
    List<Course> list(String courseName, String category, Integer status);
    List<Course> listOnSale();
    Course getById(Integer courseId);
    void create(Course course);
    void update(Integer courseId, Course course);
    void delete(Integer courseId);
}
