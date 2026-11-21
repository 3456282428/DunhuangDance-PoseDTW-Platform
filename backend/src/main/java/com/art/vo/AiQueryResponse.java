package com.art.vo;

import com.art.entity.KbDoc;
import lombok.Data;

import java.util.List;

/**
 * AI统一调用入口响应（预留规范）
  * @author liukang&&heyan
 */
@Data
public class AiQueryResponse {

    /** 调用日志ID（ai_invoke_log） */
    private Long invokeId;

    /** 处理状态：PENDING_AI_INTEGRATION（未配置可用模型）/ SUCCESS / FAILED */
    private String status;

    /** 提示信息 */
    private String message;

    /** 实际执行检索的知识库范围 */
    private String kbScope;

    /** 知识库检索命中条数 */
    private Integer hitCount;

    /**
     * 知识库检索命中条目（AI回答依据：后续接入大模型后，
     * 该列表将作为上下文传给模型，回答基于知识库已有数据生成）
     */
    private List<KbDoc> retrievedKnowledge;

    /** AI生成的回答（未配置模型或调用失败时为null） */
    private String answer;
}
