package com.art.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class ExamRegistration {
    private Integer regId;
    private Integer projectId;
    private Integer studentId;
    private Integer classId;
    private String realName;
    private String gender;
    private LocalDate birthDate;
    private String idCard;
    private String phone;
    private String materialUrl;
    private Integer teacherId;
    private String teacherComment;
    private String status;        // 待审核/已确认/已缴费/已完成/已驳回/已取消
    private String auditRemark;
    private String examTime;
    private String examRoom;
    private String seatNo;
    private String result;
    private String award;
    private String certificateNo;
    private LocalDateTime createTime;
}
