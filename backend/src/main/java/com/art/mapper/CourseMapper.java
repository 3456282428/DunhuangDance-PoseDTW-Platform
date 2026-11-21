package com.art.mapper;

import com.art.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface CourseMapper {
    List<Course> selectByCondition(@Param("courseName") String courseName,
                                   @Param("category") String category,
                                   @Param("status") Integer status);
    List<Course> selectAllOnSale();
    Course selectById(@Param("courseId") Integer courseId);
    Course selectByName(@Param("courseName") String courseName);
    int insert(Course course);
    int update(Course course);
    int deleteById(@Param("courseId") Integer courseId);
    int countClasses(@Param("courseId") Integer courseId);
}
