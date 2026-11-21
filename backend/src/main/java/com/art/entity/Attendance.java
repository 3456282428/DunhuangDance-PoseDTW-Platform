package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Attendance implements Serializable {

    private Integer attendanceId;
    private Integer scheduleId;
    private Integer studentId;
    private Integer requestId;    // 关联请假申请（student_request.request_id）
    private String status;        // 出勤 / 迟到 / 缺勤 / 请假
    private LocalDateTime recordTime;
    private String remark;

    // 关联排课/班级的展示字段（仅学员考勤明细等联表查询时填充）
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String className;
}