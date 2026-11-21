package com.art.mapper;

import com.art.entity.CourseClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface CourseClassMapper {
    List<CourseClass> selectAll();
    CourseClass selectById(@Param("classId") Integer classId);
    int insert(CourseClass courseClass);
    int update(CourseClass courseClass);
    int deleteById(@Param("classId") Integer classId);
    List<CourseClass> selectByCondition(@Param("className") String className,
                                        @Param("categoryId") Integer categoryId,
                                        @Param("courseId") Integer courseId);
}