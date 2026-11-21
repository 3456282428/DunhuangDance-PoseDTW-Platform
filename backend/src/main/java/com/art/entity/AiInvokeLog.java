package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI调用日志（统一调用入口全量留痕，含知识库检索命中情况）
  * @author liukang&&heyan
 */
@Data
public class AiInvokeLog implements Serializable {

    private Long invokeId;
    /** 调用端别：ADMIN/TEACHER/STUDENT */
    private String callerScope;
    private Integer callerUserId;
    private String question;
    /** 实际检索的知识库范围 */
    private String kbScope;
    private Integer hitCount;
    /** AI回答（预留字段，当前为NULL） */
    private String answer;
    /** 状态：PENDING待接入/SUCCESS/FAILED */
    private String invokeStatus;
    private String errorMsg;
    private LocalDateTime invokeTime;
}
