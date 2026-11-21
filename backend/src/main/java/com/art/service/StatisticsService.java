package com.art.service;

import com.art.dto.DashboardOverviewDTO;
import java.time.LocalDate;
import java.util.Map;

/**
 * @author liukang&&heyan
 */
public interface StatisticsService {
    DashboardOverviewDTO getDashboardOverview();
    Map<String, Object> getRenewalRate(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getAttendanceRate(Integer classId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> getClassroomUtilization(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getTeacherHours(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getStudentProgress(Integer studentId);
}