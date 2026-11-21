package com.art.controller.student;

import com.art.common.Result;
import com.art.dto.StudentCourseDTO;
import com.art.service.StudentCourseService;
import com.art.vo.CourseScheduleVO;
import com.art.vo.AppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员端 - 在学课程与我的预约
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/course")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    /**
     * 获取当前学员的在学课程列表
     */
    @GetMapping("/list")
    public Result<List<StudentCourseDTO>> listMyCourses() {
        return Result.success(studentCourseService.getMyCourses());
    }

    /**
     * 获取某门课程的详细课表
     */
    @GetMapping("/{enrollmentId}/schedule")
    public Result<List<CourseScheduleVO>> getCourseSchedule(@PathVariable Integer enrollmentId) {
        return Result.success(studentCourseService.getCourseSchedule(enrollmentId));
    }

    /**
     * 获取当前学员的预约记录（试听/补课）
     */
    @GetMapping("/appointments")
    public Result<List<AppointmentDTO>> getMyAppointments() {
        return Result.success(studentCourseService.getMyAppointments());
    }
}