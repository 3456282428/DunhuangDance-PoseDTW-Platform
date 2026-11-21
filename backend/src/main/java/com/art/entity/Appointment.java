package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Appointment implements Serializable {

    private Integer appointmentId;
    private Integer studentId;
    private Integer teacherId;
    private Integer classId;
    private String appointmentType;  // 试听 / 补课
    private LocalDateTime appointmentTime;
    private String status;           // 待确认 / 已接受 / 已拒绝
    private String remark;
}
