package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
/**
 * @author liukang&&heyan
 */
public class ScheduleDTO {

    private Integer scheduleId;

    @NotNull(message = "班级不能为空")
    private Integer classId;

    @NotNull(message = "教师不能为空")
    private Integer teacherId;

    @NotNull(message = "教室不能为空")
    private Integer classroomId;

    @NotNull(message = "上课日期不能为空")
    private LocalDate courseDate;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    private Double hours;   // 课时数

    private String status;  // 待上课 / 已完成 / 已取消

    private String remark;

    // ===== 周期批量排课（可选） =====
    /** 周期开始日期（含） */
    private LocalDate rangeStart;
    /** 周期结束日期（含） */
    private LocalDate rangeEnd;
    /** 每周上课日（1=周一 … 7=周日），为空则仅按 courseDate 单次排课 */
    private List<Integer> weekdays;
}
