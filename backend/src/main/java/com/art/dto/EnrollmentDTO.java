package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class EnrollmentDTO {

    private Integer enrollmentId;

    @NotNull(message = "学员不能为空")
    private Integer studentId;

    @NotNull(message = "班级不能为空")
    private Integer classId;

    private LocalDate enrollDate;

    private BigDecimal tuitionAmount;

    private String status;  // 在读 / 退学 / 结课

    private BigDecimal totalPaid;
}