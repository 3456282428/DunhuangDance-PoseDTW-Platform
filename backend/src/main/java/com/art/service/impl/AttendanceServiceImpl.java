package com.art.service.impl;

import com.art.common.PageResult;
import com.art.entity.Attendance;
import com.art.mapper.AttendanceMapper;
import com.art.service.AttendanceService;
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
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceMapper attendanceMapper;

    @Override
    public PageResult<Attendance> pageAttendances(Integer pageNum, Integer pageSize, Integer classId, LocalDate date, Integer studentId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Attendance> list = attendanceMapper.selectByConditions(classId, date, studentId);
        PageInfo<Attendance> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public void updateStatus(Integer attendanceId, String status) {
        attendanceMapper.updateStatus(attendanceId, status);
    }
}