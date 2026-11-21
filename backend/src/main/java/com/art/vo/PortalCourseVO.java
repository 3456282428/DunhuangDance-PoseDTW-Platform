package com.art.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
/**
 * @author liukang&&heyan
 */
public class PortalCourseVO {

    private Integer classId;
    private String className;
    private Integer categoryId;
    private String categoryName;
    private String teacherName;
    private String classroomName;
    private String semester;
    private BigDecimal tuition;
    private Integer totalHours;
    private Integer lessonDuration; // 单次时长（分钟）
    private String ageGroup;      // 年龄段
    private String level;         // 难度
    private String description;   // 课程简介
    private Integer currentStudents;  // 当前报名人数
    private Integer maxStudents;
}
