package com.art.service;

import com.art.common.PageResult;
import com.art.entity.Attendance;
import com.art.vo.AttendanceSummaryVO;
import java.time.LocalDate;

/**
 * @author liukang&&heyan
 */
public interface StudentAttendanceService {
    PageResult<Attendance> pageMyAttendance(Integer pageNum, Integer pageSize, LocalDate startDate, LocalDate endDate, Integer classId);
    AttendanceSummaryVO getMyAttendanceSummary();
}