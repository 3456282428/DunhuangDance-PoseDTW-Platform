package com.art.service.impl;

import com.art.common.PageResult;
import com.art.dto.AttendanceBatchDTO;
import com.art.entity.Attendance;
import com.art.entity.CourseSchedule;
import com.art.entity.Enrollment;
import com.art.mapper.AttendanceMapper;
import com.art.mapper.CourseScheduleMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.StudentMapper;
import com.art.service.TeacherAttendanceService;
import com.art.util.SecurityUtil;
import com.art.vo.AttendanceHistoryVO;
import com.art.vo.AttendanceStudentVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherAttendanceServiceImpl implements TeacherAttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;

    @Override
    public List<AttendanceStudentVO> getAttendanceList(Integer scheduleId) {
        List<AttendanceStudentVO> result = new ArrayList<>();
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) return result;
        Integer classId = schedule.getClassId();
        List<Attendance> existing = attendanceMapper.selectByScheduleId(scheduleId);
        for (Enrollment en : enrollmentMapper.selectByClassId(classId)) {
            Integer sid = en.getStudentId();
            AttendanceStudentVO vo = new AttendanceStudentVO();
            vo.setStudentId(sid);
            vo.setStudentName(studentMapper.selectById(sid) != null ? studentMapper.selectById(sid).getName() : null);
            Attendance matched = null;
            for (Attendance a : existing) {
                if (sid.equals(a.getStudentId())) { matched = a; break; }
            }
            vo.setStatus(matched != null ? matched.getStatus() : "未点名");
            vo.setRemark(matched != null ? matched.getRemark() : null);
            result.add(vo);
        }
        return result;
    }

    @Override
    public void batchUpdateAttendance(Integer scheduleId, AttendanceBatchDTO batchDTO) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) return;
        Integer classId = schedule.getClassId();
        // 仅允许给本班报名学员打考勤，杜绝孤儿考勤
        java.util.Set<Integer> enrolledIds = new java.util.HashSet<>();
        if (classId != null) {
            for (Enrollment e : enrollmentMapper.selectByClassId(classId)) enrolledIds.add(e.getStudentId());
        }
        List<Attendance> existing = attendanceMapper.selectByScheduleId(scheduleId);
        java.util.Set<Integer> touched = new java.util.HashSet<>();
        for (AttendanceBatchDTO.AttendanceItem item : batchDTO.getItems()) {
            if (classId != null && !enrolledIds.contains(item.getStudentId()))
                throw new com.art.common.BusinessException("学员未报名该班级，不能登记考勤");
            Attendance target = null;
            for (Attendance a : existing) {
                if (item.getStudentId().equals(a.getStudentId())) { target = a; break; }
            }
            if (target != null) {
                attendanceMapper.updateStatusAndRemark(target.getAttendanceId(), item.getStatus(), item.getRemark());
            } else {
                Attendance a = new Attendance();
                a.setScheduleId(scheduleId);
                a.setStudentId(item.getStudentId());
                a.setStatus(item.getStatus());
                a.setRecordTime(LocalDateTime.now());
                a.setRemark(item.getRemark());
                attendanceMapper.insert(a);
            }
            touched.add(item.getStudentId());
        }
        // 自动消课：重算每位受影响学员在本班的累计出勤/缺勤次数，回写报名记录
        if (classId != null) {
            List<CourseSchedule> classSchedules = scheduleMapper.selectByConditions(null, null, null, null, classId);
            for (Integer sid : touched) {
                int attended = 0, absent = 0;
                for (CourseSchedule cs : classSchedules) {
                    for (Attendance a : attendanceMapper.selectByScheduleId(cs.getScheduleId())) {
                        if (!sid.equals(a.getStudentId())) continue;
                        String st = a.getStatus();
                        if ("出勤".equals(st) || "迟到".equals(st)) attended++;
                        else if ("缺勤".equals(st)) absent++;
                    }
                }
                Enrollment en = enrollmentMapper.selectByStudentAndClass(sid, classId);
                if (en != null) {
                    enrollmentMapper.updateAttendCount(en.getEnrollmentId(), attended, absent);
                }
            }
        }
    }

    @Override
    public PageResult<AttendanceHistoryVO> getHistory(Integer pageNum, Integer pageSize, Integer classId,
                                                      String date, String status, String keyword) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        LocalDate localDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        PageHelper.startPage(pageNum, pageSize);
        List<AttendanceHistoryVO> list = attendanceMapper.selectHistory(teacherId, classId, localDate, status, kw);
        PageInfo<AttendanceHistoryVO> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }
}
