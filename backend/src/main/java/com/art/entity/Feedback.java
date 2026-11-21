package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Feedback implements Serializable {

    private Integer feedbackId;
    private Integer studentId;
    private String content;
    private String type;         // 建议 / 投诉 / 咨询
    private LocalDateTime createTime;
    private String status;       // 待处理 / 已回复
    private String replyContent;
    private LocalDateTime replyTime;
    private String replyBy;      // 回复人（文本快照，兼容历史展示）
    private Integer replyUserId; // 回复人用户ID（权威外键 -> sys_user）
}
