package com.art.service;

import com.art.common.PageResult;
import com.art.entity.Attendance;
import java.time.LocalDate;

/**
 * @author liukang&&heyan
 */
public interface AttendanceService {
    PageResult<Attendance> pageAttendances(Integer pageNum, Integer pageSize, Integer classId, LocalDate date, Integer studentId);
    void updateStatus(Integer attendanceId, String status);
}