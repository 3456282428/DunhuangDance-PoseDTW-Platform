package com.art.mapper;

import com.art.entity.CourseCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface CourseCategoryMapper {
    List<CourseCategory> selectAll();
    CourseCategory selectById(@Param("categoryId") Integer categoryId);
    int insert(CourseCategory category);
    int update(CourseCategory category);
    int deleteById(@Param("categoryId") Integer categoryId);
}