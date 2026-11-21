package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentRequestDTO {

    private Integer requestId;
    private Integer studentId;
    private String studentName;
    private Integer teacherId;
    private String teacherName;
    private Integer classId;
    private String className;

    @NotBlank(message = "申请类型不能为空")
    private String requestType;   // 请假 / 缓考 / 补考

    private LocalDate requestDate;

    @NotBlank(message = "申请理由不能为空")
    private String reason;

    private String status;        // 待处理 / 已批准 / 已拒绝
    private String replyRemark;
    private LocalDateTime createTime;
}
