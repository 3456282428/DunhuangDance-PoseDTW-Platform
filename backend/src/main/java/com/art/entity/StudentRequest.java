package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentRequest implements Serializable {

    private Integer requestId;
    private Integer studentId;
    private Integer teacherId;
    private Integer classId;
    private String requestType;   // 请假 / 缓考 / 补考
    private LocalDate requestDate;
    private String reason;
    private String status;        // 待处理 / 已批准 / 已拒绝
    private String replyRemark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
