package com.art.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 教师端考勤历史记录视图（关联学员、班级、排课）
  * @author liukang&&heyan
 */
@Data
public class AttendanceHistoryVO {
    private Integer attendanceId;
    private Integer scheduleId;
    private Integer classId;
    private String className;
    private Integer studentId;
    private String studentName;
    private String account;
    private String status;
    private String remark;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime recordTime;
}
