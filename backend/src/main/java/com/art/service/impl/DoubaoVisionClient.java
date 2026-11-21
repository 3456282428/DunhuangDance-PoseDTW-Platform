package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.AiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 豆包多模态理解客户端（通道二·语义增强层）：
 * - JDK原生HttpClient对接方舟Ark /responses端点（doubao-seed-2-0系列）
 * - 输入规范：多张关键帧（input_image，data URL）+ 文本指令（input_text）
 * - 负责动作理解/发力点标注/视听一致性判断/自然语言改进建议
 * - 通道一（MediaPipe+DTW精确评分）不依赖本客户端，两者互补
  * @author liukang&&heyan
 */
@Slf4j
@Component
public class DoubaoVisionClient {

    /** 方舟API默认地址（ai_model.endpoint未配置时兜底） */
    private static final String DEFAULT_ENDPOINT = "https://ark.cn-beijing.volces.com/api/v3";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper objectMapper;

    public DoubaoVisionClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 多模态理解调用。
     *
     * @param model      ai_model配置（model=current_version，endpoint，apiKey）
     * @param imageDataUrls 关键帧data URL列表（data:image/jpeg;base64,...）
     * @param prompt     文本指令（含量化评分上下文与输出规范）
     * @return 模型输出文本
     */
    public String analyze(AiModel model, List<String> imageDataUrls, String prompt) throws Exception {
        String base = (model.getEndpoint() != null && !model.getEndpoint().isBlank())
                ? model.getEndpoint() : DEFAULT_ENDPOINT;
        String modelName = (model.getCurrentVersion() != null && !model.getCurrentVersion().isBlank())
                ? model.getCurrentVersion() : "doubao-seed-2-0-mini-260428";

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        ArrayNode input = root.putArray("input");
        ObjectNode userMsg = input.addObject();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");
        if (imageDataUrls != null) {
            for (String url : imageDataUrls) {
                content.addObject().put("type", "input_image").put("image_url", url);
            }
        }
        content.addObject().put("type", "input_text").put("text", prompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/responses"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + model.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .build();
        // 网络波动防护：连接超时/重置等传输层异常自动重试（共3次，退避2s/4s）
        IOException lastErr = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<String> resp = HTTP_CLIENT.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                return parseResponse(resp);
            } catch (IOException e) {
                lastErr = e;
                log.warn("豆包语义API第{}/3次调用失败: {}", attempt, e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException("豆包语义API调用被中断");
                    }
                }
            }
        }
        throw new BusinessException("豆包语义API连接失败：" + lastErr.getMessage());
    }

    /** 解析Ark /responses响应：output数组中type=message节点的output_text拼接 */
    private String parseResponse(HttpResponse<String> resp) {
        if (resp.statusCode() != 200) {
            String body = resp.body() == null ? "" : resp.body();
            throw new BusinessException("豆包语义API返回 " + resp.statusCode()
                    + "：" + body.substring(0, Math.min(300, body.length())));
        }
        JsonNode respRoot;
        try {
            respRoot = objectMapper.readTree(resp.body());
        } catch (Exception e) {
            throw new BusinessException("豆包语义API响应解析失败：" + e.getMessage());
        }
        // output数组含reasoning与message两类节点，取message节点的output_text拼接
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : respRoot.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode part : item.path("content")) {
                if ("output_text".equals(part.path("type").asText())) {
                    sb.append(part.path("text").asText());
                }
            }
        }
        if (sb.length() == 0) {
            throw new BusinessException("豆包语义API未返回有效内容");
        }
        return sb.toString();
    }
}
