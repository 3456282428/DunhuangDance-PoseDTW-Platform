package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.BusinessException;
import com.art.dto.RescheduleRequestDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.TeacherScheduleService;
import com.art.util.SecurityUtil;
import com.art.vo.AttendanceStudentVO;
import com.art.vo.RescheduleRequestVO;
import com.art.vo.ScheduleConflictVO;
import com.art.vo.ScheduleDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherScheduleServiceImpl implements TeacherScheduleService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseClassMapper classMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final AttendanceMapper attendanceMapper;
    private final StudentMapper studentMapper;
    private final RescheduleRequestMapper rescheduleMapper;

    @Override
    public List<CourseSchedule> listMySchedule(LocalDate startDate, LocalDate endDate) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (startDate == null) startDate = LocalDate.of(2000, 1, 1);
        if (endDate == null) endDate = LocalDate.of(2999, 12, 31);
        List<CourseSchedule> list = scheduleMapper.selectByTeacherAndDate(teacherId, startDate, endDate);
        for (CourseSchedule cs : list) {
            if (cs.getClassId() != null) {
                CourseClass cc = classMapper.selectById(cs.getClassId());
                if (cc != null) cs.setClassName(cc.getClassName());
            }
            if (cs.getClassroomId() != null) {
                Classroom room = classroomMapper.selectById(cs.getClassroomId());
                if (room != null) cs.setClassroomName(room.getClassroomName());
            }
            if (cs.getTeacherId() != null) {
                Teacher t = teacherMapper.selectById(cs.getTeacherId());
                if (t != null) cs.setTeacherName(t.getName());
            }
        }
        return list;
    }

    @Override
    public ScheduleDetailVO getScheduleDetail(Integer scheduleId) {
        ScheduleDetailVO vo = new ScheduleDetailVO();
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) return vo;
        vo.setScheduleId(schedule.getScheduleId());
        vo.setClassId(schedule.getClassId());
        vo.setTeacherId(schedule.getTeacherId());
        vo.setClassroomId(schedule.getClassroomId());
        vo.setCourseDate(schedule.getCourseDate());
        vo.setStartTime(schedule.getStartTime());
        vo.setEndTime(schedule.getEndTime());
        vo.setHours(schedule.getHours());
        vo.setStatus(schedule.getStatus());
        vo.setRemark(schedule.getRemark());
        CourseClass cc = schedule.getClassId() != null ? classMapper.selectById(schedule.getClassId()) : null;
        vo.setClassName(cc != null ? cc.getClassName() : null);
        vo.setTeacherName(teacherMapper.selectById(schedule.getTeacherId()) != null
                ? teacherMapper.selectById(schedule.getTeacherId()).getName() : null);
        vo.setClassroomName(classroomMapper.selectById(schedule.getClassroomId()) != null
                ? classroomMapper.selectById(schedule.getClassroomId()).getClassroomName() : null);

        List<AttendanceStudentVO> studentList = new ArrayList<>();
        List<Attendance> existing = attendanceMapper.selectByScheduleId(scheduleId);
        if (cc != null) {
            for (Enrollment en : enrollmentMapper.selectByClassId(cc.getClassId())) {
                AttendanceStudentVO aVo = new AttendanceStudentVO();
                aVo.setStudentId(en.getStudentId());
                aVo.setStudentName(studentMapper.selectById(en.getStudentId()) != null
                        ? studentMapper.selectById(en.getStudentId()).getName() : null);
                Attendance matched = null;
                for (Attendance a : existing) {
                    if (en.getStudentId().equals(a.getStudentId())) { matched = a; break; }
                }
                aVo.setStatus(matched != null ? matched.getStatus() : "未点名");
                aVo.setRemark(matched != null ? matched.getRemark() : null);
                studentList.add(aVo);
            }
        }
        vo.setStudentList(studentList);
        return vo;
    }

    @Override
    public void requestReschedule(Integer scheduleId, RescheduleRequestDTO requestDTO) {
        CourseSchedule source = scheduleMapper.selectById(scheduleId);
        if (source == null) throw new BusinessException("原排课不存在");
        if (requestDTO.getNewDate() == null || requestDTO.getNewStartTime() == null || requestDTO.getNewEndTime() == null)
            throw new BusinessException("请选择新日期与时间段");
        if (!requestDTO.getNewEndTime().isAfter(requestDTO.getNewStartTime()))
            throw new BusinessException("结束时间必须晚于开始时间");
        // 同一节课存在待审核申请时禁止重复提交
        for (RescheduleRequest old : rescheduleMapper.selectByScheduleId(scheduleId)) {
            if (BizConstants.RR_PENDING.equals(old.getStatus()))
                throw new BusinessException("该节课已有待审核的调课申请，请勿重复提交");
        }
        boolean swap = "交换".equals(requestDTO.getRequestType());
        List<ScheduleConflictVO> conflicts = checkConflict(scheduleId, requestDTO.getNewDate(),
                requestDTO.getNewStartTime(), requestDTO.getNewEndTime());
        if (swap) {
            // 交换：必须指定且只能指定一个真实冲突的目标课程
            if (requestDTO.getTargetScheduleId() == null)
                throw new BusinessException("交换调课必须指定要交换的课程");
            boolean match = conflicts.stream().anyMatch(c -> requestDTO.getTargetScheduleId().equals(c.getScheduleId()));
            if (!match) throw new BusinessException("所选交换课程与目标时段不冲突，无需交换");
            if (conflicts.size() > 1)
                throw new BusinessException("目标时段存在多节冲突课程，无法两两交换，请更换时段");
        } else if (!conflicts.isEmpty()) {
            throw new BusinessException("目标时段已有其他课程（" + conflicts.get(0).getSummary()
                    + "），请更换时段或选择与其交换");
        }

        RescheduleRequest req = new RescheduleRequest();
        req.setScheduleId(scheduleId);
        req.setRequestType(swap ? "交换" : BizConstants.RR_MOVE);
        req.setTargetScheduleId(swap ? requestDTO.getTargetScheduleId() : null);
        req.setNewDate(requestDTO.getNewDate());
        req.setNewStartTime(requestDTO.getNewStartTime());
        req.setNewEndTime(requestDTO.getNewEndTime());
        req.setReason(requestDTO.getReason());
        req.setStatus(BizConstants.RR_PENDING);
        req.setApplyTime(LocalDateTime.now());
        rescheduleMapper.insert(req);
    }

    @Override
    public List<ScheduleConflictVO> checkConflict(Integer scheduleId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        CourseSchedule source = scheduleMapper.selectById(scheduleId);
        if (source == null) throw new BusinessException("原排课不存在");
        if (date == null || startTime == null || endTime == null) throw new BusinessException("请先选齐日期与时间段");
        List<ScheduleConflictVO> result = new ArrayList<>();
        for (CourseSchedule cs : scheduleMapper.selectOverlaps(date, startTime, endTime, scheduleId)) {
            ScheduleConflictVO vo = new ScheduleConflictVO();
            vo.setScheduleId(cs.getScheduleId());
            vo.setCourseDate(cs.getCourseDate());
            vo.setStartTime(cs.getStartTime());
            vo.setEndTime(cs.getEndTime());
            List<String> reasons = new ArrayList<>();
            if (source.getTeacherId() != null && source.getTeacherId().equals(cs.getTeacherId())) reasons.add("教师冲突");
            if (source.getClassroomId() != null && source.getClassroomId().equals(cs.getClassroomId())) reasons.add("教室冲突");
            if (source.getClassId() != null && source.getClassId().equals(cs.getClassId())) reasons.add("班级冲突");
            if (reasons.isEmpty()) continue;
            CourseClass cc = cs.getClassId() != null ? classMapper.selectById(cs.getClassId()) : null;
            if (cc != null) vo.setClassName(cc.getClassName());
            Teacher t = cs.getTeacherId() != null ? teacherMapper.selectById(cs.getTeacherId()) : null;
            if (t != null) vo.setTeacherName(t.getName());
            Classroom room = cs.getClassroomId() != null ? classroomMapper.selectById(cs.getClassroomId()) : null;
            if (room != null) vo.setClassroomName(room.getClassroomName());
            vo.setReasons(reasons);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<RescheduleRequestVO> getMyRescheduleRequests() {
        List<RescheduleRequestVO> result = new ArrayList<>();
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        Set<Integer> myScheduleIds = new HashSet<>();
        for (CourseSchedule cs : scheduleMapper.selectByConditions(null, null, teacherId, null, null)) {
            myScheduleIds.add(cs.getScheduleId());
        }
        for (RescheduleRequest rr : rescheduleMapper.selectAll()) {
            if (!myScheduleIds.contains(rr.getScheduleId())) continue;
            RescheduleRequestVO vo = new RescheduleRequestVO();
            vo.setRequestId(rr.getRequestId());
            vo.setScheduleId(rr.getScheduleId());
            vo.setRequestType(rr.getRequestType() != null ? rr.getRequestType() : BizConstants.RR_MOVE);
            vo.setTargetScheduleId(rr.getTargetScheduleId());
            CourseSchedule cs = scheduleMapper.selectById(rr.getScheduleId());
            if (cs != null) {
                vo.setClassName(classMapper.selectById(cs.getClassId()) != null
                        ? classMapper.selectById(cs.getClassId()).getClassName() : null);
                vo.setOriginalDate(cs.getCourseDate());
                vo.setOriginalStartTime(cs.getStartTime());
                vo.setOriginalEndTime(cs.getEndTime());
            }
            vo.setNewDate(rr.getNewDate());
            vo.setNewStartTime(rr.getNewStartTime());
            vo.setNewEndTime(rr.getNewEndTime());
            vo.setReason(rr.getReason());
            vo.setStatus(rr.getStatus());
            vo.setApplyTime(rr.getApplyTime() != null ? rr.getApplyTime().toString() : null);
            result.add(vo);
        }
        return result;
    }
}
