package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class AttendanceStudentVO {

    private Integer studentId;
    private String studentName;
    private String status;      // 出勤 / 迟到 / 缺勤 / 请假
    private String remark;
}
