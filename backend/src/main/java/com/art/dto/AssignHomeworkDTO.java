package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class AssignHomeworkDTO {

    @NotNull(message = "班级不能为空")
    private Integer classId;

    private Integer scheduleId;  // 可关联某次课程

    @NotBlank(message = "作业标题不能为空")
    private String title;

    @NotBlank(message = "作业内容不能为空")
    private String content;

    private LocalDateTime deadline; // 截止时间

    private String refVideoUrl;     // 教师参考视频地址（AI评分用）
    private String refVideoName;    // 教师参考视频原始文件名
}