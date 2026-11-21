package com.art.controller.admin;

import com.art.common.Result;
import com.art.dto.ScheduleDTO;
import com.art.service.ScheduleService;
import com.art.vo.CourseScheduleVO;
import com.art.vo.RescheduleRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 运营管理端 - 课程排班管理
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 查询某周/某日的排课安排（日期可选，默认查全部）
     */
    @GetMapping("/list")
    public Result<List<CourseScheduleVO>> list(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                               @RequestParam(required = false) Integer teacherId,
                                               @RequestParam(required = false) Integer classroomId) {
        return Result.success(scheduleService.listSchedules(startDate, endDate, teacherId, classroomId));
    }

    /**
     * 新增排课（含教师/教室冲突检测，支持按周期批量生成）
     */
    @PostMapping
    public Result<Integer> create(@RequestBody ScheduleDTO scheduleDTO) {
        int count = scheduleService.createSchedule(scheduleDTO);
        return Result.success("成功排课 " + count + " 节", count);
    }

    /**
     * 修改排课（调课）
     */
    @PutMapping("/{scheduleId}")
    public Result<Void> update(@PathVariable Integer scheduleId, @RequestBody ScheduleDTO scheduleDTO) {
        scheduleService.updateSchedule(scheduleId, scheduleDTO);
        return Result.success();
    }

    /**
     * 取消排课
     */
    @DeleteMapping("/{scheduleId}")
    public Result<Void> delete(@PathVariable Integer scheduleId) {
        scheduleService.cancelSchedule(scheduleId);
        return Result.success();
    }

    /**
     * 调课申请列表（status 可选：待审核/已通过/已拒绝）
     */
    @GetMapping("/reschedule/list")
    public Result<List<RescheduleRequestVO>> rescheduleList(@RequestParam(required = false) String status) {
        return Result.success(scheduleService.listRescheduleRequests(status));
    }

    /**
     * 通过调课申请（自动回写排课时间）
     */
    @PutMapping("/reschedule/{requestId}/approve")
    public Result<Void> approve(@PathVariable Integer requestId, @RequestBody(required = false) Map<String, String> body) {
        scheduleService.approveReschedule(requestId, body != null ? body.get("remark") : null);
        return Result.success();
    }

    /**
     * 拒绝调课申请
     */
    @PutMapping("/reschedule/{requestId}/reject")
    public Result<Void> reject(@PathVariable Integer requestId, @RequestBody(required = false) Map<String, String> body) {
        scheduleService.rejectReschedule(requestId, body != null ? body.get("remark") : null);
        return Result.success();
    }
}
