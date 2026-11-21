package com.art.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交视图（教师批改 / 学员提交）
  * @author liukang&&heyan
 */
@Data
public class HomeworkSubmissionVO {

    private Integer submissionId;
    private Integer homeworkId;
    private Integer studentId;
    private String studentName;
    private String title;
    private String content;         // 学生提交的内容
    private String attachmentUrl;   // 附件地址，多个以逗号分隔
    private String attachmentName;  // 附件原始文件名，多个以逗号分隔
    private LocalDateTime submitTime;
    private BigDecimal score;
    private String teacherComment;
    private String status;          // 未提交 / 已提交 / 已批改
    private BigDecimal aiScore;     // AI智能评分
    private String aiReport;        // AI评分报告（JSON字符串）
    private String aiStatus;        // AI评分状态：评分中 / 已完成 / 失败
    private LocalDateTime aiScoredAt; // AI评分时间
}
