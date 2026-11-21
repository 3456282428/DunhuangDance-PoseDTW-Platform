package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.BusinessException;
import com.art.dto.ScheduleDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.MessageService;
import com.art.service.ScheduleService;
import com.art.vo.CourseScheduleVO;
import com.art.vo.RescheduleRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class ScheduleServiceImpl implements ScheduleService {

    private final CourseScheduleMapper scheduleMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final RescheduleRequestMapper rescheduleMapper;
    private final MessageService messageService;

    @Override
    public List<CourseScheduleVO> listSchedules(LocalDate startDate, LocalDate endDate, Integer teacherId, Integer classroomId) {
        return scheduleMapper.selectVOByConditions(startDate, endDate, teacherId, classroomId, null);
    }

    @Override
    @Transactional
    public int createSchedule(ScheduleDTO dto) {
        // 计算课时长（小时）
        double hours = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes() / 60.0;
        if (hours <= 0) throw new BusinessException("结束时间必须晚于开始时间");

        List<LocalDate> dates = resolveDates(dto);
        int created = 0;
        for (LocalDate date : dates) {
            checkConflict(dto, date, null);
            CourseSchedule s = new CourseSchedule();
            BeanUtils.copyProperties(dto, s, "scheduleId", "rangeStart", "rangeEnd", "weekdays");
            s.setCourseDate(date);
            s.setHours(Math.round(hours * 100) / 100.0);
            s.setStatus(BizConstants.SCHED_PENDING);
            scheduleMapper.insert(s);
            created++;
        }
        return created;
    }

    @Override
    public void updateSchedule(Integer scheduleId, ScheduleDTO dto) {
        CourseSchedule existing = scheduleMapper.selectById(scheduleId);
        if (existing == null) throw new BusinessException("排课记录不存在");
        checkConflict(dto, dto.getCourseDate(), scheduleId);
        BeanUtils.copyProperties(dto, existing, "scheduleId", "rangeStart", "rangeEnd", "weekdays");
        double hours = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes() / 60.0;
        existing.setHours(hours > 0 ? Math.round(hours * 100) / 100.0 : existing.getHours());
        scheduleMapper.update(existing);
    }

    @Override
    public void cancelSchedule(Integer scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new BusinessException("排课记录不存在");
        schedule.setStatus(BizConstants.SCHED_CANCELED);
        scheduleMapper.update(schedule);
    }

    @Override
    public CourseSchedule getById(Integer scheduleId) {
        return scheduleMapper.selectById(scheduleId);
    }

    @Override
    public List<CourseSchedule> listByTeacher(Integer teacherId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectByTeacherAndDate(teacherId, startDate, endDate);
    }

    // ==================== 调课申请审批 ====================

    @Override
    @Transactional
    public Integer applyReschedule(Integer scheduleId, LocalDate newDate,
                                   LocalTime newStartTime, LocalTime newEndTime, String reason) {
        CourseSchedule source = scheduleMapper.selectById(scheduleId);
        if (source == null) throw new BusinessException("原排课不存在");
        if (BizConstants.SCHED_CANCELED.equals(source.getStatus()))
            throw new BusinessException("该节课已取消，不能调课");
        if (newDate == null || newStartTime == null || newEndTime == null)
            throw new BusinessException("请选择新日期与时间段");
        if (!newEndTime.isAfter(newStartTime)) throw new BusinessException("结束时间必须晚于开始时间");
        // 同一节课存在待审核申请时禁止重复提交
        for (RescheduleRequest old : rescheduleMapper.selectByScheduleId(scheduleId)) {
            if (BizConstants.RR_PENDING.equals(old.getStatus()))
                throw new BusinessException("该节课已有待审核的调课申请，请勿重复提交");
        }
        // 申请阶段即做三重冲突检测，与审批通过时的校验口径保持一致
        if (source.getTeacherId() != null
                && scheduleMapper.countTeacherConflict(source.getTeacherId(), newDate,
                        newStartTime, newEndTime, scheduleId) > 0)
            throw new BusinessException("目标时段该教师已有其他排课，时间冲突");
        if (source.getClassroomId() != null
                && scheduleMapper.countClassroomConflict(source.getClassroomId(), newDate,
                        newStartTime, newEndTime, scheduleId) > 0)
            throw new BusinessException("目标时段该教室已被占用，时间冲突");
        if (source.getClassId() != null
                && scheduleMapper.countClassConflict(source.getClassId(), newDate,
                        newStartTime, newEndTime, scheduleId) > 0)
            throw new BusinessException("目标时段该班级已有其他排课，时间冲突");

        RescheduleRequest req = new RescheduleRequest();
        req.setScheduleId(scheduleId);
        req.setRequestType(BizConstants.RR_MOVE);
        req.setNewDate(newDate);
        req.setNewStartTime(newStartTime);
        req.setNewEndTime(newEndTime);
        req.setReason(reason);
        req.setStatus(BizConstants.RR_PENDING);
        req.setApplyTime(LocalDateTime.now());
        rescheduleMapper.insert(req);
        return req.getRequestId();
    }

    @Override
    public List<RescheduleRequestVO> listRescheduleRequests(String status) {
        List<RescheduleRequestVO> result = new ArrayList<>();
        for (RescheduleRequest rr : rescheduleMapper.selectAll()) {
            if (status != null && !status.isEmpty() && !status.equals(rr.getStatus())) continue;
            RescheduleRequestVO vo = new RescheduleRequestVO();
            vo.setRequestId(rr.getRequestId());
            vo.setScheduleId(rr.getScheduleId());
            vo.setRequestType(rr.getRequestType() != null ? rr.getRequestType() : BizConstants.RR_MOVE);
            vo.setTargetScheduleId(rr.getTargetScheduleId());
            vo.setNewDate(rr.getNewDate());
            vo.setNewStartTime(rr.getNewStartTime());
            vo.setNewEndTime(rr.getNewEndTime());
            vo.setReason(rr.getReason());
            vo.setStatus(rr.getStatus());
            vo.setApplyTime(rr.getApplyTime() != null ? rr.getApplyTime().toString() : null);
            vo.setHandleRemark(rr.getHandleRemark());
            CourseSchedule cs = scheduleMapper.selectById(rr.getScheduleId());
            if (cs != null) {
                vo.setOriginalDate(cs.getCourseDate());
                vo.setOriginalStartTime(cs.getStartTime());
                vo.setOriginalEndTime(cs.getEndTime());
                CourseClass cc = cs.getClassId() != null ? classMapper.selectById(cs.getClassId()) : null;
                vo.setClassName(cc != null ? cc.getClassName() : null);
                Teacher t = cs.getTeacherId() != null ? teacherMapper.selectById(cs.getTeacherId()) : null;
                vo.setTeacherName(t != null ? t.getName() : null);
                Classroom room = cs.getClassroomId() != null ? classroomMapper.selectById(cs.getClassroomId()) : null;
                vo.setClassroomName(room != null ? room.getClassroomName() : null);
            }
            if (rr.getTargetScheduleId() != null) {
                CourseSchedule ts = scheduleMapper.selectById(rr.getTargetScheduleId());
                if (ts != null) {
                    vo.setTargetDate(ts.getCourseDate());
                    vo.setTargetStartTime(ts.getStartTime());
                    vo.setTargetEndTime(ts.getEndTime());
                    CourseClass tcc = ts.getClassId() != null ? classMapper.selectById(ts.getClassId()) : null;
                    if (tcc != null) vo.setTargetClassName(tcc.getClassName());
                    Teacher ttt = ts.getTeacherId() != null ? teacherMapper.selectById(ts.getTeacherId()) : null;
                    if (ttt != null) vo.setTargetTeacherName(ttt.getName());
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void approveReschedule(Integer requestId, String remark) {
        RescheduleRequest rr = rescheduleMapper.selectById(requestId);
        if (rr == null) throw new BusinessException("调课申请不存在");
        if (!BizConstants.RR_PENDING.equals(rr.getStatus())) throw new BusinessException("该申请已处理，不能重复审批");
        CourseSchedule schedule = scheduleMapper.selectById(rr.getScheduleId());
        if (schedule == null) throw new BusinessException("原排课记录不存在");
        if (rr.getNewDate() == null || rr.getNewStartTime() == null || rr.getNewEndTime() == null)
            throw new BusinessException("申请缺少目标时间，无法通过");
        if (!rr.getNewEndTime().isAfter(rr.getNewStartTime())) throw new BusinessException("目标结束时间必须晚于开始时间");

        boolean swap = BizConstants.RR_SWAP.equals(rr.getRequestType());
        CourseSchedule target = null;
        if (swap) {
            // 交换调课：目标排课必须存在且确实占据目标时段
            if (rr.getTargetScheduleId() == null) throw new BusinessException("交换申请缺少交换目标");
            target = scheduleMapper.selectById(rr.getTargetScheduleId());
            if (target == null || BizConstants.SCHED_CANCELED.equals(target.getStatus()))
                throw new BusinessException("要交换的课程不存在或已取消");
            boolean overlap = !target.getEndTime().isAfter(rr.getNewStartTime()) ? false
                    : target.getStartTime().isBefore(rr.getNewEndTime())
                        && target.getCourseDate() != null && target.getCourseDate().equals(rr.getNewDate());
            if (!overlap) throw new BusinessException("交换目标已不在该时段，无法交换，请教师重新申请");
        } else {
            // 普通调课：三重冲突检测 教师 / 教室 / 班级
            if (schedule.getTeacherId() != null
                    && scheduleMapper.countTeacherConflict(schedule.getTeacherId(), rr.getNewDate(),
                            rr.getNewStartTime(), rr.getNewEndTime(), schedule.getScheduleId()) > 0)
                throw new BusinessException("目标时段该教师已有其他排课，时间冲突");
            if (schedule.getClassroomId() != null
                    && scheduleMapper.countClassroomConflict(schedule.getClassroomId(), rr.getNewDate(),
                            rr.getNewStartTime(), rr.getNewEndTime(), schedule.getScheduleId()) > 0)
                throw new BusinessException("目标时段该教室已被占用，时间冲突");
            if (schedule.getClassId() != null
                    && scheduleMapper.countClassConflict(schedule.getClassId(), rr.getNewDate(),
                            rr.getNewStartTime(), rr.getNewEndTime(), schedule.getScheduleId()) > 0)
                throw new BusinessException("目标时段该班级已有其他排课，时间冲突");
        }

        // 回写排课（交换时两节课互换时间段）
        LocalDate srcDate = schedule.getCourseDate();
        LocalTime srcStart = schedule.getStartTime();
        LocalTime srcEnd = schedule.getEndTime();
        double hours = Duration.between(rr.getNewStartTime(), rr.getNewEndTime()).toMinutes() / 60.0;
        schedule.setCourseDate(rr.getNewDate());
        schedule.setStartTime(rr.getNewStartTime());
        schedule.setEndTime(rr.getNewEndTime());
        schedule.setHours(Math.round(hours * 100) / 100.0);
        if (BizConstants.SCHED_CANCELED.equals(schedule.getStatus())) schedule.setStatus(BizConstants.SCHED_PENDING);
        scheduleMapper.update(schedule);

        if (swap) {
            double targetHours = srcStart != null && srcEnd != null
                    ? Duration.between(srcStart, srcEnd).toMinutes() / 60.0 : hours;
            target.setCourseDate(srcDate);
            target.setStartTime(srcStart);
            target.setEndTime(srcEnd);
            target.setHours(Math.round(targetHours * 100) / 100.0);
            if (BizConstants.SCHED_CANCELED.equals(target.getStatus())) target.setStatus(BizConstants.SCHED_PENDING);
            scheduleMapper.update(target);
        }

        rr.setStatus(BizConstants.RR_PASSED);
        rr.setHandleTime(LocalDateTime.now());
        rr.setHandleRemark(remark);
        rr.setHandlerId(com.art.util.SecurityUtil.getCurrentUserId());
        rescheduleMapper.update(rr);

        // 通知申请教师
        Teacher t = schedule.getTeacherId() != null ? teacherMapper.selectById(schedule.getTeacherId()) : null;
        if (t != null && t.getUserId() != null) {
            messageService.send(t.getUserId(), "调课", swap ? "调课交换申请已通过" : "调课申请已通过",
                    (swap ? "您的课程已与其他课程交换，现调整为 " : "您申请的调课已通过，调整为 ")
                            + rr.getNewDate() + " " + rr.getNewStartTime() + "-" + rr.getNewEndTime()
                            + (remark != null && !remark.isBlank() ? "（审批意见：" + remark + "）" : ""),
                    "reschedule", requestId);
        }
        // 交换时通知被交换课程的教师
        if (swap && target.getTeacherId() != null
                && (schedule.getTeacherId() == null || !target.getTeacherId().equals(schedule.getTeacherId()))) {
            Teacher tt = teacherMapper.selectById(target.getTeacherId());
            CourseClass tcc = target.getClassId() != null ? classMapper.selectById(target.getClassId()) : null;
            if (tt != null && tt.getUserId() != null) {
                messageService.send(tt.getUserId(), "调课", "您的课程因调课交换调整了时间",
                        "【" + (tcc != null ? tcc.getClassName() : "课程") + "】经教务审批与其他课程交换，现调整为 "
                                + srcDate + " " + srcStart + "-" + srcEnd,
                        "reschedule", requestId);
            }
        }
    }

    @Override
    public void rejectReschedule(Integer requestId, String remark) {
        RescheduleRequest rr = rescheduleMapper.selectById(requestId);
        if (rr == null) throw new BusinessException("调课申请不存在");
        if (!BizConstants.RR_PENDING.equals(rr.getStatus())) throw new BusinessException("该申请已处理，不能重复审批");
        rr.setStatus(BizConstants.RR_REJECTED);
        rr.setHandleTime(LocalDateTime.now());
        rr.setHandleRemark(remark);
        rr.setHandlerId(com.art.util.SecurityUtil.getCurrentUserId());
        rescheduleMapper.update(rr);

        CourseSchedule schedule = scheduleMapper.selectById(rr.getScheduleId());
        if (schedule != null && schedule.getTeacherId() != null) {
            Teacher t = teacherMapper.selectById(schedule.getTeacherId());
            if (t != null && t.getUserId() != null) {
                messageService.send(t.getUserId(), "调课", "调课申请未通过",
                        "您申请的调课未通过。" + (remark != null && !remark.isBlank() ? "原因：" + remark : "请与教务沟通"),
                        "reschedule", requestId);
            }
        }
    }

    /** 解析需要排课的日期：周期模式按星期展开，否则单次 */
    private List<LocalDate> resolveDates(ScheduleDTO dto) {
        List<LocalDate> result = new ArrayList<>();
        if (dto.getRangeStart() != null && dto.getRangeEnd() != null
                && dto.getWeekdays() != null && !dto.getWeekdays().isEmpty()) {
            if (dto.getRangeEnd().isBefore(dto.getRangeStart()))
                throw new BusinessException("周期结束日期不能早于开始日期");
            for (LocalDate d = dto.getRangeStart(); !d.isAfter(dto.getRangeEnd()); d = d.plusDays(1)) {
                int dow = d.getDayOfWeek().getValue(); // 1=周一 7=周日
                if (dto.getWeekdays().contains(dow)) result.add(d);
            }
            if (result.isEmpty()) throw new BusinessException("所选周期内没有匹配的上课星期");
        } else {
            result.add(dto.getCourseDate());
        }
        return result;
    }

    /** 教师/教室/班级时间冲突检测 */
    private void checkConflict(ScheduleDTO dto, LocalDate date, Integer excludeId) {
        String teacherName = teacherMapper.selectById(dto.getTeacherId()) != null
                ? teacherMapper.selectById(dto.getTeacherId()).getName() : "该教师";
        if (scheduleMapper.countTeacherConflict(dto.getTeacherId(), date, dto.getStartTime(), dto.getEndTime(), excludeId) > 0) {
            throw new BusinessException(date + " " + dto.getStartTime() + "-" + dto.getEndTime()
                    + " 教师【" + teacherName + "】已有其他班级排课，时间冲突");
        }
        String roomName = classroomMapper.selectById(dto.getClassroomId()) != null
                ? classroomMapper.selectById(dto.getClassroomId()).getClassroomName() : "该教室";
        // 教室容量必须容纳班级人数
        CourseClass cc = dto.getClassId() == null ? null : classMapper.selectById(dto.getClassId());
        if (cc != null) {
            int enrolled = 0;
            for (Enrollment e : enrollmentMapper.selectByClassId(dto.getClassId())) {
                if (e.getStatus() == null || BizConstants.ENROLL_ACTIVE.equals(e.getStatus())) enrolled++;
            }
            // 以班级最大人数为准（招满也能容纳），无最大人数时退回当前在读人数
            int need = cc.getMaxStudents() != null && cc.getMaxStudents() > 0 ? cc.getMaxStudents() : enrolled;
            var room = classroomMapper.selectById(dto.getClassroomId());
            if (room != null && need > 0 && room.getCapacity() != null && room.getCapacity() < need) {
                throw new BusinessException("教室【" + roomName + "】仅可容纳" + room.getCapacity()
                        + "人，班级约" + need + "人，请选择更大的教室");
            }
            // 同一班级不得同时段重复排课
            if (scheduleMapper.countClassConflict(dto.getClassId(), date, dto.getStartTime(), dto.getEndTime(), excludeId) > 0) {
                throw new BusinessException(date + " " + dto.getStartTime() + "-" + dto.getEndTime()
                        + " 班级【" + cc.getClassName() + "】在该时段已有排课，时间冲突");
            }
        }
        if (scheduleMapper.countClassroomConflict(dto.getClassroomId(), date, dto.getStartTime(), dto.getEndTime(), excludeId) > 0) {
            throw new BusinessException(date + " " + dto.getStartTime() + "-" + dto.getEndTime()
                    + " 教室【" + roomName + "】已被其他班级占用，时间冲突");
        }
    }
}
