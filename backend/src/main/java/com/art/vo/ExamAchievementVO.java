package com.art.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 考级/比赛成果视图（由 exam_registration JOIN exam_project 实时得出，
 * 替代原 exam_competition 冗余台账，字段名与旧对象保持一致以兼容上层逻辑）
 * @author liukang&&heyan
 */
@Data
public class ExamAchievementVO implements Serializable {
    private Integer regId;
    private Integer studentId;
    private Integer classId;
    private Integer projectId;
    private String activityType;   // 考级 / 比赛（来自 exam_project）
    private String activityName;   // 项目名称（来自 exam_project）
    private LocalDate activityDate;// 考试日期（来自 exam_project）
    private String result;
    private String award;
}
