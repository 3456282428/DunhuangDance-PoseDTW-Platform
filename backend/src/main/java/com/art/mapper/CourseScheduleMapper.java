package com.art.mapper;

import com.art.entity.CourseSchedule;
import com.art.vo.CourseScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface CourseScheduleMapper {
    List<CourseSchedule> selectByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    List<CourseSchedule> selectByConditions(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("teacherId") Integer teacherId,
                                            @Param("classroomId") Integer classroomId,
                                            @Param("classId") Integer classId);

    /** 关联班级/教师/教室名称的视图查询 */
    List<CourseScheduleVO> selectVOByConditions(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("teacherId") Integer teacherId,
                                                @Param("classroomId") Integer classroomId,
                                                @Param("classId") Integer classId);

    CourseSchedule selectById(@Param("scheduleId") Integer scheduleId);
    int insert(CourseSchedule schedule);
    int update(CourseSchedule schedule);
    int deleteById(@Param("scheduleId") Integer scheduleId);
    List<CourseSchedule> selectByTeacherAndDate(@Param("teacherId") Integer teacherId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /** 同一教师在指定日期时间段的冲突数量（重叠时间段，排除已取消与自身） */
    int countTeacherConflict(@Param("teacherId") Integer teacherId,
                             @Param("courseDate") LocalDate courseDate,
                             @Param("startTime") LocalTime startTime,
                             @Param("endTime") LocalTime endTime,
                             @Param("excludeId") Integer excludeId);

    /** 同一教室在指定日期时间段的冲突数量 */
    int countClassroomConflict(@Param("classroomId") Integer classroomId,
                               @Param("courseDate") LocalDate courseDate,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime") LocalTime endTime,
                               @Param("excludeId") Integer excludeId);

    /** 同一班级在指定日期时间段的冲突数量 */
    int countClassConflict(@Param("classId") Integer classId,
                           @Param("courseDate") LocalDate courseDate,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime,
                           @Param("excludeId") Integer excludeId);

    /** 指定日期时间段内所有时间重叠且未取消的排课（用于冲突明细展示与交换选择） */
    List<CourseSchedule> selectOverlaps(@Param("courseDate") LocalDate courseDate,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime,
                                        @Param("excludeId") Integer excludeId);
}
