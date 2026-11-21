package com.art.controller.admin;

import com.art.common.Result;
import com.art.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 运营管理端 - 运营数据统计
 * 提供续课率、出勤率、教室利用率、教师课时、学员进度等统计接口
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC','FINANCE')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 续课率统计
     */
    @GetMapping("/renewal-rate")
    public Result<Map<String, Object>> renewalRate(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.getRenewalRate(startDate, endDate));
    }

    /**
     * 出勤率统计（按班级/课程）
     */
    @GetMapping("/attendance-rate")
    public Result<Map<String, Object>> attendanceRate(@RequestParam(required = false) Integer classId,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.getAttendanceRate(classId, startDate, endDate));
    }

    /**
     * 教室利用率
     */
    @GetMapping("/classroom-utilization")
    public Result<Map<String, Object>> classroomUtilization(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.getClassroomUtilization(startDate, endDate));
    }

    /**
     * 教师课时统计
     */
    @GetMapping("/teacher-hours")
    public Result<Map<String, Object>> teacherHours(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(statisticsService.getTeacherHours(startDate, endDate));
    }

    /**
     * 学员学习进度统计
     */
    @GetMapping("/student-progress")
    public Result<Map<String, Object>> studentProgress(@RequestParam Integer studentId) {
        return Result.success(statisticsService.getStudentProgress(studentId));
    }
}