package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentProgressVO {

    private Integer studentId;
    private String studentName;
    private Integer classId;
    private String className;
    private Integer totalHours;
    private Integer attendedHours;
    private Integer absentCount;
    private Double attendanceRate;
    private Integer homeworkSubmitted;
    private Integer homeworkTotal;
    private Double homeworkRate;
    private Integer examPassed;
    private Integer examTotal;
    private String latestComment;   // 最新教师评语
}
