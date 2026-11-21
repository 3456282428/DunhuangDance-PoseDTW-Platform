package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class ClassDTO {

    private Integer classId;

    @NotNull(message = "请选择所属课程")
    private Integer courseId;

    private String className;   // 不传则默认用课程名

    private Integer categoryId; // 由课程带出，兼容旧字段

    private Integer teacherId;

    private Integer classroomId;

    private String semester;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer totalHours;     // 不传则取课程配置

    private Integer lessonDuration; // 不传则取课程配置

    private BigDecimal tuition;     // 不传则取课程价格

    private Integer maxStudents;

    private Integer status; // 1-正常 0-停用

    private String ageGroup;

    private String level;

    private String description;
}
