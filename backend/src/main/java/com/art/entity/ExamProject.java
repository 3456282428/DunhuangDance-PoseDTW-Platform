package com.art.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class ExamProject {
    private Integer projectId;
    private String name;
    private String activityType;   // 考级/比赛
    private String category;       // 乐器/舞蹈/美术/声乐
    private String organizer;
    private LocalDate signupStart;
    private LocalDate signupEnd;
    private LocalDate examDate;
    private String examStart;       // HH:mm
    private String examEnd;         // HH:mm
    private String examRoom;        // 考场
    private String location;
    private java.math.BigDecimal fee;
    private String requirement;
    private String awardDesc;
    private String materials;
    private String status;         // 报名中/即将截止/已结束
}
