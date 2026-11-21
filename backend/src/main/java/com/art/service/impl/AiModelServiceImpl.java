package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.dto.AiQueryRequest;
import com.art.entity.AiInvokeLog;
import com.art.entity.AiModel;
import com.art.entity.AiModelKbBinding;
import com.art.entity.KbDoc;
import com.art.mapper.AiInvokeLogMapper;
import com.art.mapper.AiModelKbBindingMapper;
import com.art.mapper.AiModelMapper;
import com.art.service.AiModelService;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.art.vo.AiQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI模型管理实现：模型注册与知识库绑定约定管理；统一调用入口按约定
 * "知识库优先检索"后将命中条目作为上下文调用已启用的大模型生成回答。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final AiModelMapper modelMapper;
    private final AiModelKbBindingMapper bindingMapper;
    private final AiInvokeLogMapper invokeLogMapper;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    // ============ 模型注册管理 ============

    @Override
    public List<AiModel> listModels() {
        return modelMapper.selectAll();
    }

    @Override
    public AiModel getModel(Integer modelId) {
        return modelMapper.selectById(modelId);
    }

    @Override
    public void createModel(AiModel model) {
        if (model.getModelName() == null || model.getModelName().isBlank()) {
            throw new BusinessException("模型名称不能为空");
        }
        if (model.getStatus() == null || model.getStatus().isBlank()) model.setStatus("UNCONFIGURED");
        if (model.getModelType() == null || model.getModelType().isBlank()) model.setModelType("LLM");
        model.setCreateTime(LocalDateTime.now());
        modelMapper.insert(model);
    }

    @Override
    public void updateModel(Integer modelId, AiModel model) {
        if (modelMapper.selectById(modelId) == null) throw new BusinessException("AI模型不存在");
        model.setModelId(modelId);
        modelMapper.update(model);
    }

    @Override
    public void deleteModel(Integer modelId) {
        bindingMapper.deleteByModelId(modelId);
        modelMapper.deleteById(modelId);
    }

    // ============ 模型-知识库关联约定 ============

    @Override
    public List<AiModelKbBinding> listBindings() {
        return bindingMapper.selectAll();
    }

    @Override
    public void createBinding(AiModelKbBinding binding) {
        if (binding.getModelId() == null) throw new BusinessException("必须指定AI模型");
        if (binding.getKbScope() == null || binding.getKbScope().isBlank()) {
            throw new BusinessException("必须指定知识库范围（ADMIN/TEACHER/STUDENT/ALL）");
        }
        String scope = binding.getKbScope().toUpperCase();
        if (!"ALL".equals(scope) && !"ADMIN".equals(scope) && !"TEACHER".equals(scope) && !"STUDENT".equals(scope)) {
            throw new BusinessException("非法的知识库范围：" + binding.getKbScope());
        }
        binding.setKbScope(scope);
        if (binding.getPriority() == null) binding.setPriority(0);
        if (binding.getEnabled() == null) binding.setEnabled(1);
        binding.setCreateTime(LocalDateTime.now());
        bindingMapper.insert(binding);
    }

    @Override
    public void updateBindingEnabled(Integer bindingId, Integer enabled) {
        bindingMapper.updateEnabled(bindingId, enabled != null && enabled == 1 ? 1 : 0);
    }

    @Override
    public void deleteBinding(Integer bindingId) {
        bindingMapper.deleteById(bindingId);
    }

    // ============ 统一调用入口 ============

    @Override
    public AiQueryResponse invoke(AiQueryRequest request, String callerScope, Integer callerUserId) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException("提问内容不能为空");
        }
        if (request.getQuestion().length() > 1000) {
            throw new BusinessException("提问内容不能超过1000字");
        }

        // 1. 按 ai_model_kb_binding 约定确定检索范围：启用绑定 scope=ALL 时检索三端，否则检索调用端
        String kbScope = callerScope;
        List<String> searchScopes = new ArrayList<>();
        boolean hasAllBinding = bindingMapper.selectAll().stream()
                .anyMatch(b -> b.getEnabled() != null && b.getEnabled() == 1 && "ALL".equals(b.getKbScope()));
        // 安全边界：仅管理端可跨端检索；教师/学员端一律只检索本端（本端已含机构共享条目），
        // 避免管理端全量明细（账号权限/薪资/财务等）被非管理端检索到
        if (hasAllBinding && KnowledgeService.SCOPE_ADMIN.equals(callerScope)) {
            searchScopes.add(KnowledgeService.SCOPE_ADMIN);
            searchScopes.add(KnowledgeService.SCOPE_TEACHER);
            searchScopes.add(KnowledgeService.SCOPE_STUDENT);
            kbScope = "ALL";
        } else {
            searchScopes.add(callerScope);
        }

        // 2. 知识库优先检索（AI回答依据约定）
        // 日期查询扩展：含"今天/明天/昨天…"时补充对应 yyyy-MM-dd 关键词，仅用于检索，原始提问仍完整留痕
        String searchQuestion = expandDateKeywords(request.getQuestion());
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        List<KbDoc> retrieved = new ArrayList<>();
        for (String scope : searchScopes) {
            // 教师/学员端检索时仅检索本人可见范围（本人条目+机构共享），需换算档案ID
            Integer ownerId = null;
            if (KnowledgeService.SCOPE_TEACHER.equals(scope)) {
                ownerId = SecurityUtil.getCurrentTeacherId();
            } else if (KnowledgeService.SCOPE_STUDENT.equals(scope)) {
                ownerId = SecurityUtil.getCurrentStudentId();
            }
            retrieved.addAll(knowledgeService.searchForAi(scope, ownerId, searchQuestion, request.getCategory(), topK));
            if (retrieved.size() >= topK) break;
        }
        if (retrieved.size() > topK) retrieved = new ArrayList<>(retrieved.subList(0, topK));

        // 3. 全量留痕（先插 PENDING，模型调用结束后回写结果）
        AiInvokeLog invokeLog = new AiInvokeLog();
        invokeLog.setCallerScope(callerScope);
        invokeLog.setCallerUserId(callerUserId);
        invokeLog.setQuestion(request.getQuestion());
        invokeLog.setKbScope(kbScope);
        invokeLog.setHitCount(retrieved.size());
        invokeLog.setInvokeStatus("PENDING");
        invokeLog.setInvokeTime(LocalDateTime.now());
        invokeLogMapper.insert(invokeLog);

        AiQueryResponse response = new AiQueryResponse();
        response.setInvokeId(invokeLog.getInvokeId());
        response.setKbScope(kbScope);
        response.setHitCount(retrieved.size());
        response.setRetrievedKnowledge(retrieved);

        // 4. 存在已启用且配置完整的大模型时，以检索条目为上下文生成回答
        AiModel model = pickEnabledModel();
        if (model == null) {
            response.setStatus("PENDING_AI_INTEGRATION");
            response.setMessage("未配置可用的大模型：已返回知识库检索结果");
            return response;
        }
        try {
            String answer = callLlm(model, request.getQuestion(), retrieved);
            invokeLog.setInvokeStatus("SUCCESS");
            invokeLog.setAnswer(answer);
            invokeLogMapper.updateResult(invokeLog);
            response.setStatus("SUCCESS");
            response.setMessage("AI回答已生成（依据知识库命中 " + retrieved.size() + " 条）");
            response.setAnswer(answer);
        } catch (Exception e) {
            log.warn("AI模型调用失败：{}", e.getMessage());
            String err = e.getMessage() == null ? "未知错误" : e.getMessage();
            if (err.length() > 500) err = err.substring(0, 500);
            invokeLog.setInvokeStatus("FAILED");
            invokeLog.setErrorMsg(err);
            invokeLogMapper.updateResult(invokeLog);
            response.setStatus("FAILED");
            response.setMessage("AI模型调用失败：" + err + "（已返回知识库检索结果）");
        }
        return response;
    }

    /** 日期查询扩展：提问含"今天/今日/明天/昨天/后天/前天"时，追加对应 yyyy-MM-dd 独立片段用于检索 */
    private String expandDateKeywords(String q) {
        if (q == null || q.isBlank()) return q;
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder(q);
        if (q.contains("今天") || q.contains("今日")) sb.append(' ').append(today);
        if (q.contains("明天")) sb.append(' ').append(today.plusDays(1));
        if (q.contains("后天")) sb.append(' ').append(today.plusDays(2));
        if (q.contains("昨天")) sb.append(' ').append(today.minusDays(1));
        if (q.contains("前天")) sb.append(' ').append(today.minusDays(2));
        return sb.toString();
    }

    /** 选取一个已启用且 endpoint/apiKey 配置完整的LLM聊天模型（排除VISION/VIDEO等专用通道） */
    private AiModel pickEnabledModel() {
        return modelMapper.selectAll().stream()
                .filter(m -> "LLM".equals(m.getModelType()))
                .filter(m -> "ENABLED".equals(m.getStatus()))
                .filter(m -> m.getEndpoint() != null && !m.getEndpoint().isBlank())
                .filter(m -> m.getApiKey() != null && !m.getApiKey().isBlank())
                .findFirst().orElse(null);
    }

    /** 以 OpenAI 兼容协议调用大模型，知识库命中条目作为上下文（JDK 原生 HttpClient） */
    private String callLlm(AiModel model, String question, List<KbDoc> retrieved) throws Exception {
        String modelName = (model.getCurrentVersion() != null && !model.getCurrentVersion().isBlank())
                ? model.getCurrentVersion() : "deepseek-chat";

        StringBuilder kb = new StringBuilder();
        if (retrieved.isEmpty()) {
            kb.append("（知识库未命中相关条目）");
        } else {
            for (int i = 0; i < retrieved.size(); i++) {
                KbDoc d = retrieved.get(i);
                kb.append('[').append(i + 1).append("] ").append(d.getTitle());
                if (d.getCategory() != null && !d.getCategory().isBlank()) kb.append("（").append(d.getCategory()).append(')');
                kb.append('\n');
                String content = d.getContent() == null ? "" : d.getContent();
                if (content.length() > 600) content = content.substring(0, 600) + "…";
                kb.append(content).append("\n\n");
            }
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("temperature", 0.3);
        root.put("stream", false);
        LocalDate now = LocalDate.now();
        String weekday = "星期" + "一二三四五六日".charAt(now.getDayOfWeek().getValue() - 1);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content",
                "你是艺术培训机构的AI知识助手。当前日期：" + now + " " + weekday + "。"
                        + "请优先依据给定的知识库条目回答问题；"
                        + "涉及具体日期时以条目中的实际日期为准，条目中没有该日期信息就明确说明该日期没有相关安排，不要用其它日期的条目代替；"
                        + "条目与问题无关时不要强行引用，可基于一般知识简要回答并注明非知识库内容。"
                        + "使用中文，分点简洁作答，不超过300字。");
        messages.addObject().put("role", "user").put("content",
                "知识库条目：\n" + kb + "\n问题：" + question);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(model.getEndpoint()))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + model.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            String body = resp.body() == null ? "" : resp.body();
            throw new BusinessException("模型服务返回 " + resp.statusCode() + "：" + body.substring(0, Math.min(200, body.length())));
        }
        JsonNode content = objectMapper.readTree(resp.body()).path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new BusinessException("模型未返回有效回答");
        }
        return content.asText();
    }
}
