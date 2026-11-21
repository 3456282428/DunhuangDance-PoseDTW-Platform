package com.art.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class VisitorBookingDTO {

    private Integer bookingId;
    private String name;
    private String phone;
    private Integer classId;
    private Integer teacherId;
    private String className;
    private String teacherName;
    private LocalDateTime preferredTime;
    private String remark;
    private String status;       // 待确认 / 已确认 / 已取消
    private LocalDateTime createTime;
}
