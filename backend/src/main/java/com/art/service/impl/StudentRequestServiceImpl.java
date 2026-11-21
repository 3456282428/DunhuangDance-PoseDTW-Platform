package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.dto.StudentRequestDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.MessageService;
import com.art.service.StudentRequestService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class StudentRequestServiceImpl implements StudentRequestService {

    private final StudentRequestMapper requestMapper;
    private final CourseClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final AttendanceMapper attendanceMapper;
    private final MessageService messageService;

    @Override
    public void submit(StudentRequestDTO dto) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        StudentRequest req = new StudentRequest();
        req.setStudentId(studentId);
        req.setClassId(dto.getClassId());
        CourseClass cc = dto.getClassId() != null ? classMapper.selectById(dto.getClassId()) : null;
        req.setTeacherId(cc != null ? cc.getTeacherId() : null);
        req.setRequestType(dto.getRequestType());
        req.setRequestDate(dto.getRequestDate());
        req.setReason(dto.getReason());
        req.setStatus("待处理");
        req.setCreateTime(LocalDateTime.now());
        requestMapper.insert(req);
    }

    @Override
    public List<StudentRequestDTO> listMine() {
        List<StudentRequestDTO> result = new ArrayList<>();
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return result;
        for (StudentRequest r : requestMapper.selectByStudentId(studentId)) {
            result.add(toDTO(r));
        }
        return result;
    }

    @Override
    public List<StudentRequestDTO> listForTeacher() {
        List<StudentRequestDTO> result = new ArrayList<>();
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return result;
        for (StudentRequest r : requestMapper.selectByTeacherId(teacherId)) {
            result.add(toDTO(r));
        }
        return result;
    }

    @Override
    @Transactional
    public void handle(Integer requestId, String action, String remark) {
        StudentRequest req = requestMapper.selectById(requestId);
        if (req == null) throw new com.art.common.BusinessException("申请不存在");
        boolean pass = "APPROVE".equalsIgnoreCase(action);
        String status = pass ? BizConstants.REQ_APPROVED : BizConstants.REQ_REJECTED;
        requestMapper.updateStatus(requestId, status, remark, LocalDateTime.now());

        // 请假批准：自动在申请日期的本班排课上补登“请假”考勤（未登记时）
        if (pass && "请假".equals(req.getRequestType()) && req.getClassId() != null && req.getRequestDate() != null) {
            createLeaveAttendance(req);
        }

        // 站内通知学员
        Student s = studentMapper.selectById(req.getStudentId());
        if (s != null && s.getUserId() != null) {
            String title = pass ? "您的" + req.getRequestType() + "申请已批准" : "您的" + req.getRequestType() + "申请未通过";
            String content = pass ? "申请已批准。" + (remark != null && !remark.isBlank() ? "意见：" + remark : "")
                                  : "申请未通过。" + (remark != null && !remark.isBlank() ? "原因：" + remark : "请与教师沟通");
            messageService.send(s.getUserId(), "申请", title, content, "student_request", requestId);
        }
    }

    private void createLeaveAttendance(StudentRequest req) {
        LocalDate day = req.getRequestDate();
        List<CourseSchedule> daySchedules = scheduleMapper.selectByConditions(day, day, null, null, req.getClassId());
        // 申请日期当天该班没有排课时，审批回滚并明确提示，避免“批准了但考勤无痕迹”
        if (daySchedules.isEmpty())
            throw new com.art.common.BusinessException("申请日期 " + day + " 该班级没有排课，无法登记请假考勤，请核对请假日期");
        for (CourseSchedule cs : daySchedules) {
            boolean exists = attendanceMapper.selectByScheduleId(cs.getScheduleId()).stream()
                    .anyMatch(a -> req.getStudentId().equals(a.getStudentId()));
            if (exists) continue;
            Attendance a = new Attendance();
            a.setScheduleId(cs.getScheduleId());
            a.setStudentId(req.getStudentId());
            a.setRequestId(req.getRequestId());
            a.setStatus(BizConstants.ATT_LEAVE);
            a.setRecordTime(LocalDateTime.now());
            a.setRemark("请假申请批准自动登记");
            attendanceMapper.insert(a);
        }
    }

    private StudentRequestDTO toDTO(StudentRequest r) {
        StudentRequestDTO dto = new StudentRequestDTO();
        dto.setRequestId(r.getRequestId());
        dto.setStudentId(r.getStudentId());
        Student s = r.getStudentId() != null ? studentMapper.selectById(r.getStudentId()) : null;
        dto.setStudentName(s != null ? s.getName() : null);
        dto.setTeacherId(r.getTeacherId());
        Teacher t = r.getTeacherId() != null ? teacherMapper.selectById(r.getTeacherId()) : null;
        dto.setTeacherName(t != null ? t.getName() : null);
        dto.setClassId(r.getClassId());
        CourseClass cc = r.getClassId() != null ? classMapper.selectById(r.getClassId()) : null;
        dto.setClassName(cc != null ? cc.getClassName() : null);
        dto.setRequestType(r.getRequestType());
        dto.setRequestDate(r.getRequestDate());
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        dto.setReplyRemark(r.getReplyRemark());
        dto.setCreateTime(r.getCreateTime());
        return dto;
    }
}
