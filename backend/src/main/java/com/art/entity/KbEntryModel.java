package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识条目-3D模型推荐映射（变量映射：知识库内容变化后由规则引擎自动重算）
 * @author liukang&&heyan
 */
@Data
public class KbEntryModel implements Serializable {

    private Long id;
    /** 端别：ADMIN/TEACHER/STUDENT */
    private String kbScope;
    private Integer kbId;
    /** 推荐的3D模型ID */
    private Integer modelId;
    /** 匹配分（0~1） */
    private BigDecimal matchScore;
    /** 匹配规则说明（标签命中/关联加成/类别默认，可追溯） */
    private String matchRule;
    /** 命中的标签集合 */
    private String matchTags;
    private LocalDateTime updateTime;
}
