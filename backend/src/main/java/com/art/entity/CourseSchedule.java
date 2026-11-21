package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class CourseSchedule implements Serializable {

    private Integer scheduleId;
    private Integer classId;
    private Integer teacherId;
    private Integer classroomId;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double hours;         // 课时数
    private String status;        // 待上课 / 已完成 / 已取消
    private String remark;

    // 非持久化的关联展示字段
    private transient String className;
    private transient String classroomName;
    private transient String teacherName;
}