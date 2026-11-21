package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型-知识库关联约定（约定：AI调用时优先检索的知识库范围/分类）
  * @author liukang&&heyan
 */
@Data
public class AiModelKbBinding implements Serializable {

    private Integer bindingId;
    private Integer modelId;
    /** 知识库范围：ADMIN/TEACHER/STUDENT/ALL */
    private String kbScope;
    /** 限定分类（NULL=不限） */
    private String kbCategory;
    /** 检索优先级（越大越先检索） */
    private Integer priority;
    private Integer enabled;
    private LocalDateTime createTime;

    /** 关联展示字段：模型名称（非表字段） */
    private String modelName;
}
