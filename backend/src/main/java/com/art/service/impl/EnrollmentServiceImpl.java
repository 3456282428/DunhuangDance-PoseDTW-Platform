package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.PageResult;
import com.art.dto.EnrollmentDTO;
import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.entity.Student;
import com.art.entity.SysUser;
import com.art.entity.Attendance;
import com.art.entity.CourseSchedule;
import com.art.entity.Payment;
import com.art.mapper.AttendanceMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseScheduleMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.PaymentMapper;
import com.art.mapper.StudentMapper;
import com.art.mapper.UserMapper;
import com.art.common.BusinessException;
import com.art.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseClassMapper courseClassMapper;
    private final UserMapper userMapper;
    private final AttendanceMapper attendanceMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final PaymentMapper paymentMapper;

    /** studentId -> 登录账号（学号） */
    private Map<Integer, String> studentUsernameMap() {
        Map<Integer, String> uname = new HashMap<>();
        for (SysUser u : userMapper.selectAll()) uname.put(u.getUserId(), u.getUsername());
        Map<Integer, String> r = new HashMap<>();
        for (Student s : studentMapper.selectAll()) r.put(s.getStudentId(), s.getUserId() == null ? null : uname.get(s.getUserId()));
        return r;
    }

    @Override
    public PageResult<Enrollment> pageEnrollments(Integer pageNum, Integer pageSize, String studentName, Integer classId, String status) {
        Map<Integer, String> studentNames = new HashMap<>();
        for (Student s : studentMapper.selectAll()) studentNames.put(s.getStudentId(), s.getName());
        Map<Integer, String> studentUsers = studentUsernameMap();
        Map<Integer, CourseClass> classes = new HashMap<>();
        for (CourseClass c : courseClassMapper.selectByCondition(null, null, null)) classes.put(c.getClassId(), c);

        String kw = studentName == null ? null : studentName.trim();
        List<Enrollment> filtered = enrollmentMapper.selectAll().stream()
                .filter(e -> classId == null || classId.equals(e.getClassId()))
                .filter(e -> status == null || status.isEmpty() || status.equals(e.getStatus()))
                .filter(e -> {
                    if (kw == null || kw.isEmpty()) return true;
                    String name = studentNames.get(e.getStudentId());
                    String uname = studentUsers.get(e.getStudentId());
                    return (name != null && name.contains(kw)) || (uname != null && uname.contains(kw));
                })
                .collect(Collectors.toList());

        for (Enrollment e : filtered) {
            e.setStudentName(studentNames.get(e.getStudentId()));
            e.setStudentUsername(studentUsers.get(e.getStudentId()));
            CourseClass c = classes.get(e.getClassId());
            if (c != null) e.setClassName(c.getClassName());
        }

        int total = filtered.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Enrollment> rows = from >= total ? new ArrayList<>() : filtered.subList(from, to);
        return PageResult.of((long) total, rows, pageNum, pageSize);
    }

    @Override
    public void createEnrollment(EnrollmentDTO dto) {
        if (dto.getStudentId() == null) throw new BusinessException("请选择学员");
        if (dto.getClassId() == null) throw new BusinessException("请选择班级");
        if (studentMapper.selectById(dto.getStudentId()) == null) throw new BusinessException("学员不存在");
        CourseClass cc = courseClassMapper.selectById(dto.getClassId());
        if (cc == null) throw new BusinessException("班级不存在");
        // 同一学员同一班级在读报名不可重复；退学/结课后重新报名则复用原记录
        Enrollment exist = enrollmentMapper.selectByStudentAndClass(dto.getStudentId(), dto.getClassId());
        if (exist != null && BizConstants.ENROLL_ACTIVE.equals(exist.getStatus()))
            throw new BusinessException("该学员已在此班级报名，请勿重复");

        if (exist != null) {
            // 重新报名：恢复在读，重置考勤计数，更新学费
            if (dto.getTuitionAmount() != null) exist.setTuitionAmount(dto.getTuitionAmount());
            else if (exist.getTuitionAmount() == null && cc.getTuition() != null) exist.setTuitionAmount(cc.getTuition());
            exist.setAttendedCount(0);
            exist.setAbsentCount(0);
            exist.setStatus(BizConstants.ENROLL_ACTIVE);
            enrollmentMapper.update(exist);
            return;
        }

        Enrollment enrollment = new Enrollment();
        BeanUtils.copyProperties(dto, enrollment);
        if (enrollment.getTuitionAmount() == null && cc.getTuition() != null)
            enrollment.setTuitionAmount(cc.getTuition());
        enrollment.setAttendedCount(0);
        enrollment.setAbsentCount(0);
        enrollment.setStatus(BizConstants.ENROLL_ACTIVE);
        enrollmentMapper.insert(enrollment);
    }

    @Override
    public void updateStatus(Integer enrollmentId, String status) {
        enrollmentMapper.updateStatus(enrollmentId, status);
    }

    @Override
    public void delete(Integer enrollmentId) {
        enrollmentMapper.deleteById(enrollmentId);
    }

    @Override
    @Transactional
    public int recalcAllStats() {
        int fixed = 0;
        for (Enrollment e : enrollmentMapper.selectAll()) {
            // 考勤：按该班全部排课明细重算
            int attended = 0, absent = 0;
            List<CourseSchedule> schedules = scheduleMapper.selectByConditions(null, null, null, null, e.getClassId());
            for (CourseSchedule cs : schedules) {
                for (Attendance a : attendanceMapper.selectByScheduleId(cs.getScheduleId())) {
                    if (!e.getStudentId().equals(a.getStudentId())) continue;
                    if (BizConstants.ATT_PRESENT.equals(a.getStatus()) || BizConstants.ATT_LATE.equals(a.getStatus())) attended++;
                    else if (BizConstants.ATT_ABSENT.equals(a.getStatus()) || BizConstants.ATT_LEAVE.equals(a.getStatus())) absent++; // 与触发器口径一致：缺勤/请假均计缺勤桶
                }
            }
            // 缴费：按流水净额重算
            BigDecimal paid = BigDecimal.ZERO;
            for (Payment p : paymentMapper.selectByStudentId(e.getStudentId())) {
                if (e.getEnrollmentId().equals(p.getEnrollmentId())
                        && (BizConstants.PAY_PAID.equals(p.getStatus()) || BizConstants.PAY_REFUNDED.equals(p.getStatus()))
                        && p.getAmount() != null) {
                    paid = paid.add(p.getAmount());
                }
            }
            paid = paid.max(BigDecimal.ZERO);
            boolean changed = !java.util.Objects.equals(attended, e.getAttendedCount())
                    || !java.util.Objects.equals(absent, e.getAbsentCount())
                    || (e.getTotalPaid() != null && paid.compareTo(e.getTotalPaid()) != 0)
                    || e.getTotalPaid() == null;
            if (changed) {
                enrollmentMapper.updateAttendCount(e.getEnrollmentId(), attended, absent);
                Enrollment up = enrollmentMapper.selectById(e.getEnrollmentId());
                up.setTotalPaid(paid);
                enrollmentMapper.update(up);
                fixed++;
            }
        }
        return fixed;
    }
}
