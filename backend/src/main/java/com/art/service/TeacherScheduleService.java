package com.art.service;

import com.art.dto.RescheduleRequestDTO;
import com.art.entity.CourseSchedule;
import com.art.vo.ScheduleConflictVO;
import com.art.vo.ScheduleDetailVO;
import com.art.vo.RescheduleRequestVO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherScheduleService {
    List<CourseSchedule> listMySchedule(LocalDate startDate, LocalDate endDate);
    ScheduleDetailVO getScheduleDetail(Integer scheduleId);
    void requestReschedule(Integer scheduleId, RescheduleRequestDTO requestDTO);
    List<RescheduleRequestVO> getMyRescheduleRequests();
    /** 检测某排课改到目标时段后的冲突课程明细 */
    List<ScheduleConflictVO> checkConflict(Integer scheduleId, LocalDate date, LocalTime startTime, LocalTime endTime);
}