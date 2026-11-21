package com.art.mapper;

import com.art.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface EnrollmentMapper {
    List<Enrollment> selectAll();
    Enrollment selectById(@Param("enrollmentId") Integer enrollmentId);
    int insert(Enrollment enrollment);
    int update(Enrollment enrollment);
    int updateStatus(@Param("enrollmentId") Integer enrollmentId, @Param("status") String status);
    int deleteById(@Param("enrollmentId") Integer enrollmentId);
    List<Enrollment> selectByStudentId(@Param("studentId") Integer studentId);
    Enrollment selectByStudentAndClass(@Param("studentId") Integer studentId, @Param("classId") Integer classId);
    List<Enrollment> selectByClassId(@Param("classId") Integer classId);
    int updateAttendCount(@Param("enrollmentId") Integer enrollmentId,
                          @Param("attendedCount") Integer attendedCount,
                          @Param("absentCount") Integer absentCount);
}