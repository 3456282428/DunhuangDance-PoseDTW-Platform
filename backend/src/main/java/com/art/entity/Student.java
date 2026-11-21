package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class Student implements Serializable {

    private Integer studentId;
    private String studentNo;
    private Integer userId;
    private String username;   // 登录账号（学号，非持久化）
    private String name;
    private String gender;        // 男 / 女
    private LocalDate birthDate;
    private String parentPhone;
    private String email;
    private LocalDate enrollDate;
    private Integer status;       // 1-在读 0-离校
}