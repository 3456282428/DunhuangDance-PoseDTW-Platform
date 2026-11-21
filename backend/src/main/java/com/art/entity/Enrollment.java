package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class Enrollment implements Serializable {

    private Integer enrollmentId;
    private Integer studentId;
    private Integer classId;
    private LocalDate enrollDate;
    private BigDecimal tuitionAmount;
    private String status;        // 在读 / 退学 / 结课
    private Integer attendedCount;
    private Integer absentCount;
    private BigDecimal totalPaid;

    // 关联展示字段（非数据库列）
    private String studentName;
    private String studentUsername; // 学号/登录账号
    private String className;
    private String categoryName;
}