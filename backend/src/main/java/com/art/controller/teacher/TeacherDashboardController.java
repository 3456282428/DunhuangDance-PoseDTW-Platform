package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherDashboardService;
import com.art.vo.CourseScheduleVO;
import com.art.vo.TodoItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 教师端 - 首页概括
 * 提供今日课表、待办事项、请假提醒、未批作业数等
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherDashboardController {

    private final TeacherDashboardService dashboardService;

    /**
     * 获取首页概括数据
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary() {
        return Result.success(dashboardService.getDashboardSummary());
    }

    /**
     * 获取今日课表
     */
    @GetMapping("/today-schedule")
    public Result<List<CourseScheduleVO>> getTodaySchedule() {
        return Result.success(dashboardService.getTodaySchedule());
    }

    /**
     * 获取待办事项（未批作业、未处理反馈等）
     */
    @GetMapping("/todos")
    public Result<List<TodoItemVO>> getTodos() {
        return Result.success(dashboardService.getTodos());
    }
}