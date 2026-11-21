package com.art.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学员端作业视图：作业题目 + 该学员的提交状态
  * @author liukang&&heyan
 */
@Data
public class StudentHomeworkVO {

    private Integer homeworkId;
    private Integer classId;
    private String className;
    private Integer scheduleId;
    private String title;
    private String content;         // 作业题目
    private String refVideoUrl;     // 教师参考视频地址（AI评分用）
    private String refVideoName;    // 教师参考视频原始文件名
    private LocalDateTime createTime;

    private Integer submissionId;
    private String submitContent;   // 学生提交的答案
    private String attachmentUrl;
    private String attachmentName;
    private LocalDateTime submitTime;
    private BigDecimal score;
    private String teacherComment;
    private String status;          // 未提交 / 已提交 / 已批改
    private BigDecimal aiScore;     // AI智能评分
    private String aiReport;        // AI评分报告（JSON字符串）
    private String aiStatus;        // AI评分状态：评分中 / 已完成 / 失败
}
