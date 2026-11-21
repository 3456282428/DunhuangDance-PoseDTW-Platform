package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识库关联映射（ref 级关系：业务实体之间的引用关系，用于跨知识库关联召回与关联可视化）
  * @author liukang&&heyan
 */
@Data
public class KbRelation implements Serializable {

    private Long relationId;
    /** 源业务引用（如 schedule:12） */
    private String srcRef;
    /** 目标业务引用（如 class:3） */
    private String dstRef;
    /** 关系类型：BELONGS_TO/OF_CLASS/IN_CLASS/OF_TEACHER/OF_STUDENT/OF_COURSE/IN_ROOM/OF_PROJECT/OF_HOMEWORK/OF_SCHEDULE/OF_CATEGORY/CONTENT_SIM */
    private String relType;
    /** 关联维度：ORG组织/TEACH教学/FIN财务/ASSET资产/CONTENT内容 */
    private String relDimension;
    /** 关联来源：BUSINESS_FK业务外键推导/CONTENT_SIM内容相似计算 */
    private String relSource;
    private String srcLabel;
    private String dstLabel;
    private BigDecimal weight;
    private LocalDateTime createTime;
}
