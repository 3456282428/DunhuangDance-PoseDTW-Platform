package com.art.mapper;

import com.art.entity.StudentComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface StudentCommentMapper {
    int insert(StudentComment comment);
    List<StudentComment> selectByStudent(@Param("studentId") Integer studentId,
                                         @Param("classId") Integer classId);
}
