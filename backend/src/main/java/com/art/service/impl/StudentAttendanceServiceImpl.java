package com.art.service.impl;

import com.art.common.PageResult;
import com.art.entity.Attendance;
import com.art.mapper.AttendanceMapper;
import com.art.service.StudentAttendanceService;
import com.art.util.SecurityUtil;
import com.art.vo.AttendanceSummaryVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final AttendanceMapper attendanceMapper;

    @Override
    public PageResult<Attendance> pageMyAttendance(Integer pageNum, Integer pageSize, LocalDate startDate, LocalDate endDate, Integer classId) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        PageHelper.startPage(pageNum, pageSize);
        List<Attendance> list = attendanceMapper.selectByStudentAndDateRange(studentId, startDate, endDate, classId);
        PageInfo<Attendance> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public AttendanceSummaryVO getMyAttendanceSummary() {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        AttendanceSummaryVO vo = new AttendanceSummaryVO();
        int attended = 0, late = 0, absent = 0, leave = 0;
        for (Attendance a : attendanceMapper.selectByConditions(null, null, studentId)) {
            switch (a.getStatus() == null ? "" : a.getStatus()) {
                case "出勤": attended++; break;
                case "迟到": late++; break;
                case "缺勤": absent++; break;
                case "请假": leave++; break;
                default: break;
            }
        }
        int total = attended + late + absent + leave;
        vo.setTotalSchedules(total);
        vo.setAttended(attended);
        vo.setLate(late);
        vo.setAbsent(absent);
        vo.setLeave(leave);
        vo.setAttendanceRate(total > 0 ? Math.round((attended + late) * 100.0 / total * 100.0) / 100.0 : 0.0);
        return vo;
    }
}