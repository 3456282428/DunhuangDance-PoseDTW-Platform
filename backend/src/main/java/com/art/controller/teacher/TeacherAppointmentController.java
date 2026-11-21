package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherAppointmentService;
import com.art.vo.AppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 预约学员查询
 * 查看预约本人课程的学员名单（试听/补课）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/appointment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAppointmentController {

    private final TeacherAppointmentService appointmentService;

    /**
     * 查询我的预约学员列表（按时间段筛选）
     */
    @GetMapping("/list")
    public Result<List<AppointmentVO>> listMyAppointments(@RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate,
                                                          @RequestParam(required = false) String status) {
        return Result.success(appointmentService.listMyAppointments(startDate, endDate, status));
    }

    /**
     * 确认预约（接受或拒绝）
     */
    @PutMapping("/{appointmentId}/confirm")
    public Result<Void> confirmAppointment(@PathVariable Integer appointmentId,
                                           @RequestParam String action) { // action: ACCEPT / REJECT
        appointmentService.confirmAppointment(appointmentId, action);
        return Result.success();
    }
}