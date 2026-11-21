package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库条目（管理员/教师/学生三端同构实体，由 Mapper XML 绑定到不同表）
  * @author liukang&&heyan
 */
@Data
public class KbDoc implements Serializable {

    private Integer kbId;
    private String title;
    private String category;
    private String content;
    private String summary;
    private String tags;
    /** 来源：MANUAL手动 / AUTO_ASSIGN布置作业 / AUTO_GRADE批改 / AUTO_SUBMIT作业提交 / AUTO_SCHEDULE定时归集 */
    private String sourceType;
    /** 来源引用（如 homework:12 / submission:9 / daily_report:20261015），唯一防重复归集 */
    private String sourceRef;
    /** 归属人ID（教师ID/学员ID，管理员端NULL=机构级公开） */
    private Integer ownerId;
    private String ownerName;
    private String fileUrl;
    private String fileName;
    /** 状态：1启用（AI可检索） 0停用 */
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 检索相关度（0~1，混合检索：词法命中率 * 权重 + 向量余弦 * 权重），仅检索时填充 */
    private Double relevanceScore;
    /** 命中方式：lexical词法 / vector向量 / hybrid混合 / related关联召回 */
    private String matchType;
    /** 关联召回来源（如「排课课表 schedule:12」），matchType=related 时填充 */
    private String relatedFrom;
}
