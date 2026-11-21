package com.art.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 作业题目视图（教师端/管理端）
  * @author liukang&&heyan
 */
@Data
public class HomeworkVO {

    private Integer homeworkId;
    private Integer classId;
    private String className;
    private Integer scheduleId;
    private String title;
    private String content;         // 作业题目
    private String refVideoUrl;     // 教师参考视频地址（AI评分用）
    private String refVideoName;    // 教师参考视频原始文件名
    private LocalDateTime createTime;
    private LocalDateTime deadline; // 截止时间
    private String deadlineStatus;  // 未截止 / 已截止
    private Integer submittedCount; // 已提交人数
    private Integer totalStudents;  // 应交人数
    private java.math.BigDecimal avgScore; // 平均分
}
