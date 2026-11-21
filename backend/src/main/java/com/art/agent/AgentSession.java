package com.art.agent;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 会话（内存态工作副本）：保留多轮对话历史、待确认动作与「槽位记忆」，
 * 支撑连续复杂任务与省略/指代理解；超时自动失效。
 * <p>
 * 与数据库的关系：本对象是 LLM 提示词的即时工作副本（截断窗口、槽位），
 * 全量消息另由 AgentService 双写至 agent_message，重启后按会话 ID 回填。
 * @author liukang&&heyan
 */
public class AgentSession {

    public record Msg(String role, String content) {}

    /** 待确认动作（敏感工具调用挂起态） */
    public record PendingAction(String actionId, String toolName, Map<String, Object> args, String description) {}

    /** 注入 LLM 的历史窗口（条） */
    private static final int HISTORY_CAP = 12;
    /** 单条历史注入 LLM 的截断长度 */
    private static final int MSG_LIMIT = 800;
    /** 槽位记忆上限（避免无限膨胀） */
    private static final int SLOT_CAP = 24;

    private final String sessionId;
    private final Integer userId;
    private final String username;

    private final Deque<Msg> history = new ArrayDeque<>();
    /** 跨轮次槽位记忆：即使用户省略，也能沿用并替换相应维度（如「那教师呢」仅替换角色） */
    private final Map<String, String> slots = new ConcurrentHashMap<>();

    private volatile String title;
    private volatile PendingAction pending;
    private volatile LocalDateTime lastActive = LocalDateTime.now();

    public AgentSession(String sessionId, Integer userId, String username) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
    }

    public String getSessionId() { return sessionId; }
    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getLastActive() { return lastActive; }

    public synchronized void append(String role, String content) {
        if (history.size() >= HISTORY_CAP) history.pollFirst();
        history.addLast(new Msg(role, content.length() > MSG_LIMIT ? content.substring(0, MSG_LIMIT) : content));
        lastActive = LocalDateTime.now();
    }

    /** 由数据库历史回填（取最近 HISTORY_CAP 条），用于重启/换端后续聊 */
    public synchronized void hydrate(List<Msg> msgs) {
        history.clear();
        if (msgs == null) return;
        int from = Math.max(0, msgs.size() - HISTORY_CAP);
        for (int i = from; i < msgs.size(); i++) {
            Msg m = msgs.get(i);
            String c = m.content();
            history.addLast(new Msg(m.role(), c != null && c.length() > MSG_LIMIT ? c.substring(0, MSG_LIMIT) : c));
        }
        lastActive = LocalDateTime.now();
    }

    public synchronized Deque<Msg> snapshotHistory() {
        return new ArrayDeque<>(history);
    }

    /* ==================== 槽位记忆 ==================== */

    public void putSlot(String key, Object value) {
        if (key == null || value == null) return;
        String v = String.valueOf(value).trim();
        if (v.isEmpty()) return;
        if (slots.size() >= SLOT_CAP && !slots.containsKey(key)) {
            // 已满则按插入顺序淘汰最早的一个（LinkedHashMap 语义近似）
            String oldest = slots.keySet().iterator().next();
            slots.remove(oldest);
        }
        slots.put(key, v.length() > 80 ? v.substring(0, 80) : v);
    }

    public Map<String, String> getSlots() {
        return new LinkedHashMap<>(slots);
    }

    /** 槽位提示词片段（无槽位返回 null） */
    public String slotsPrompt() {
        Map<String, String> s = getSlots();
        if (s.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        s.forEach((k, v) -> sb.append(k).append('=').append(v).append("；"));
        return sb.toString();
    }

    public PendingAction getPending() { return pending; }
    public void setPending(PendingAction p) { this.pending = p; }

    public boolean isExpired(int ttlMinutes) {
        return lastActive.plusMinutes(ttlMinutes).isBefore(LocalDateTime.now());
    }
}
