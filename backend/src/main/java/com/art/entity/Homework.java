package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Homework implements Serializable {

    private Integer homeworkId;
    private Integer classId;
    private Integer scheduleId;
    private String title;
    private String content;         // 作业题目
    private LocalDateTime createTime;
    private LocalDateTime deadline; // 截止时间
    private String refVideoUrl;     // 教师参考视频地址（AI评分用）
    private String refVideoName;    // 教师参考视频原始文件名
}
