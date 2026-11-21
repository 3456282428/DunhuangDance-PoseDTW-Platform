package com.art.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class PortalScheduleVO {
    private Integer scheduleId;
    private Integer classId;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
}
