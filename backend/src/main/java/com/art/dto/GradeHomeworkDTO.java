package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
/**
 * @author liukang&&heyan
 */
public class GradeHomeworkDTO {

    @NotNull(message = "分数不能为空")
    private BigDecimal score;

    private String teacherComment;
}