package com.art.service.impl;

import com.art.dto.StudentCourseDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.StudentCourseService;
import com.art.util.SecurityUtil;
import com.art.vo.AppointmentDTO;
import com.art.vo.CourseScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class StudentCourseServiceImpl implements StudentCourseService {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseClassMapper classMapper;
    private final CourseCategoryMapper categoryMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final AppointmentMapper appointmentMapper;
    private final StudentMapper studentMapper;

    @Override
    public List<StudentCourseDTO> getMyCourses() {
        List<StudentCourseDTO> result = new ArrayList<>();
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return result;
        for (Enrollment en : enrollmentMapper.selectByStudentId(studentId)) {
            CourseClass cc = classMapper.selectById(en.getClassId());
            if (cc == null) continue;
            StudentCourseDTO dto = new StudentCourseDTO();
            dto.setEnrollmentId(en.getEnrollmentId());
            dto.setClassId(cc.getClassId());
            dto.setClassName(cc.getClassName());
            dto.setCategoryName(cc.getCategoryId() != null && categoryMapper.selectById(cc.getCategoryId()) != null
                    ? categoryMapper.selectById(cc.getCategoryId()).getCategoryName() : null);
            dto.setTeacherName(teacherMapper.selectById(cc.getTeacherId()) != null
                    ? teacherMapper.selectById(cc.getTeacherId()).getName() : null);
            dto.setSemester(cc.getSemester());
            dto.setTuition(cc.getTuition());
            dto.setAttendedCount(en.getAttendedCount());
            dto.setTotalHours(cc.getTotalHours());
            dto.setStatus(en.getStatus());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<CourseScheduleVO> getCourseSchedule(Integer enrollmentId) {
        List<CourseScheduleVO> result = new ArrayList<>();
        Enrollment en = enrollmentMapper.selectById(enrollmentId);
        if (en == null) return result;
        for (CourseSchedule cs : scheduleMapper.selectByConditions(null, null, null, null, en.getClassId())) {
            CourseScheduleVO vo = new CourseScheduleVO();
            vo.setScheduleId(cs.getScheduleId());
            vo.setClassId(cs.getClassId());
            vo.setTeacherId(cs.getTeacherId());
            vo.setClassroomId(cs.getClassroomId());
            vo.setCourseDate(cs.getCourseDate());
            vo.setStartTime(cs.getStartTime());
            vo.setEndTime(cs.getEndTime());
            vo.setHours(cs.getHours());
            vo.setStatus(cs.getStatus());
            vo.setRemark(cs.getRemark());
            CourseClass cc = classMapper.selectById(cs.getClassId());
            if (cc != null) {
                vo.setClassName(cc.getClassName());
                vo.setCategoryName(cc.getCategoryId() != null && categoryMapper.selectById(cc.getCategoryId()) != null
                        ? categoryMapper.selectById(cc.getCategoryId()).getCategoryName() : null);
                vo.setTeacherName(teacherMapper.selectById(cc.getTeacherId()) != null
                        ? teacherMapper.selectById(cc.getTeacherId()).getName() : null);
            }
            vo.setClassroomName(cs.getClassroomId() != null && classroomMapper.selectById(cs.getClassroomId()) != null
                    ? classroomMapper.selectById(cs.getClassroomId()).getClassroomName() : null);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AppointmentDTO> getMyAppointments() {
        List<AppointmentDTO> result = new ArrayList<>();
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return result;
        for (Appointment a : appointmentMapper.selectByStudent(studentId)) {
            AppointmentDTO dto = new AppointmentDTO();
            dto.setAppointmentId(a.getAppointmentId());
            dto.setStudentId(a.getStudentId());
            dto.setStudentName(studentMapper.selectById(a.getStudentId()) != null
                    ? studentMapper.selectById(a.getStudentId()).getName() : null);
            dto.setTeacherId(a.getTeacherId());
            dto.setTeacherName(teacherMapper.selectById(a.getTeacherId()) != null
                    ? teacherMapper.selectById(a.getTeacherId()).getName() : null);
            dto.setClassId(a.getClassId());
            CourseClass cc = a.getClassId() != null ? classMapper.selectById(a.getClassId()) : null;
            dto.setClassName(cc != null ? cc.getClassName() : null);
            dto.setAppointmentType(a.getAppointmentType());
            dto.setAppointmentTime(a.getAppointmentTime());
            dto.setStatus(a.getStatus());
            dto.setRemark(a.getRemark());
            result.add(dto);
        }
        return result;
    }
}
