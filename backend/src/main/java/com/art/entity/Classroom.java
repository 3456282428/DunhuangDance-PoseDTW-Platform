package com.art.entity;

import lombok.Data;
import java.io.Serializable;

@Data
/**
 * @author liukang&&heyan
 */
public class Classroom implements Serializable {

    private Integer classroomId;
    private String classroomName;
    private Integer capacity;
    private String roomType;      // 钢琴教室、舞蹈教室、美术教室等
    private Integer status;       // 1-可用 0-停用
}