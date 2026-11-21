package com.art.service;

import com.art.dto.StudentCourseDTO;
import com.art.vo.CourseScheduleVO;
import com.art.vo.AppointmentDTO;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface StudentCourseService {
    List<StudentCourseDTO> getMyCourses();
    List<CourseScheduleVO> getCourseSchedule(Integer enrollmentId);
    List<AppointmentDTO> getMyAppointments();
}