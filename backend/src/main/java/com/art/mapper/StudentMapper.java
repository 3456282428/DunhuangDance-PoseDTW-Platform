package com.art.mapper;

import com.art.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface StudentMapper {
    List<Student> selectAll();
    Student selectById(@Param("studentId") Integer studentId);
    Student selectByUserId(@Param("userId") Integer userId);
    int insert(Student student);
    int update(Student student);
    int deleteById(@Param("studentId") Integer studentId);
    List<Student> selectByCondition(@Param("keyword") String keyword);
    Student selectByStudentNo(@Param("studentNo") String studentNo);
}