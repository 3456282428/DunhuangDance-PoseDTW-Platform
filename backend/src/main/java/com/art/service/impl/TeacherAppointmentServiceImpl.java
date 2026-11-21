package com.art.service.impl;

import com.art.entity.Student;
import com.art.entity.Appointment;
import com.art.entity.CourseClass;
import com.art.mapper.AppointmentMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.StudentMapper;
import com.art.mapper.TeacherMapper;
import com.art.service.TeacherAppointmentService;
import com.art.util.SecurityUtil;
import com.art.vo.AppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherAppointmentServiceImpl implements TeacherAppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseClassMapper classMapper;
    private final com.art.service.MessageService messageService;

    @Override
    public List<AppointmentVO> listMyAppointments(String startDate, String endDate, String status) {
        List<AppointmentVO> result = new ArrayList<>();
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        for (Appointment a : appointmentMapper.selectByTeacher(teacherId, status)) {
            if (a.getAppointmentTime() == null) continue;
            LocalDate d = a.getAppointmentTime().toLocalDate();
            if (startDate != null && !startDate.isEmpty() && d.isBefore(LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE))) continue;
            if (endDate != null && !endDate.isEmpty() && d.isAfter(LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE))) continue;
            result.add(toVO(a));
        }
        return result;
    }

    @Override
    public void confirmAppointment(Integer appointmentId, String action) {
        Appointment a = appointmentMapper.selectById(appointmentId);
        if (a == null) return;
        if ("ACCEPT".equalsIgnoreCase(action)) {
            a.setStatus("已接受");
        } else if ("REJECT".equalsIgnoreCase(action)) {
            a.setStatus("已拒绝");
        } else {
            return;
        }
        appointmentMapper.update(a);
        Student stu = studentMapper.selectById(a.getStudentId());
        if (stu != null && stu.getUserId() != null) {
            boolean accept = "ACCEPT".equalsIgnoreCase(action);
            messageService.send(stu.getUserId(), "预约",
                    accept ? "您的预约已接受" : "您的预约未通过",
                    accept ? "教师已接受您的预约，请按时参加。" : "教师未能接受该预约，请改约其他时间。",
                    "appointment", appointmentId);
        }
    }

    private AppointmentVO toVO(Appointment a) {
        AppointmentVO vo = new AppointmentVO();
        vo.setAppointmentId(a.getAppointmentId());
        vo.setStudentId(a.getStudentId());
        vo.setStudentName(studentMapper.selectById(a.getStudentId()) != null
                ? studentMapper.selectById(a.getStudentId()).getName() : null);
        vo.setTeacherId(a.getTeacherId());
        vo.setTeacherName(teacherMapper.selectById(a.getTeacherId()) != null
                ? teacherMapper.selectById(a.getTeacherId()).getName() : null);
        vo.setClassId(a.getClassId());
        CourseClass cc = a.getClassId() != null ? classMapper.selectById(a.getClassId()) : null;
        vo.setClassName(cc != null ? cc.getClassName() : null);
        vo.setAppointmentType(a.getAppointmentType());
        vo.setAppointmentTime(a.getAppointmentTime());
        vo.setStatus(a.getStatus());
        vo.setRemark(a.getRemark());
        return vo;
    }
}

