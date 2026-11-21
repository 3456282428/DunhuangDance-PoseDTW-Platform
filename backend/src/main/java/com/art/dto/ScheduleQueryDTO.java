package com.art.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class ScheduleQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer teacherId;
    private Integer classroomId;
    private Integer classId;
}