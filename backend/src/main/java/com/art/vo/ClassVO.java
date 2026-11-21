package com.art.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class ClassVO {

    private Integer classId;
    private String className;
    private String categoryName;
    private String teacherName;
    private String classroomName;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalHours;
    private BigDecimal tuition;
    private Integer maxStudents;
    private Integer currentStudents;
    private Integer status;
}
