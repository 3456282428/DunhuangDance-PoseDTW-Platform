package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class AttendanceSummaryVO {

    private Integer totalSchedules;     // 应上课次数
    private Integer attended;           // 实际出勤次数
    private Integer late;               // 迟到次数
    private Integer absent;             // 缺勤次数
    private Integer leave;              // 请假次数
    private Double attendanceRate;      // 出勤率
}
