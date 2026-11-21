package com.art.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * @author liukang&&heyan
 */
public class CourseScheduleVO {

    private Integer scheduleId;
    private Integer classId;
    private String className;
    private String categoryName;
    private Integer teacherId;
    private String teacherName;
    private Integer classroomId;
    private String classroomName;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double hours;
    private String status;
    private String remark;

    // 额外统计信息（用于首页/教师端）
    private Integer enrolledCount;      // 报名人数
    private Integer attendedCount;      // 出勤人数
}
