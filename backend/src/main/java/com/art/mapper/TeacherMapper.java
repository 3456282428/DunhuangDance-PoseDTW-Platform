package com.art.mapper;

import com.art.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface TeacherMapper {
    List<Teacher> selectAll();
    Teacher selectById(@Param("teacherId") Integer teacherId);
    Teacher selectByUserId(@Param("userId") Integer userId);
    int insert(Teacher teacher);
    int update(Teacher teacher);
    int deleteById(@Param("teacherId") Integer teacherId);
    List<Teacher> selectByCondition(@Param("keyword") String keyword,
                                    @Param("employType") String employType);
    java.math.BigDecimal completedHours(@Param("teacherId") Integer teacherId);
    List<java.util.Map<String, Object>> classHours(@Param("teacherId") Integer teacherId);
    int deleteCoursesByTeacher(@Param("teacherId") Integer teacherId);
    int insertTeacherCourse(@Param("teacherId") Integer teacherId, @Param("courseId") Integer courseId);
}