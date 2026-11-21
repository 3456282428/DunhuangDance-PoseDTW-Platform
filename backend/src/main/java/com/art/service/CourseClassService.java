package com.art.service;

import com.art.common.PageResult;
import com.art.dto.ClassDTO;
import com.art.entity.CourseClass;

/**
 * @author liukang&&heyan
 */
public interface CourseClassService {
    PageResult<CourseClass> pageClasses(Integer pageNum, Integer pageSize, String className, Integer categoryId);
    void createClass(ClassDTO classDTO);
    void updateClass(Integer classId, ClassDTO classDTO);
    void deleteClass(Integer classId);
    CourseClass getById(Integer classId);
}