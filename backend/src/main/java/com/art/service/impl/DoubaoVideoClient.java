package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.AiModel;
import com.art.mapper.AiModelMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 豆包（火山引擎方舟Ark）视频生成客户端（预留）：
 * - 仅实现调用入口与参数组装规范（JDK原生HttpClient，不引入额外依赖）
 * - 对接Ark视频生成API：POST /contents/generations/tasks 创建任务、GET /contents/generations/tasks/{id} 查询
 * - 由AiVideoServiceImpl在模型ENABLED时调用；模型默认DISABLED，不会触发真实外呼
  * @author liukang&&heyan
 */
@Slf4j
@Component
public class DoubaoVideoClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** 方舟API默认地址（ai_model.endpoint未配置时兜底） */
    @Value("${ai.video.ark-endpoint:https://ark.cn-beijing.volces.com/api/v3}")
    private String arkEndpoint;

    private final ObjectMapper objectMapper;

    public DoubaoVideoClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 创建视频生成任务（参数规范见AiVideoRequest）。
     * Ark视频生成以文本指令内联参数：--resolution 720p --duration 5 --fps 24
     *
     * @return 任务ID（如 cgt-2024...）
     */
    public String createVideoTask(AiModel model, String prompt, String resolution,
                                  Integer durationSeconds, Integer fps, String refVideoUrl) throws Exception {
        String base = (model.getEndpoint() != null && !model.getEndpoint().isBlank())
                ? model.getEndpoint() : arkEndpoint;
        String modelName = (model.getCurrentVersion() != null && !model.getCurrentVersion().isBlank())
                ? model.getCurrentVersion() : "doubao-seedance-1-0-lite-t2v-250428";

        StringBuilder text = new StringBuilder(prompt);
        if (resolution != null && !resolution.isBlank()) text.append(" --resolution ").append(resolution);
        if (durationSeconds != null) text.append(" --duration ").append(durationSeconds);
        if (fps != null) text.append(" --fps ").append(fps);
        // 水印参数默认关闭（机构内部教学素材）
        text.append(" --watermark false");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        var content = root.putArray("content");
        content.addObject().put("type", "text").put("text", text.toString());
        if (refVideoUrl != null && !refVideoUrl.isBlank()) {
            content.addObject().put("type", "video_url")
                    .putObject("video_url").put("url", refVideoUrl);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/contents/generations/tasks"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + model.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            String body = resp.body() == null ? "" : resp.body();
            throw new BusinessException("豆包视频API返回 " + resp.statusCode()
                    + "：" + body.substring(0, Math.min(200, body.length())));
        }
        JsonNode id = objectMapper.readTree(resp.body()).path("id");
        if (id.isMissingNode() || id.asText().isBlank()) {
            throw new BusinessException("豆包视频API未返回任务ID");
        }
        return id.asText();
    }

    /**
     * 查询视频生成任务状态。
     *
     * @return {status: queued/running/succeeded/failed, videoUrl: 成品地址(成功时), error: 失败原因}
     */
    public JsonNode getVideoTask(AiModel model, String taskId) throws Exception {
        String base = (model.getEndpoint() != null && !model.getEndpoint().isBlank())
                ? model.getEndpoint() : arkEndpoint;
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(base + "/contents/generations/tasks/" + taskId))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + model.getApiKey())
                .GET().build();
        HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            String body = resp.body() == null ? "" : resp.body();
            throw new BusinessException("豆包视频API返回 " + resp.statusCode()
                    + "：" + body.substring(0, Math.min(200, body.length())));
        }
        return objectMapper.readTree(resp.body());
    }
}
