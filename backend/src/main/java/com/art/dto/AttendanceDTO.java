package com.art.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考勤数据传输对象
  * @author liukang&&heyan
 */
@Data
public class AttendanceDTO {

    private Integer attendanceId;
    private Integer scheduleId;
    private Integer studentId;
    private String status;          // 出勤 / 迟到 / 缺勤 / 请假
    private LocalDateTime recordTime;
    private String remark;
}