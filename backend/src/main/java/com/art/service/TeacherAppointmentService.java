package com.art.service;

import com.art.vo.AppointmentVO;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherAppointmentService {
    List<AppointmentVO> listMyAppointments(String startDate, String endDate, String status);
    void confirmAppointment(Integer appointmentId, String action);
}