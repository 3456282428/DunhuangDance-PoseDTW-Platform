package com.art.mapper;

import com.art.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface ClassroomMapper {
    List<Classroom> selectAll();
    Classroom selectById(@Param("classroomId") Integer classroomId);
    int insert(Classroom classroom);
    int update(Classroom classroom);
    int deleteById(@Param("classroomId") Integer classroomId);
}