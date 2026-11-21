package com.art.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 调课目标时段冲突明细（教师端选时间后实时检测）
 *
 * @author liukang&&heyan
 */
@Data
public class ScheduleConflictVO {
    private Integer scheduleId;
    private String className;
    private String teacherName;
    private String classroomName;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    /** 冲突原因：教师冲突 / 教室冲突 / 班级冲突 */
    private List<String> reasons = new ArrayList<>();

    public String getSummary() {
        String time = (courseDate != null ? courseDate.toString() : "") + " "
                + (startTime != null ? startTime.toString().substring(0, 5) : "") + "-"
                + (endTime != null ? endTime.toString().substring(0, 5) : "");
        return (className == null ? "未知班级" : className) + "（"
                + (teacherName == null ? "" : teacherName + " · ")
                + (classroomName == null ? "" : classroomName) + "）" + time;
    }
}
