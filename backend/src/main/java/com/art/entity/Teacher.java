package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
/**
 * @author liukang&&heyan
 */
public class Teacher implements Serializable {

    private Integer teacherId;
    private String teacherNo;
    private Integer userId;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;
    private String major;
    private String education;     // 学历
    private String school;        // 毕业院校
    private String title;         // 职称
    private String employType;    // 全职/兼职/实习/离职
    private Integer teachYears;   // 教龄
    private String photo;
    private String certs;         // 资质证件 JSON
    private transient String courseIds; // 非表列：由 teacher_course 关联表聚合得到的逗号串
    private BigDecimal hourlyRate;
    private BigDecimal baseSalary;
    private String intro;
    private LocalDate hireDate;
    private Integer status;       // 1-在职 0-离职

    // ===== 非持久化统计字段 =====
    private Integer classCount;
    private BigDecimal totalHours;
    private List<String> certList;
}
