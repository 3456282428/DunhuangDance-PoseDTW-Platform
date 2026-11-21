package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class VisitorBooking implements Serializable {

    private Integer bookingId;
    private String name;
    private String phone;
    private Integer classId;
    private Integer teacherId;
    private LocalDateTime preferredTime;
    private String remark;
    private String status;       // 待确认 / 已确认 / 已取消
    private LocalDateTime createTime;
}
