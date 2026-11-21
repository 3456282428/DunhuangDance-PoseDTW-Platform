package com.art.controller.teacher;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.AttendanceBatchDTO;
import com.art.service.TeacherAttendanceService;
import com.art.vo.AttendanceHistoryVO;
import com.art.vo.AttendanceStudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 教学考勤管理
 * 按课程点名/签到，记录出勤状态
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAttendanceController {

    private final TeacherAttendanceService attendanceService;

    /**
     * 获取某次课程的学员名单及当前考勤状态
     */
    @GetMapping("/{scheduleId}/students")
    public Result<List<AttendanceStudentVO>> getAttendanceList(@PathVariable Integer scheduleId) {
        return Result.success(attendanceService.getAttendanceList(scheduleId));
    }

    /**
     * 批量更新考勤状态（点名）
     */
    @PostMapping("/{scheduleId}/batch-update")
    public Result<Void> batchUpdateAttendance(@PathVariable Integer scheduleId,
                                              @RequestBody AttendanceBatchDTO batchDTO) {
        attendanceService.batchUpdateAttendance(scheduleId, batchDTO);
        return Result.success();
    }

    /**
     * 查询历史考勤记录（可按班级、日期筛选）
     */
    @GetMapping("/history")
    public Result<PageResult<AttendanceHistoryVO>> getHistory(@RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) Integer classId,
                                                     @RequestParam(required = false) String date,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(attendanceService.getHistory(pageNum, pageSize, classId, date, status, keyword));
    }
}