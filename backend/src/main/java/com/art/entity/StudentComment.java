package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class StudentComment implements Serializable {

    private Integer commentId;
    private Integer studentId;
    private Integer classId;
    private Integer teacherId;
    private String content;
    private LocalDateTime createTime;
}
