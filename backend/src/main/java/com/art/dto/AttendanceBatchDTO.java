package com.art.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
/**
 * @author liukang&&heyan
 */
public class AttendanceBatchDTO {

    @NotEmpty(message = "考勤列表不能为空")
    private List<AttendanceItem> items;

    @Data
    public static class AttendanceItem {
        private Integer studentId;
        private String status;  // 出勤 / 迟到 / 缺勤 / 请假
        private String remark;
    }
}