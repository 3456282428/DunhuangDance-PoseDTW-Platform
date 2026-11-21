package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class SubmitHomeworkDTO {

    @NotBlank(message = "作业内容不能为空")
    private String content;

    private String attachmentUrl;    // 附件地址，多个以逗号分隔
    private String attachmentName;   // 附件原始文件名，多个以逗号分隔
}