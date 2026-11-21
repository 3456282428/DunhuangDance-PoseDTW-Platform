package com.art.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentCourseDTO {

    private Integer enrollmentId;
    private Integer classId;
    private String className;
    private String categoryName;
    private String teacherName;
    private String semester;
    private BigDecimal tuition;
    private Integer attendedCount;
    private Integer totalHours;
    private String status;
}
