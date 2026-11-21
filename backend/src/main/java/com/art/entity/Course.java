package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Course implements Serializable {
    private Integer courseId;
    private String courseNo;
    private String courseName;   // 唯一
    private String category;     // 乐器/舞蹈/美术/声乐
    private Integer categoryId;  // 类别ID（权威外键，按 category 文本匹配回填）
    private String ageGroup;
    private String level;
    private Integer totalHours;
    private Integer lessonDuration;
    private BigDecimal originalPrice;
    private BigDecimal price;
    private String intro;
    private Integer status;      // 1上架 0下架
    private Integer sortOrder;
    private LocalDateTime createTime;
}
