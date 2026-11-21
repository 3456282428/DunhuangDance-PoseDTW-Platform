package com.art.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentProgressDetailVO {

    private Integer studentId;
    private String studentName;
    private Integer classId;
    private String className;

    // 出勤明细（月份或日期）
    private List<AttendanceStudentVO> attendanceRecords;
    // 作业成绩列表
    private List<HomeworkSubmissionVO> homeworkRecords;
    // 考级/比赛记录
    private List<Map<String, Object>> examCompetitionRecords;
    // 教师评语列表
    private List<String> comments;
}
