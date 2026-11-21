package com.art.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
/**
 * @author liukang&&heyan
 */
public class ScheduleDetailVO {

    private Integer scheduleId;
    private Integer classId;
    private String className;
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

    // 该次课的学员名单
    private List<AttendanceStudentVO> studentList;
}
