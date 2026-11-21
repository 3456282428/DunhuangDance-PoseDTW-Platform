package com.art.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
/**
 * @author liukang&&heyan
 */
public class TeacherProfileVO {

    private Integer teacherId;
    private String teacherNo;
    private String name;
    private String gender;
    private String phone;
    private String major;
    private LocalDate hireDate;
    private Integer status;

    // 扩展信息
    private Integer classCount;      // 授课班级数
    private Long totalHours;         // 累计课时
}
