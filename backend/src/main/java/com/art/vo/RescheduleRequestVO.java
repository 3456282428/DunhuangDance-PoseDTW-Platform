package com.art.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class RescheduleRequestVO {

    private Integer requestId;
    private Integer scheduleId;
    private String requestType;       // 调课 / 交换
    private Integer targetScheduleId;
    private String targetClassName;
    private String targetTeacherName;
    private java.time.LocalDate targetDate;
    private java.time.LocalTime targetStartTime;
    private java.time.LocalTime targetEndTime;
    private String className;
    private LocalDate originalDate;
    private LocalTime originalStartTime;
    private LocalTime originalEndTime;
    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String teacherName;
    private String classroomName;
    private String reason;
    private String status;      // 待审核 / 已通过 / 已拒绝
    private String applyTime;
    private String handleRemark;
}
