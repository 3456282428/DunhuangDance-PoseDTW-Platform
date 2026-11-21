package com.art.service;

import com.art.vo.CourseScheduleVO;
import com.art.vo.TodoItemVO;
import java.util.List;
import java.util.Map;

/**
 * @author liukang&&heyan
 */
public interface TeacherDashboardService {
    Map<String, Object> getDashboardSummary();
    List<CourseScheduleVO> getTodaySchedule();
    List<TodoItemVO> getTodos();
}