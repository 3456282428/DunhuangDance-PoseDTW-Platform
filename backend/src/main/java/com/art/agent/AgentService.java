package com.art.agent;

import com.art.agent.AgentSession.Msg;
import com.art.common.BusinessException;
import com.art.entity.AgentLog;
import com.art.entity.AgentMessageRecord;
import com.art.entity.AgentSessionRecord;
import com.art.entity.AiModel;
import com.art.mapper.AgentChatMapper;
import com.art.mapper.AgentLogMapper;
import com.art.mapper.AiModelMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 调度器（借鉴 spring-ai-alibaba ChatClient/Advisor 职责划分）：
 * 意图识别 → 工具匹配 → 权限与确认门控 → 执行 → 观察回填 → 最多3步链式续调 → 反馈闭环。
 * <p>
 * 本次升级要点：
 * <ul>
 *   <li><b>意图识别</b>：规则快速通道（寒暄/能力咨询零 LLM 开销）+ 提示词 few-shot 示例 + JSON 容错解析 + 一次纠正重试；</li>
 *   <li><b>上下文记忆</b>：会话历史「内存 + 数据库」双写，持久化会话与消息，重启可续聊；</li>
 *   <li><b>槽位/指代记忆</b>：跨轮记录最近工具与参数槽位，支撑「那教师呢」「改成明天」等省略与指代补全；</li>
 *   <li><b>多轮与工具链</b>：最多 3 步链式调用，重复调用去重中断，失败即停并交由模型总结。</li>
 * </ul>
 * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    /** 内存会话空闲失效时长（分钟） */
    private static final int SESSION_TTL_MINUTES = 30;
    /** 单条用户消息允许的最大连续工具调用轮次（链式上限，防死循环） */
    private static final int MAX_TOOL_ROUNDS = 3;
    /** 内存缓存会话上限（防内存膨胀） */
    private static final int MAX_CACHED_SESSIONS = 200;
    /** 会话列表返回上限 */
    private static final int SESSION_LIST_LIMIT = 50;
    /** 会话历史返回上限 */
    private static final int MESSAGE_LIMIT = 100;

    private final AgentToolRegistry registry;
    private final AiModelMapper modelMapper;
    private final AgentLogMapper agentLogMapper;
    private final AgentChatMapper agentChatMapper;
    private final ObjectMapper objectMapper;

    /** 会话工作副本缓存：sessionId -> Session（内存态，超时失效；全量消息另落库） */
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    /* ==================== 对外响应结构 ==================== */

    public record StepRecord(String tool, Map<String, Object> args, boolean success, String summary, Object data) {}

    public record ChatResponse(String type, String sessionId, String content,
                               List<StepRecord> steps, PendingInfo pendingAction) {}

    public record PendingInfo(String actionId, String tool, Map<String, Object> args,
                              String description, String riskHint) {}

    /* ==================== 会话管理（多段对话） ==================== */

    /** 新建会话（内存 + 数据库双写），返回会话 */
    public AgentSession createSession(AgentContext ctx, String title) {
        String sid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        AgentSession session = new AgentSession(sid, ctx.userId(), ctx.username());
        session.setTitle(title);
        sessions.put(sid, session);
        evictIfNeeded();
        safe(() -> {
            AgentSessionRecord rec = new AgentSessionRecord();
            rec.setSessionId(sid);
            rec.setUserId(ctx.userId());
            rec.setUsername(ctx.username());
            rec.setTitle(title);
            agentChatMapper.insertSession(rec);
        }, "创建会话");
        return session;
    }

    /**
     * 解析当前会话：内存命中直接返回；内存缺失则按 ID 从数据库回填历史；
     * ID 为空/不属于当前用户/已被删除时新建会话。
     */
    public AgentSession resolveSession(AgentContext ctx, String sessionId) {
        evictExpired();
        if (sessionId != null && !sessionId.isBlank()) {
            AgentSession cached = sessions.get(sessionId);
            if (cached != null && !cached.isExpired(SESSION_TTL_MINUTES)) return cached;
            AgentSessionRecord rec = loadRecord(sessionId, ctx.userId());
            if (rec != null) {
                AgentSession restored = new AgentSession(rec.getSessionId(), ctx.userId(), ctx.username());
                restored.setTitle(rec.getTitle());
                restored.hydrate(loadHistory(rec.getSessionId()));
                sessions.put(rec.getSessionId(), restored);
                return restored;
            }
        }
        return createSession(ctx, null);
    }

    /** 会话列表（数据库为准，保证多端一致） */
    public List<AgentSessionRecord> listSessions(Integer userId) {
        try {
            List<AgentSessionRecord> list = agentChatMapper.selectSessionsByUser(userId, SESSION_LIST_LIMIT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("[Agent] 会话列表查询失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 会话历史消息（持久化记录，供前端切换会话时回填） */
    public List<AgentMessageRecord> history(Integer userId, String sessionId) {
        AgentSessionRecord rec = loadRecord(sessionId, userId);
        if (rec == null) return List.of();
        try {
            List<AgentMessageRecord> list = agentChatMapper.selectMessages(sessionId, MESSAGE_LIMIT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("[Agent] 会话历史查询失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 删除会话（含消息） */
    public boolean deleteSession(Integer userId, String sessionId) {
        AgentSessionRecord rec = loadRecord(sessionId, userId);
        if (rec == null) return false;
        sessions.remove(sessionId);
        safe(() -> agentChatMapper.deleteMessages(sessionId), "删除会话消息");
        safe(() -> agentChatMapper.deleteSession(sessionId), "删除会话");
        return true;
    }

    /* ==================== 主入口 ==================== */

    public ChatResponse chat(AgentContext ctx, String sessionId, String message) {
        if (message == null || message.isBlank()) throw new BusinessException("消息内容不能为空");
        if (message.length() > 500) throw new BusinessException("单条消息不能超过500字");
        AgentSession session = resolveSession(ctx, sessionId);
        boolean firstMsg = session.getTitle() == null;

        // 已有挂起的敏感操作时收到新消息：视为放弃该操作
        if (session.getPending() != null) {
            AgentSession.PendingAction abandoned = session.getPending();
            session.setPending(null);
            writeLog(session, ctx, message, abandoned.toolName(), abandoned.args(), 2,
                    "REJECTED", "用户发送新消息，挂起操作已自动取消");
        }

        session.append("user", message);
        persistMessage(session.getSessionId(), "user", message);
        if (firstMsg) {
            String title = abbreviate(message, 30);
            session.setTitle(title);
            safe(() -> agentChatMapper.updateSessionTitle(session.getSessionId(), title), "更新会话标题");
        }

        // 意图识别快速通道：寒暄/能力咨询直接应答（降延迟；模型不可用时仍可用）
        String quick = tryFastIntent(message, ctx);
        if (quick != null) {
            session.append("assistant", quick);
            persistAssistant(session, quick);
            session.putSlot("上轮诉求", abbreviate(message, 60));
            writeLog(session, ctx, message, null, null, 0, "CHAT", "规则快速意图");
            return new ChatResponse("reply", session.getSessionId(), quick, List.of(), null);
        }

        List<StepRecord> steps = new ArrayList<>();
        StringBuilder observation = new StringBuilder();
        Set<String> executed = new HashSet<>();

        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {
            String llmOutput;
            try {
                llmOutput = callAgentLlmWithRetry(buildMessages(ctx, session, message, steps, observation));
            } catch (BusinessException e) {
                // 工具已执行过时不能直接报错丢弃结果：退化为基于观察的总结
                if (steps.isEmpty()) {
                    return finishError(ctx, session, message, steps, "AI服务暂不可用：" + e.getMessage());
                }
                break;
            }
            JsonNode decision = parseDecision(llmOutput);
            if (decision == null) {
                // 模型未按协议输出：追加纠错指令再试一次
                try {
                    llmOutput = callAgentLlmWithRetry(buildCorrectiveMessages(ctx, session, message, llmOutput));
                    decision = parseDecision(llmOutput);
                } catch (BusinessException e) {
                    if (steps.isEmpty()) {
                        return finishError(ctx, session, message, steps, "AI服务暂不可用：" + e.getMessage());
                    }
                    break;
                }
            }
            if (decision == null) {
                // 纠错后仍不合规：原文回退，保证可用性
                String fallback = abbreviate(llmOutput, 600);
                session.append("assistant", fallback);
                persistAssistant(session, fallback);
                writeLog(session, ctx, message, null, null, 0, "CHAT", "模型未按JSON协议输出，原文返回");
                return new ChatResponse("reply", session.getSessionId(), fallback, steps, null);
            }
            String action = decision.path("action").asText("");

            if ("tool".equals(action)) {
                String toolName = decision.path("tool").asText("");
                Map<String, Object> args = jsonToMap(decision.path("args"));
                AgentTool tool = registry.get(toolName);
                if (tool == null) {
                    observation.append("【系统反馈】工具 ").append(toolName).append(" 不存在，请改用其他工具或直接回答。\n");
                    continue;
                }
                if (!registry.allowed(tool, ctx)) {
                    return finishError(ctx, session, message, steps, "当前角色无权执行该操作。");
                }
                // 重复调用去重：同一轮次内相同工具+参数不重复执行，避免模型死循环
                String callKey = toolName + "@" + args;
                if (!executed.add(callKey)) {
                    observation.append("【系统反馈】工具 ").append(toolName)
                            .append(" 已用相同参数调用过，请基于已有结果直接回答或更换参数。\n");
                    continue;
                }
                // 敏感操作门控：挂起并请求用户确认
                if (tool.requiresConfirmation()) {
                    String actionId = UUID.randomUUID().toString().substring(0, 8);
                    String desc = decision.path("thought").asText(tool.description());
                    session.setPending(new AgentSession.PendingAction(actionId, toolName, args, desc));
                    String content = "我准备执行敏感操作：**" + tool.description() + "**\n\n"
                            + "- 操作参数：`" + abbreviate(String.valueOf(args), 200) + "`\n"
                            + "- 风险提示：" + tool.riskHint() + "\n\n请确认是否执行？";
                    session.append("assistant", content);
                    persistAssistant(session, content);
                    memorySlots(session, message, toolName, args);
                    writeLog(session, ctx, message, toolName, args, 0, "PENDING", "等待用户确认");
                    return new ChatResponse("confirm", session.getSessionId(), content, steps,
                            new PendingInfo(actionId, toolName, args, desc, tool.riskHint()));
                }
                // 普通工具：立即执行
                ToolResult result = registry.execute(toolName, args, ctx);
                steps.add(new StepRecord(toolName, args, result.success(), result.summary(), result.data()));
                writeLog(session, ctx, message, toolName, args, 0,
                        result.success() ? "SUCCESS" : "FAILED",
                        abbreviate(result.summary() + (result.suggestion() != null ? "；建议：" + result.suggestion() : ""), 400));
                observation.append("【工具 ").append(toolName).append(" 执行").append(result.success() ? "成功" : "失败")
                        .append("】\n").append(result.summary()).append('\n');
                if (result.suggestion() != null) observation.append("后续建议：").append(result.suggestion()).append('\n');
                memorySlots(session, message, toolName, args);
                // 失败则不再续轮，直接由模型总结反馈
                if (!result.success()) round = MAX_TOOL_ROUNDS;
                continue;
            }

            // reply：结束循环
            String content = decision.path("content").asText("（模型未返回内容）");
            if (!steps.isEmpty()) {
                // 内存历史带出已执行工具与参数，支撑后续轮次的指代理解
                StringBuilder trace = new StringBuilder(content).append("\n（本轮已调用：");
                for (StepRecord s : steps) trace.append(s.tool()).append(s.args()).append('；');
                trace.append('）');
                session.append("assistant", trace.toString());
            } else {
                session.append("assistant", content);
            }
            persistAssistant(session, content);
            session.putSlot("上轮诉求", abbreviate(message, 60));
            if (steps.isEmpty()) {
                writeLog(session, ctx, message, null, null, 0, "CHAT", abbreviate(content, 200));
            }
            return new ChatResponse(steps.isEmpty() ? "reply" : "executed",
                    session.getSessionId(), content, steps, null);
        }

        // 达到链式步数上限：把最后一次观察交给模型做最终总结（强制reply）
        String summary = summarize(ctx, session, message, observation.toString());
        session.append("assistant", summary);
        persistAssistant(session, summary);
        session.putSlot("上轮诉求", abbreviate(message, 60));
        return new ChatResponse("executed", session.getSessionId(), summary, steps, null);
    }

    /* ==================== 敏感操作确认/拒绝 ==================== */

    public ChatResponse confirm(AgentContext ctx, String sessionId, String actionId, boolean approve) {
        AgentSession session = resolveSession(ctx, sessionId);
        AgentSession.PendingAction pending = session.getPending();
        if (pending == null || !pending.actionId().equals(actionId)) {
            return new ChatResponse("error", session.getSessionId(),
                    "该操作已失效（可能已超时或被新消息取消），请重新发起。", List.of(), null);
        }
        session.setPending(null);
        if (!approve) {
            writeLog(session, ctx, "【用户拒绝】" + pending.description(), pending.toolName(), pending.args(),
                    2, "REJECTED", "用户主动拒绝执行");
            String content = "好的，已取消该操作，未对系统做任何修改。";
            session.append("assistant", content);
            persistAssistant(session, content);
            return new ChatResponse("reply", session.getSessionId(), content, List.of(), null);
        }
        AgentTool tool = registry.get(pending.toolName());
        if (tool == null || !registry.allowed(tool, ctx)) {
            return new ChatResponse("error", session.getSessionId(), "工具不可用或无权执行。", List.of(), null);
        }
        ToolResult result = registry.execute(pending.toolName(), pending.args(), ctx);
        writeLog(session, ctx, "【用户确认】" + pending.description(), pending.toolName(), pending.args(),
                1, result.success() ? "SUCCESS" : "FAILED", abbreviate(result.summary(), 400));
        StepRecord step = new StepRecord(pending.toolName(), pending.args(), result.success(), result.summary(), result.data());
        String content;
        if (result.success()) {
            content = "✅ 已按您的确认执行完成：\n\n" + result.summary();
        } else {
            String reason = result.summary() != null && result.summary().startsWith("执行失败：")
                    ? result.summary().substring(5) : result.summary();
            content = "❌ 执行失败：" + reason
                    + (result.suggestion() != null ? "\n\n建议：" + result.suggestion() : "");
        }
        session.append("assistant", content);
        persistAssistant(session, content);
        return new ChatResponse("executed", session.getSessionId(), content, List.of(step), null);
    }

    /* ==================== 意图识别快速通道 ==================== */

    /** 规则快速意图：寒暄/道谢/能力咨询直接应答，省去一次 LLM 往返 */
    private String tryFastIntent(String msg, AgentContext ctx) {
        String m = msg.trim().toLowerCase();
        if (m.length() > 12) return null;
        if (m.matches("^(你好|您好|hi|hello|嗨|哈喽|在吗|在么)[!！。~～]*$")) {
            return "你好！我是" + personaName(ctx) + "，可以直接帮你查询和操作系统数据。\n"
                    + "试试说「查一下本月新增学员」，或问「你能做什么」。";
        }
        if (m.matches("^(谢谢|感谢|多谢|thanks|thank you|3q)[!！。~～]*$")) {
            return "不客气，还有其他需要随时说～";
        }
        if (m.matches("^(你能做什么|能做什么|会做什么|有什么功能|有哪些功能|功能|帮助|help)[?？!！。~～]*$")) {
            StringBuilder sb = new StringBuilder("我可以调用以下工具帮你完成任务：\n");
            for (AgentTool t : registry.listFor(ctx)) sb.append("- ").append(t.description()).append('\n');
            sb.append("\n直接说出你的需求即可，例如「查一下学生账号数」「看看张老师本周的排课」。");
            return sb.toString();
        }
        return null;
    }

    private String personaName(AgentContext ctx) {
        return ctx.hasRole("ADMIN") ? "智能体助手「程程」"
                : ctx.hasRole("TEACHER") ? "AI教学助手「助助」" : "AI学舞伙伴「舞舞」";
    }

    /* ==================== LLM 调用（OpenAI兼容协议，JSON模式） ==================== */

    private ArrayNode buildMessages(AgentContext ctx, AgentSession session, String message,
                                    List<StepRecord> steps, StringBuilder observation) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode messages = root.putArray("messages");
        String persona;
        String scopeNote = null;
        if (ctx.hasRole("ADMIN")) {
            persona = "艺术培训机构管理系统的AI智能体助手「程程」，可通过调用工具直接完成系统操作";
        } else if (ctx.hasRole("TEACHER")) {
            persona = "艺术培训机构教师端的AI教学助手「助助」，可通过调用工具直接完成教学相关操作";
            scopeNote = "数据范围：仅可访问当前教师本人授课的班级、学员、作业与排课数据；不要尝试查询其他教师或全机构数据。\n";
        } else {
            persona = "艺术培训机构学生端的AI学舞伙伴「舞舞」，可通过调用工具帮助学生查询个人学习信息";
            scopeNote = "数据范围：仅可访问当前学生本人的课程、作业、考勤、订单与报名数据；不要尝试查询其他同学或机构运营数据。"
                    + "作业提交须由学生在「我的作业」页面上传视频完成，你无法代为提交，也不要承诺代为提交。\n";
        }
        StringBuilder sys = new StringBuilder();
        sys.append("你是").append(persona).append("。\n");
        sys.append("当前操作者：").append(ctx.username()).append("（角色：").append(String.join(",", ctx.roles())).append("）。\n");
        if (scopeNote != null) sys.append(scopeNote);
        sys.append("今天是 ").append(java.time.LocalDate.now()).append("。\n\n");
        sys.append("【可用工具】\n").append(registry.catalogPrompt(ctx)).append('\n');
        sys.append("【输出协议】你必须且只能输出一个JSON对象，禁止输出任何其他文字：\n");
        sys.append("1) 直接回答：{\"action\":\"reply\",\"content\":\"回答内容（中文，简洁，可用Markdown）\"}\n");
        sys.append("2) 调用工具：{\"action\":\"tool\",\"tool\":\"工具名\",\"args\":{参数},\"thought\":\"一句话说明要做什么\"}\n\n");
        sys.append("【示例】\n");
        sys.append("用户：你好 ⇒ {\"action\":\"reply\",\"content\":\"你好！我是智能体助手，可以帮你查询和操作系统数据。\"}\n");
        sys.append("用户：帮我看下这个学生 ⇒ {\"action\":\"tool\",\"tool\":\"list_users\",\"args\":{\"keyword\":\"张\"},\"thought\":\"按关键词查询相关账号\"}\n");
        sys.append("用户：那教师呢 ⇒ {\"action\":\"tool\",\"tool\":\"list_users\",\"args\":{\"role\":\"TEACHER\"},\"thought\":\"沿用上轮诉求，仅切换角色为教师\"}\n\n");
        sys.append("【规则】\n");
        sys.append("- 需要查询系统数据时必须调用工具，禁止编造数据；闲聊与不需数据的问题直接reply。\n");
        sys.append("- 参数只能来自用户消息或对话上下文/槽位；信息不足时用reply向用户澄清，不要瞎猜。\n");
        sys.append("- 注意理解省略句与指代：结合上下文与槽位补全或切换参数（例如先问学生账号数再问「那教师呢」，应把角色参数切换为TEACHER重新调用，而不是重复上次参数）。\n");
        sys.append("- 可链式调用：先概览再取明细（最多3步），每步只调用一个工具；同一工具+相同参数不要重复调用。\n");
        sys.append("- 标注【敏感操作】的工具同样必须直接输出 tool 调用；系统会自动向用户发起确认，你绝不能在reply里代替用户确认或请求确认，也禁止模仿「我准备执行敏感操作…请确认」这类确认卡片文案，那是系统专用格式。\n");
        sys.append("- 日期一律yyyy-MM-dd；页数参数不超过10。\n");
        sys.append("- 回答不超过300字，分点呈现关键数据。\n");
        String slotText = session.slotsPrompt();
        if (slotText != null) {
            sys.append("\n【上下文槽位】用户此前已提供的信息：").append(slotText)
                    .append("当用户使用省略或指代（如「那教师呢」「再来一页」「改成明天」）时，请沿用槽位并仅替换变化的维度，不要重复询问已有信息。\n");
        }
        if (!steps.isEmpty()) {
            sys.append("\n【本轮已执行的工具观察】\n").append(observation)
                    .append("请基于观察结果用reply总结给用户（成功则呈现关键数据，失败则说明原因与建议）。");
        }
        messages.addObject().put("role", "system").put("content", sys.toString());
        for (Msg m : session.snapshotHistory()) {
            String c = m.content();
            // 确认卡片文案只面向用户展示；注入模型前替换为中性描述，
            // 防止模型模仿卡片格式伪造"确认流程"而非真正调用工具
            if ("assistant".equals(m.role()) && c != null && c.startsWith("我准备执行敏感操作")) {
                c = "（已向用户展示敏感操作确认卡，等待用户确认或取消）";
            }
            messages.addObject().put("role", m.role()).put("content", c);
        }
        if (!steps.isEmpty()) {
            messages.addObject().put("role", "user").put("content",
                    "（系统提示：以上是工具执行结果，请输出reply总结。原始请求：" + message + "）");
        }
        return messages;
    }

    /** 纠错重试提示：模型未按 JSON 协议输出时，明确要求只输出 JSON */
    private ArrayNode buildCorrectiveMessages(AgentContext ctx, AgentSession session, String message, String badOutput) {
        ArrayNode messages = objectMapper.createObjectNode().putArray("messages");
        messages.addObject().put("role", "system").put("content",
                "你是" + personaName(ctx) + "。请严格只输出一个JSON对象，不要输出任何解释文字或Markdown代码块。\n"
                        + "格式：{\"action\":\"reply\",\"content\":\"...\"} 或 {\"action\":\"tool\",\"tool\":\"工具名\",\"args\":{...}}。");
        for (Msg m : session.snapshotHistory()) {
            messages.addObject().put("role", m.role()).put("content", m.content());
        }
        messages.addObject().put("role", "user").put("content",
                "你刚才的输出不符合JSON协议，请仅输出一个合法JSON对象。原始请求：" + message);
        return messages;
    }

    /** LLM 调用（空响应/网络异常自动退避重试，最多3次；deepseek偶发返回空content） */
    private String callAgentLlmWithRetry(ArrayNode messages) {
        BusinessException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return callAgentLlm(messages);
            } catch (BusinessException e) {
                last = e;
                log.warn("[Agent] LLM调用失败（第{}次尝试）：{}", attempt, e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(400L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw last;
    }

    private String callAgentLlm(ArrayNode messages) {
        AiModel model = modelMapper.selectAll().stream()
                .filter(m -> "LLM".equals(m.getModelType()))
                .filter(m -> "ENABLED".equals(m.getStatus()))
                .filter(m -> m.getEndpoint() != null && !m.getEndpoint().isBlank())
                .filter(m -> m.getApiKey() != null && !m.getApiKey().isBlank())
                .findFirst().orElse(null);
        if (model == null) throw new BusinessException("未配置可用的大模型（ai_model 无 ENABLED 记录）");
        String modelName = (model.getCurrentVersion() != null && !model.getCurrentVersion().isBlank())
                ? model.getCurrentVersion() : "deepseek-chat";

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("temperature", 0.2);
        root.put("stream", false);
        root.set("messages", messages);
        // JSON模式（DeepSeek兼容）：强制模型输出合法JSON
        root.putObject("response_format").put("type", "json_object");

        try {
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
            JsonNode msgNode = objectMapper.readTree(resp.body()).path("choices").path(0).path("message");
            String content = msgNode.path("content").asText("");
            if (content.isBlank()) content = msgNode.path("reasoning_content").asText(""); // reasoning模型兼容
            if (content.isBlank()) throw new BusinessException("模型未返回有效内容");
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("模型调用失败：" + e.getMessage());
        }
    }

    /** 工具观察的最终总结（强制 reply 输出） */
    private String summarize(AgentContext ctx, AgentSession session, String message, String observation) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content",
                "你是" + personaName(ctx) + "。请基于工具执行结果，用简洁中文（≤200字，可分点）向用户转述结果；"
                        + "成功则呈现关键数据，失败则说明原因并给出可行的后续操作建议。只输出JSON：{\"action\":\"reply\",\"content\":\"...\"}");
        messages.addObject().put("role", "user").put("content",
                "用户请求：" + message + "\n\n工具执行结果：\n" + observation);
        try {
            String out = callAgentLlm(messages);
            JsonNode d = parseDecision(out);
            if (d != null && !d.path("content").asText("").isBlank()) return d.path("content").asText();
        } catch (Exception e) {
            log.warn("Agent总结调用失败，回退原始摘要：{}", e.getMessage());
        }
        return observation.isBlank() ? "已完成处理。" : observation.toString();
    }

    /* ==================== 解析与审计 ==================== */

    /** 容错解析：剥离 Markdown 代码块/前后缀噪声，取首个 { 到末个 } 的 JSON 对象 */
    private JsonNode parseDecision(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int i = s.indexOf('{');
        int j = s.lastIndexOf('}');
        if (i < 0 || j <= i) return null;
        try {
            JsonNode node = objectMapper.readTree(s.substring(i, j + 1));
            return node != null && node.has("action") ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isObject()) return Map.of();
        return objectMapper.convertValue(node, Map.class);
    }

    private String abbreviate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    /* ==================== 上下文/槽位记忆 ==================== */

    /** 记录跨轮槽位：最近工具与其参数，供省略/指代补全 */
    private void memorySlots(AgentSession session, String userMessage, String toolName, Map<String, Object> args) {
        session.putSlot("最近工具", toolName);
        if (args != null) {
            for (Map.Entry<String, Object> e : args.entrySet()) {
                if (e.getValue() == null) continue;
                session.putSlot(e.getKey(), e.getValue());
            }
        }
        session.putSlot("上轮诉求", abbreviate(userMessage, 60));
    }

    /* ==================== 会话持久化（双写，异常隔离） ==================== */

    private void persistMessage(String sessionId, String role, String content) {
        safe(() -> {
            AgentMessageRecord m = new AgentMessageRecord();
            m.setSessionId(sessionId);
            m.setRole(role);
            m.setContent(content);
            agentChatMapper.insertMessage(m);
            agentChatMapper.touchSession(sessionId);
        }, "写入会话消息");
    }

    private void persistAssistant(AgentSession session, String content) {
        persistMessage(session.getSessionId(), "assistant", content);
    }

    private AgentSessionRecord loadRecord(String sessionId, Integer userId) {
        if (sessionId == null || sessionId.isBlank() || userId == null) return null;
        try {
            AgentSessionRecord rec = agentChatMapper.selectSession(sessionId);
            if (rec == null || !userId.equals(rec.getUserId())) return null;
            return rec;
        } catch (Exception e) {
            log.warn("[Agent] 会话查询失败：{}", e.getMessage());
            return null;
        }
    }

    private List<Msg> loadHistory(String sessionId) {
        List<AgentMessageRecord> rows;
        try {
            rows = agentChatMapper.selectMessages(sessionId, MESSAGE_LIMIT);
        } catch (Exception e) {
            log.warn("[Agent] 会话历史回填失败：{}", e.getMessage());
            return List.of();
        }
        List<Msg> msgs = new ArrayList<>();
        if (rows != null) {
            for (AgentMessageRecord r : rows) msgs.add(new Msg(r.getRole(), r.getContent()));
        }
        return msgs;
    }

    private void evictExpired() {
        sessions.entrySet().removeIf(e -> e.getValue().isExpired(SESSION_TTL_MINUTES));
    }

    private void evictIfNeeded() {
        if (sessions.size() <= MAX_CACHED_SESSIONS) return;
        // 简单淘汰：移除最早活跃的一批
        sessions.entrySet().stream()
                .sorted((a, b) -> a.getValue().getLastActive().compareTo(b.getValue().getLastActive()))
                .limit(Math.max(1, sessions.size() - MAX_CACHED_SESSIONS))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(sessions::remove);
    }

    private void safe(Runnable action, String what) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[Agent] {}失败（不影响主流程）：{}", what, e.getMessage());
        }
    }

    private ChatResponse finishError(AgentContext ctx, AgentSession session, String message,
                                     List<StepRecord> steps, String content) {
        session.append("assistant", content);
        persistAssistant(session, content);
        writeLog(session, ctx, message, null, null, 0, "ERROR", abbreviate(content, 200));
        return new ChatResponse("error", session.getSessionId(), content, steps, null);
    }

    private void writeLog(AgentSession session, AgentContext ctx, String userMessage,
                          String toolName, Map<String, Object> args, Integer confirmed,
                          String status, String summary) {
        safe(() -> {
            AgentLog l = new AgentLog();
            l.setSessionId(session.getSessionId());
            l.setUserId(ctx.userId());
            l.setUsername(ctx.username());
            l.setUserMessage(abbreviate(userMessage, 500));
            l.setToolName(toolName);
            l.setToolArgs(args == null ? null : abbreviate(String.valueOf(args), 500));
            l.setConfirmed(confirmed);
            l.setExecStatus(status);
            l.setResultSummary(abbreviate(summary, 500));
            l.setCreateTime(LocalDateTime.now());
            agentLogMapper.insert(l);
        }, "写入Agent审计日志");
    }

    /** 保留：对外暴露的会话缓存大小（运维观测） */
    public int cachedSessionCount() {
        return sessions.size();
    }
}
