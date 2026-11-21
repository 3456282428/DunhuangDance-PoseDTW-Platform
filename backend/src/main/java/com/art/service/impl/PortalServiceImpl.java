package com.art.service.impl;

import com.art.common.MobileValidator;
import com.art.dto.VisitorBookingDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.PortalService;
import com.art.vo.PortalCourseVO;
import com.art.vo.PortalScheduleVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class PortalServiceImpl implements PortalService {

    private final CourseClassMapper classMapper;
    private final CourseCategoryMapper categoryMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final VisitorBookingMapper bookingMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PortalCourseVO> listCourses(Integer categoryId, String ageGroup, String level, String keyword) {
        List<PortalCourseVO> result = new ArrayList<>();
        for (CourseClass cc : classMapper.selectAll()) {
            if (cc.getStatus() != null && cc.getStatus() != 1) continue;
            if (categoryId != null && !categoryId.equals(cc.getCategoryId())) continue;
            if (ageGroup != null && !ageGroup.isEmpty() && !ageGroup.equals(cc.getAgeGroup())) continue;
            if (level != null && !level.isEmpty() && !level.equals(cc.getLevel())) continue;
            if (keyword != null && !keyword.isEmpty()
                    && !cc.getClassName().contains(keyword)) continue;
            result.add(toVO(cc));
        }
        return result;
    }

    @Override
    public PortalCourseVO getCourseDetail(Integer classId) {
        CourseClass cc = classMapper.selectById(classId);
        return cc == null ? null : toVO(cc);
    }

    @Override
    public List<PortalScheduleVO> listCourseSchedules(Integer classId) {
        List<PortalScheduleVO> result = new ArrayList<>();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(60);
        for (CourseSchedule s : scheduleMapper.selectByConditions(start, end, null, null, classId)) {
            if (!"待上课".equals(s.getStatus())) continue;
            PortalScheduleVO vo = new PortalScheduleVO();
            vo.setScheduleId(s.getScheduleId());
            vo.setClassId(s.getClassId());
            vo.setCourseDate(s.getCourseDate());
            vo.setStartTime(s.getStartTime());
            vo.setEndTime(s.getEndTime());
            vo.setStatus(s.getStatus());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void submitTrialBooking(VisitorBookingDTO dto) {
        MobileValidator.requireNonNull(dto.getPhone(), "手机号");
        VisitorBooking b = new VisitorBooking();
        b.setName(dto.getName());
        b.setPhone(dto.getPhone());
        b.setClassId(dto.getClassId());
        CourseClass cc = dto.getClassId() != null ? classMapper.selectById(dto.getClassId()) : null;
        b.setTeacherId(cc != null ? cc.getTeacherId() : dto.getTeacherId());
        b.setPreferredTime(dto.getPreferredTime());
        b.setRemark(dto.getRemark());
        b.setStatus("待确认");
        b.setCreateTime(LocalDateTime.now());
        bookingMapper.insert(b);

        // 同步到系统用户表（角色：游客），以手机号为用户名，已存在则不重复创建
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()
                && userMapper.findByUsername(dto.getPhone()) == null) {
            SysUser visitor = new SysUser();
            visitor.setUsername(dto.getPhone());
            visitor.setPassword(passwordEncoder.encode("123456"));
            visitor.setRole("VISITOR");
            visitor.setRealName(dto.getName());
            visitor.setPhone(dto.getPhone());
            visitor.setStatus(1);
            userMapper.insert(visitor);
        }
    }

    @Override
    public List<VisitorBookingDTO> listMyBookings(String phone) {
        List<VisitorBookingDTO> result = new ArrayList<>();
        for (VisitorBooking b : bookingMapper.selectByPhone(phone)) {
            result.add(toDTO(b));
        }
        return result;
    }

    @Override
    public List<VisitorBookingDTO> listAllBookings(String status, String keyword) {
        List<VisitorBookingDTO> result = new ArrayList<>();
        for (VisitorBooking b : bookingMapper.selectByCondition(status, keyword)) {
            result.add(toDTO(b));
        }
        return result;
    }

    @Override
    public void handleBooking(Integer bookingId, String status) {
        bookingMapper.updateStatus(bookingId, status, LocalDateTime.now());
    }

    @Override
    public Object getPortalContent() {
        SystemConfig cfg = configMapper.selectByKey("portal");
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(cfg.getConfigValue(), Object.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private PortalCourseVO toVO(CourseClass cc) {
        PortalCourseVO vo = new PortalCourseVO();
        vo.setClassId(cc.getClassId());
        vo.setClassName(cc.getClassName());
        vo.setCategoryId(cc.getCategoryId());
        CourseCategory cat = cc.getCategoryId() != null ? categoryMapper.selectById(cc.getCategoryId()) : null;
        vo.setCategoryName(cat != null ? cat.getCategoryName() : null);
        Teacher t = cc.getTeacherId() != null ? teacherMapper.selectById(cc.getTeacherId()) : null;
        vo.setTeacherName(t != null ? t.getName() : null);
        Classroom room = cc.getClassroomId() != null ? classroomMapper.selectById(cc.getClassroomId()) : null;
        vo.setClassroomName(room != null ? room.getClassroomName() : null);
        vo.setSemester(cc.getSemester());
        vo.setTuition(cc.getTuition());
        vo.setTotalHours(cc.getTotalHours());
        vo.setLessonDuration(cc.getLessonDuration());
        vo.setAgeGroup(cc.getAgeGroup());
        vo.setLevel(cc.getLevel());
        vo.setDescription(cc.getDescription());
        vo.setMaxStudents(cc.getMaxStudents());
        vo.setCurrentStudents(enrollmentMapper.selectByClassId(cc.getClassId()).size());
        return vo;
    }

    private VisitorBookingDTO toDTO(VisitorBooking b) {
        VisitorBookingDTO dto = new VisitorBookingDTO();
        dto.setBookingId(b.getBookingId());
        dto.setName(b.getName());
        dto.setPhone(b.getPhone());
        dto.setClassId(b.getClassId());
        CourseClass cc = b.getClassId() != null ? classMapper.selectById(b.getClassId()) : null;
        dto.setClassName(cc != null ? cc.getClassName() : null);
        dto.setTeacherId(b.getTeacherId());
        Teacher t = b.getTeacherId() != null ? teacherMapper.selectById(b.getTeacherId()) : null;
        dto.setTeacherName(t != null ? t.getName() : null);
        dto.setPreferredTime(b.getPreferredTime());
        dto.setRemark(b.getRemark());
        dto.setStatus(b.getStatus());
        dto.setCreateTime(b.getCreateTime());
        return dto;
    }
}
