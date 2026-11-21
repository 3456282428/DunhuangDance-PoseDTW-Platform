package com.art.service;

import com.art.common.PageResult;
import com.art.dto.AttendanceBatchDTO;
import com.art.vo.AttendanceHistoryVO;
import com.art.vo.AttendanceStudentVO;

import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherAttendanceService {
    List<AttendanceStudentVO> getAttendanceList(Integer scheduleId);
    void batchUpdateAttendance(Integer scheduleId, AttendanceBatchDTO batchDTO);
    PageResult<AttendanceHistoryVO> getHistory(Integer pageNum, Integer pageSize, Integer classId,
                                               String date, String status, String keyword);
}