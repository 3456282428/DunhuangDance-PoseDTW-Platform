package com.art.mapper;

import com.art.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface AppointmentMapper {
    Appointment selectById(@Param("appointmentId") Integer appointmentId);
    int insert(Appointment appointment);
    int update(Appointment appointment);
    List<Appointment> selectByTeacher(@Param("teacherId") Integer teacherId,
                                      @Param("status") String status);
    List<Appointment> selectByStudent(@Param("studentId") Integer studentId);
}
