package com.art.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class AppointmentVO {

    private Integer appointmentId;
    private Integer studentId;
    private String studentName;
    private Integer teacherId;
    private String teacherName;
    private Integer classId;
    private String className;
    private String appointmentType; // 试听 / 补课
    private LocalDateTime appointmentTime;
    private String status;          // 待确认 / 已接受 / 已拒绝
    private String remark;
}
