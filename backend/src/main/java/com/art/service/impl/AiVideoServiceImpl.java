package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.dto.AiVideoRequest;
import com.art.entity.AiInvokeLog;
import com.art.entity.AiModel;
import com.art.entity.KbDoc;
import com.art.mapper.AiInvokeLogMapper;
import com.art.mapper.AiModelMapper;
import com.art.service.AiVideoService;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 豆包视频模型服务实现（预留）：
 * - 完成接口定义、参数规范、调用入口与全量留痕
 * - 不实现具体视频生成业务逻辑：ai_model中豆包视频模型默认DISABLED，
 *   调用时返回PENDING_AI_INTEGRATION，后续在管理端配置endpoint/apiKey并ENABLED即可无缝对接
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoServiceImpl implements AiVideoService {

    private static final Set<String> ALLOWED_RESOLUTIONS = Set.of("480p", "720p", "1080p");
    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(5, 10);
    private static final int DEFAULT_FPS = 24;

    private final AiModelMapper modelMapper;
    private final AiInvokeLogMapper invokeLogMapper;
    private final KnowledgeService knowledgeService;
    private final DoubaoVideoClient doubaoVideoClient;

    @Override
    public Map<String, Object> submitVideoTask(AiVideoRequest request, String callerScope, Integer callerUserId) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new BusinessException("生成内容描述不能为空");
        }
        if (request.getPrompt().length() > 500) {
            throw new BusinessException("生成内容描述不能超过500字");
        }
        String resolution = request.getResolution() == null || request.getResolution().isBlank()
                ? "720p" : request.getResolution().toLowerCase();
        if (!ALLOWED_RESOLUTIONS.contains(resolution)) {
            throw new BusinessException("非法分辨率：" + resolution + "（允许 480p/720p/1080p）");
        }
        Integer duration = request.getDurationSeconds() == null ? 5 : request.getDurationSeconds();
        if (!ALLOWED_DURATIONS.contains(duration)) {
            throw new BusinessException("非法时长：" + duration + "秒（允许 5/10秒）");
        }
        Integer fps = request.getFps() == null ? DEFAULT_FPS : request.getFps();

        // 1. 定位豆包视频模型（provider=DOUBAO, model_type=VIDEO）
        AiModel model = pickVideoModel(request.getModelId());

        // 2. 知识库优先检索（AI调用约定：优先使用知识库已有数据作为生成依据）
        Integer ownerId = resolveOwnerId(callerScope);
        List<KbDoc> retrieved = new ArrayList<>(knowledgeService.searchForAi(
                callerScope, ownerId, request.getPrompt(), null, 3));

        // 3. 全量留痕
        AiInvokeLog invokeLog = new AiInvokeLog();
        invokeLog.setCallerScope(callerScope);
        invokeLog.setCallerUserId(callerUserId);
        invokeLog.setQuestion("[视频生成] " + request.getPrompt());
        invokeLog.setKbScope(callerScope);
        invokeLog.setHitCount(retrieved.size());
        invokeLog.setInvokeStatus("PENDING");
        invokeLog.setInvokeTime(LocalDateTime.now());
        invokeLogMapper.insert(invokeLog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invokeId", invokeLog.getInvokeId());
        result.put("hitCount", retrieved.size());
        if (model == null) {
            result.put("status", "PENDING_AI_INTEGRATION");
            result.put("message", "豆包视频模型未注册或未启用，接口已预留（请在管理端AI模型管理中配置）");
            return result;
        }

        // 4. 模型已启用时调用方舟API创建任务（预留入口，本期默认DISABLED不会到达）
        if (!"ENABLED".equals(model.getStatus())) {
            result.put("status", "PENDING_AI_INTEGRATION");
            result.put("modelName", model.getModelName());
            result.put("message", "豆包视频模型当前为" + model.getStatus() + "状态，视频生成暂未开通");
            return result;
        }
        try {
            String taskId = doubaoVideoClient.createVideoTask(model, request.getPrompt(),
                    resolution, duration, fps, request.getRefVideoUrl());
            invokeLog.setInvokeStatus("SUCCESS");
            invokeLog.setAnswer("视频生成任务已创建: " + taskId);
            invokeLogMapper.updateResult(invokeLog);
            result.put("status", "RUNNING");
            result.put("videoTaskId", taskId);
            result.put("message", "视频生成任务已提交");
        } catch (Exception e) {
            String err = e.getMessage() == null ? "未知错误" : e.getMessage();
            if (err.length() > 500) err = err.substring(0, 500);
            log.warn("豆包视频模型调用失败：{}", err);
            invokeLog.setInvokeStatus("FAILED");
            invokeLog.setErrorMsg(err);
            invokeLogMapper.updateResult(invokeLog);
            result.put("status", "FAILED");
            result.put("message", "豆包视频模型调用失败：" + err);
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideoTask(String videoTaskId, String callerScope, Integer callerUserId) {
        if (videoTaskId == null || videoTaskId.isBlank()) {
            throw new BusinessException("任务ID不能为空");
        }
        AiModel model = pickVideoModel(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("videoTaskId", videoTaskId);
        if (model == null || !"ENABLED".equals(model.getStatus())) {
            result.put("status", "PENDING_AI_INTEGRATION");
            result.put("message", "豆包视频模型未启用，暂无可查询的视频生成任务");
            return result;
        }
        try {
            JsonNode node = doubaoVideoClient.getVideoTask(model, videoTaskId);
            result.put("status", node.path("status").asText("UNKNOWN").toUpperCase());
            result.put("videoUrl", node.path("content").path("video_url").asText(null));
            return result;
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "查询视频生成任务失败：" + e.getMessage());
            return result;
        }
    }

    /** 选取豆包视频模型：优先指定ID，否则取唯一 provider=DOUBAO 且 model_type=VIDEO 的记录 */
    private AiModel pickVideoModel(Integer modelId) {
        List<AiModel> models = modelMapper.selectAll();
        if (modelId != null) {
            return models.stream()
                    .filter(m -> modelId.equals(m.getModelId()))
                    .filter(m -> "VIDEO".equalsIgnoreCase(m.getModelType()))
                    .findFirst().orElse(null);
        }
        return models.stream()
                .filter(m -> "VIDEO".equalsIgnoreCase(m.getModelType()))
                .filter(m -> m.getProvider() != null && m.getProvider().toUpperCase().contains("DOUBAO"))
                .findFirst().orElse(null);
    }

    /** 教师/学员端知识库检索需限定本人可见范围（与统一调用入口一致） */
    private Integer resolveOwnerId(String callerScope) {
        if (KnowledgeService.SCOPE_TEACHER.equals(callerScope)) {
            return SecurityUtil.getCurrentTeacherId();
        }
        if (KnowledgeService.SCOPE_STUDENT.equals(callerScope)) {
            return SecurityUtil.getCurrentStudentId();
        }
        return null;
    }
}
