package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class AchievementStatsVO {

    private Integer totalStudents;          // 教授学员总数
    private Integer examCount;              // 参加考级人数
    private Integer examPassed;             // 考级通过人数
    private Double examPassRate;            // 考级通过率
    private Integer competitionCount;       // 参赛人数
    private Integer awardCount;             // 获奖人数
    private Double homeworkCompletionRate;  // 作业完成率
}
