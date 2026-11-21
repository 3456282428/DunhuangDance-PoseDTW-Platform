package com.art.service;

import com.art.entity.AiModel;
import com.art.entity.AiModelKbBinding;
import com.art.vo.AiQueryResponse;
import com.art.dto.AiQueryRequest;

import java.util.List;

/**
 * AI模型管理服务（预留）：
 * - 模型注册/绑定约定的管理能力（本期可用）
 * - 统一调用入口 AiClientService.invoke（占位实现，不实现具体AI业务逻辑，后续无缝对接）
  * @author liukang&&heyan
 */
public interface AiModelService {

    // ============ 模型注册管理（预留） ============
    List<AiModel> listModels();
    AiModel getModel(Integer modelId);
    void createModel(AiModel model);
    void updateModel(Integer modelId, AiModel model);
    void deleteModel(Integer modelId);

    // ============ 模型-知识库关联约定管理 ============
    List<AiModelKbBinding> listBindings();
    void createBinding(AiModelKbBinding binding);
    void updateBindingEnabled(Integer bindingId, Integer enabled);
    void deleteBinding(Integer bindingId);

    // ============ 统一调用入口（预留：知识库优先检索已实现，AI生成待接入） ============

    /**
     * AI统一调用入口：
     * 1. 校验参数与端别
     * 2. 按 ai_model_kb_binding 约定确定检索范围，优先检索知识库已有数据
     * 3. 全量留痕 ai_invoke_log
     * 4. 返回检索结果作为回答依据（answer 置空，状态 PENDING_AI_INTEGRATION）
     */
    AiQueryResponse invoke(AiQueryRequest request, String callerScope, Integer callerUserId);
}
