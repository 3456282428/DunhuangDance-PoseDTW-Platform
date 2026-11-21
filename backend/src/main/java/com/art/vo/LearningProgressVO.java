package com.art.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
/**
 * @author liukang&&heyan
 */
public class LearningProgressVO {

    private Integer totalCourses;           // 在学课程数
    private Integer totalHours;             // 总课时
    private Integer attendedHours;          // 已上课时
    private Double attendanceRate;          // 出勤率
    private Integer homeworkTotal;          // 作业总数
    private Integer homeworkSubmitted;      // 已提交数
    private Integer examTotal;              // 考级/比赛次数
    private Integer examPassed;             // 通过/获奖次数
    private List<Map<String, Object>> recentActivities; // 最近动态
}
