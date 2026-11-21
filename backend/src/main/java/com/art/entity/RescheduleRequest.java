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
public class RescheduleRequest implements Serializable {

    private Integer requestId;
    private Integer scheduleId;
    private String requestType;    // 调课 / 交换
    private Integer targetScheduleId; // 交换目标排课ID
    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String reason;
    private String status;       // 待审核 / 已通过 / 已拒绝
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private String handleRemark;
    private Integer handlerId;
}
