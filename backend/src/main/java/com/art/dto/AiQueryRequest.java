package com.art.dto;

import lombok.Data;

/**
 * AI统一调用入口请求参数（预留规范）
 * 调用约定：AI模型回答前将优先检索知识库已有数据作为回答依据
  * @author liukang&&heyan
 */
@Data
public class AiQueryRequest {

    /** 调用端别：ADMIN/TEACHER/STUDENT（决定默认检索的知识库范围） */
    private String scope;

    /** 提问内容（必填，最长1000字） */
    private String question;

    /** 知识库检索条数上限：1-20，默认5 */
    private Integer topK;

    /** 限定检索的知识库分类（可选，如"教案课件"） */
    private String category;
}
