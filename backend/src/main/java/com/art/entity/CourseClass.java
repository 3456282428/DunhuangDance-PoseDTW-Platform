package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class CourseClass implements Serializable {

    private Integer classId;
    private Integer courseId;      // 所属课程产品
    private String className;
    private Integer categoryId;
    private Integer teacherId;
    private Integer classroomId;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalHours;
    private Integer lessonDuration; // 单次时长（分钟）
    private BigDecimal tuition;
    private Integer maxStudents;
    private Integer status;       // 1-正常 0-停用
    private String ageGroup;      // 年龄段
    private String level;         // 难度
    private String description;   // 课程简介

    // ===== 关联展示字段（非持久化） =====
    private String courseName;
    private String category;
    private String teacherName;
    private String classroomName;
    private Integer currentStudents;
}