package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class RescheduleRequestDTO {

    @NotNull(message = "新的上课日期不能为空")
    private LocalDate newDate;

    @NotNull(message = "新的开始时间不能为空")
    private LocalTime newStartTime;

    @NotNull(message = "新的结束时间不能为空")
    private LocalTime newEndTime;

    private String reason; // 调课原因
    private String requestType;      // 调课 / 交换
    private Integer targetScheduleId; // 交换目标排课ID
}