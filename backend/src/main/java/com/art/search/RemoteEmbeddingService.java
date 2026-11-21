package com.art.search;

import com.art.common.BusinessException;
import com.art.entity.AiModel;
import com.art.mapper.AiModelMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程 Embedding 服务：对接 OpenAI 兼容协议的 /embeddings 接口（如 text-embedding-3-small、
 * 智谱 embedding-3、阿里百炼 text-embedding-v3 等），模型来源为 ai_model 表中
 * model_type=EMBEDDING 且 status=ENABLED 的记录（在「AI模型管理」页登记即可生效）。
 * 调用失败一律抛异常，由 {@link KbEmbeddingService} 降级为本地向量，保证检索链路可用。
 * @author liukang&&heyan
 */
@Component
public class RemoteEmbeddingService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    /** 单次批量向量化上限（避免请求体过大） */
    private static final int BATCH = 16;

    private final AiModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public RemoteEmbeddingService(AiModelMapper modelMapper, ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    /** 选取已启用的 Embedding 模型；未配置返回 null */
    public AiModel pick() {
        return modelMapper.selectAll().stream()
                .filter(m -> "EMBEDDING".equalsIgnoreCase(m.getModelType()))
                .filter(m -> "ENABLED".equals(m.getStatus()))
                .filter(m -> m.getEndpoint() != null && !m.getEndpoint().isBlank())
                .filter(m -> m.getApiKey() != null && !m.getApiKey().isBlank())
                .findFirst().orElse(null);
    }

    public String tag(AiModel m) {
        return "remote:" + (m.getModelName() == null ? "embedding" : m.getModelName());
    }

    /** 批量向量化（内部分批请求） */
    public List<float[]> embedBatch(AiModel model, List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += BATCH) {
            out.addAll(embedOnce(model, texts.subList(i, Math.min(texts.size(), i + BATCH))));
        }
        return out;
    }

    public float[] embed(AiModel model, String text) {
        return embedOnce(model, List.of(text == null ? "" : text)).get(0);
    }

    private List<float[]> embedOnce(AiModel model, List<String> texts) {
        String modelName = (model.getCurrentVersion() != null && !model.getCurrentVersion().isBlank())
                ? model.getCurrentVersion() : model.getModelName();
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", modelName);
            ArrayNode input = root.putArray("input");
            for (String t : texts) input.add(t == null ? "" : t);
            root.put("encoding_format", "float");

            String url = model.getEndpoint();
            if (!url.endsWith("/embeddings")) {
                url = url.endsWith("/") ? url + "embeddings" : url + "/embeddings";
            }
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + model.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                String body = resp.body() == null ? "" : resp.body();
                throw new BusinessException("Embedding 接口返回 " + resp.statusCode() + "：" + body.substring(0, Math.min(160, body.length())));
            }
            JsonNode data = objectMapper.readTree(resp.body()).path("data");
            if (!data.isArray() || data.size() != texts.size()) {
                throw new BusinessException("Embedding 返回条数不匹配");
            }
            List<float[]> out = new ArrayList<>(texts.size());
            for (JsonNode item : data) {
                JsonNode emb = item.path("embedding");
                if (!emb.isArray() || emb.size() == 0) throw new BusinessException("Embedding 返回空向量");
                float[] v = new float[emb.size()];
                double norm = 0;
                for (int i = 0; i < v.length; i++) {
                    v[i] = (float) emb.get(i).asDouble();
                    norm += (double) v[i] * v[i];
                }
                if (norm > 0) {
                    float f = (float) (1.0 / Math.sqrt(norm));
                    for (int i = 0; i < v.length; i++) v[i] *= f;
                }
                out.add(v);
            }
            return out;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Embedding 调用失败：" + e.getMessage());
        }
    }
}
