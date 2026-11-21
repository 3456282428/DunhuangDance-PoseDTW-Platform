package com.art.controller.student;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.Attendance;
import com.art.service.StudentAttendanceService;
import com.art.vo.AttendanceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 学员端 - 完成课时与考勤记录
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAttendanceController {

    private final StudentAttendanceService attendanceService;

    /**
     * 分页查询我的考勤记录
     */
    @GetMapping("/list")
    public Result<PageResult<Attendance>> listMyAttendance(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                           @RequestParam(required = false) Integer classId) {
        return Result.success(attendanceService.pageMyAttendance(pageNum, pageSize, startDate, endDate, classId));
    }

    /**
     * 获取考勤汇总（总课时、已上课时、缺勤次数等）
     */
    @GetMapping("/summary")
    public Result<AttendanceSummaryVO> getAttendanceSummary() {
        return Result.success(attendanceService.getMyAttendanceSummary());
    }
}