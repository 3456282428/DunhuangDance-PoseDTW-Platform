package com.art.mapper;

import com.art.entity.Attendance;
import com.art.vo.AttendanceHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface AttendanceMapper {
    Attendance selectById(@Param("attendanceId") Integer attendanceId);
    int insert(Attendance attendance);
    int updateStatus(@Param("attendanceId") Integer attendanceId, @Param("status") String status);
    int updateStatusAndRemark(@Param("attendanceId") Integer attendanceId,
                              @Param("status") String status,
                              @Param("remark") String remark);
    List<Attendance> selectByScheduleId(@Param("scheduleId") Integer scheduleId);
    List<Attendance> selectByStudentAndDateRange(@Param("studentId") Integer studentId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("classId") Integer classId);
    List<Attendance> selectByConditions(@Param("classId") Integer classId,
                                        @Param("date") LocalDate date,
                                        @Param("studentId") Integer studentId);
    int countByScheduleAndStatus(@Param("scheduleId") Integer scheduleId, @Param("status") String status);

    /** 教师端考勤历史：关联排课/班级/学员，支持筛选 */
    List<AttendanceHistoryVO> selectHistory(@Param("teacherId") Integer teacherId,
                                            @Param("classId") Integer classId,
                                            @Param("date") LocalDate date,
                                            @Param("status") String status,
                                            @Param("keyword") String keyword);
}