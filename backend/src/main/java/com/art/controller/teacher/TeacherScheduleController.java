package com.art.controller.teacher;

import com.art.common.Result;
import com.art.dto.RescheduleRequestDTO;
import com.art.dto.ScheduleQueryDTO;
import com.art.entity.CourseSchedule;
import com.art.service.TeacherScheduleService;
import com.art.vo.RescheduleRequestVO;
import com.art.vo.ScheduleConflictVO;
import com.art.vo.ScheduleDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 教师端 - 课程排班管理
 * 查看个人课表、申请调课等
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherScheduleController {

    private final TeacherScheduleService scheduleService;

    /**
     * 查询个人某时间段内的排课
     */
    @GetMapping("/list")
    public Result<List<CourseSchedule>> listMySchedule(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(scheduleService.listMySchedule(startDate, endDate));
    }

    /**
     * 实时检测目标时段的冲突课程（选时间后调用，用于提示是否可交换）
     */
    @GetMapping("/conflict-check")
    public Result<List<ScheduleConflictVO>> conflictCheck(@RequestParam Integer scheduleId,
                                                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                          @RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime startTime,
                                                          @RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime endTime) {
        return Result.success(scheduleService.checkConflict(scheduleId, date, startTime, endTime));
    }

    /**
     * 查看某次课程的详细信息（包含学员名单）
     */
    @GetMapping("/{scheduleId}")
    public Result<ScheduleDetailVO> getScheduleDetail(@PathVariable Integer scheduleId) {
        return Result.success(scheduleService.getScheduleDetail(scheduleId));
    }

    /**
     * 申请调课（提交新时间，待教务审核）
     */
    @PostMapping("/{scheduleId}/reschedule-request")
    public Result<Void> requestReschedule(@PathVariable Integer scheduleId,
                                          @RequestBody RescheduleRequestDTO requestDTO) {
        scheduleService.requestReschedule(scheduleId, requestDTO);
        return Result.success();
    }

    /**
     * 查询我的调课申请记录
     */
    @GetMapping("/reschedule-requests")
    public Result<List<RescheduleRequestVO>> getMyRescheduleRequests() {
        return Result.success(scheduleService.getMyRescheduleRequests());
    }
}