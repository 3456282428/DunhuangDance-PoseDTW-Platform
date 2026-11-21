package com.art.service;

import com.art.dto.AiVideoRequest;

import java.util.Map;

/**
 * 豆包视频模型服务（预留）：
 * - 本期仅完成接口定义、参数规范与调用入口
 * - 不实现具体视频生成业务逻辑，模型默认DISABLED，后续启用后无缝对接
 * - 全量留痕ai_invoke_log，调用前优先检索知识库已有数据
  * @author liukang&&heyan
 */
public interface AiVideoService {

    /**
     * 提交视频生成任务（预留）：
     * 1. 校验请求参数与端别
     * 2. 校验ai_model中豆包视频模型（provider=DOUBAO, model_type=VIDEO）配置与启用状态
     * 3. 优先检索知识库已有数据作为生成依据
     * 4. 全量留痕ai_invoke_log
     * 5. 模型启用时调用豆包方舟API创建视频生成任务；未启用时返回PENDING_AI_INTEGRATION
     *
     * @return {invokeId, status, message, videoTaskId(预留), retrievedKnowledge(命中条目摘要)}
     */
    Map<String, Object> submitVideoTask(AiVideoRequest request, String callerScope, Integer callerUserId);

    /**
     * 查询视频生成任务状态（预留）：
     * 模型启用时查询豆包方舟任务状态；未启用时返回任务尚未开通
     *
     * @return {videoTaskId, status(RUNNING/SUCCEEDED/FAILED/PENDING_AI_INTEGRATION), videoUrl(预留), message}
     */
    Map<String, Object> getVideoTask(String videoTaskId, String callerScope, Integer callerUserId);
}
