package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class FeedbackDTO {

    private Integer feedbackId;

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private String type;   // 建议 / 投诉 / 咨询
}