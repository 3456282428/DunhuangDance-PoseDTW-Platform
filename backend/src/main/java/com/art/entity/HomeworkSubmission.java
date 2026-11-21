package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class HomeworkSubmission implements Serializable {

    private Integer submissionId;
    private Integer homeworkId;
    private Integer studentId;
    private String content;         // 学生提交的答案
    private String attachmentUrl;   // 附件地址，多个以逗号分隔
    private String attachmentName;  // 附件原始文件名，多个以逗号分隔
    private LocalDateTime submitTime;
    private BigDecimal score;
    private String teacherComment;
    private String status;          // 未提交 / 已提交 / 已批改
    private BigDecimal aiScore;     // AI智能评分（舞蹈动作评分）
    private String aiReport;        // AI评分报告（JSON字符串）
    private String aiStatus;        // AI评分状态：评分中 / 已完成 / 失败
    private LocalDateTime aiScoredAt; // AI评分时间
}
