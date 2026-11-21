package com.art.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class FeedbackVO {

    private Integer feedbackId;
    private Integer studentId;
    private String studentName;
    private String content;
    private String type;        // 建议 / 投诉 / 咨询
    private LocalDateTime createTime;
    private String status;      // 待处理 / 已回复
    private String replyContent;
    private LocalDateTime replyTime;
    private String replyBy;     // 回复人（教师或管理员）
}
