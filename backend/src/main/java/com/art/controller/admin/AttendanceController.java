package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.Attendance;
import com.art.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 运营管理端 - 考勤管理（查看、修改）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 分页查询考勤记录
     */
    @GetMapping("/list")
    public Result<PageResult<Attendance>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer classId,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                               @RequestParam(required = false) Integer studentId) {
        return Result.success(attendanceService.pageAttendances(pageNum, pageSize, classId, date, studentId));
    }

    /**
     * 修改考勤状态（如补签）
     */
    @PutMapping("/{attendanceId}")
    public Result<Void> update(@PathVariable Integer attendanceId, @RequestParam String status) {
        attendanceService.updateStatus(attendanceId, status);
        return Result.success();
    }
}