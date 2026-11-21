package com.art.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
/**
 * @author liukang&&heyan
 */
public class DashboardOverviewDTO {

    private Integer todayCourseCount;   // 今日课程数
    private Integer pendingTodoCount;   // 待处理事项数
    private Double attendanceRate;      // 出勤率
    private BigDecimal todayRevenue;    // 今日收入
    private BigDecimal monthRevenue;    // 本月收入
    private Double renewalRate;         // 续课率
    private Integer totalStudents;      // 在册学员数
    private Integer totalTeachers;      // 在职教师数
    private Integer newStudentsThisMonth; // 本月新增学员
    private Double monthTeacherHours;   // 本月教师总课时
    private String topTeacherName;      // 授课课时最多的教师
    private Double topTeacherHours;     // 该教师本月课时
}
