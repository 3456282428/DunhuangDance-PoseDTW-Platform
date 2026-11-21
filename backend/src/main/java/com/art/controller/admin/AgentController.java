package com.art.controller.admin;

import com.art.agent.AgentContext;
import com.art.agent.AgentService;
import com.art.agent.AgentSession;
import com.art.agent.AgentTool;
import com.art.agent.AgentToolRegistry;
import com.art.common.Result;
import com.art.entity.AgentLog;
import com.art.entity.AgentMessageRecord;
import com.art.entity.AgentSessionRecord;
import com.art.mapper.AgentLogMapper;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 智能体统一入口（管理员端 + 教师端 + 学生端）：
 * 对话调度 / 敏感操作确认 / 工具目录 / 多会话管理（新建·列表·切换·删除·历史）/ 审计查询。
 * 数据边界由工具层 allowedRoles 与归属校验双重保证。
 * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class AgentController {

    private final AgentService agentService;
    private final AgentToolRegistry registry;
    private final AgentLogMapper agentLogMapper;

    public record ChatRequest(String sessionId, String message) {}
    public record ConfirmRequest(String sessionId, String actionId, Boolean approve) {}

    /** 对话入口：意图识别→工具调用→反馈闭环 */
    @PostMapping("/chat")
    public Result<AgentService.ChatResponse> chat(@RequestBody ChatRequest req) {
        return Result.success(agentService.chat(currentContext(), req.sessionId(), req.message()));
    }

    /** 敏感操作确认/拒绝 */
    @PostMapping("/confirm")
    public Result<AgentService.ChatResponse> confirm(@RequestBody ConfirmRequest req) {
        boolean approve = req.approve() != null && req.approve();
        return Result.success(agentService.confirm(currentContext(), req.sessionId(), req.actionId(), approve));
    }

    /** 当前角色可用的工具目录（前端能力面板） */
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> tools() {
        AgentContext ctx = currentContext();
        List<Map<String, Object>> list = new ArrayList<>();
        for (AgentTool t : registry.listFor(ctx)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.name());
            m.put("description", t.description());
            m.put("requiresConfirmation", t.requiresConfirmation());
            m.put("riskHint", t.riskHint());
            list.add(m);
        }
        return Result.success(list);
    }

    /* ==================== 多会话管理 ==================== */

    /** 会话列表（按最后活跃倒序） */
    @GetMapping("/sessions")
    public Result<List<AgentSessionRecord>> sessions() {
        return Result.success(agentService.listSessions(SecurityUtil.getCurrentUserId()));
    }

    /** 新建会话（可不带标题，首条消息自动生成标题） */
    @PostMapping("/sessions")
    public Result<AgentSessionRecord> createSession(@RequestBody(required = false) Map<String, String> body) {
        AgentContext ctx = currentContext();
        String title = body == null ? null : body.get("title");
        AgentSession s = agentService.createSession(ctx, title);
        AgentSessionRecord rec = new AgentSessionRecord();
        rec.setSessionId(s.getSessionId());
        rec.setUserId(ctx.userId());
        rec.setUsername(ctx.username());
        rec.setTitle(s.getTitle());
        rec.setMsgCount(0);
        return Result.success(rec);
    }

    /** 会话历史消息（切换会话时回填） */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AgentMessageRecord>> messages(@PathVariable String sessionId) {
        return Result.success(agentService.history(SecurityUtil.getCurrentUserId(), sessionId));
    }

    /** 删除会话（含消息） */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable String sessionId) {
        return Result.success(agentService.deleteSession(SecurityUtil.getCurrentUserId(), sessionId));
    }

    /** 最近操作审计：管理员看全量，教师/学生仅看本人 */
    @GetMapping("/logs")
    public Result<List<AgentLog>> logs() {
        AgentContext ctx = currentContext();
        if (ctx.hasRole("ADMIN")) {
            return Result.success(agentLogMapper.selectRecent(30));
        }
        return Result.success(agentLogMapper.selectRecentByUser(ctx.userId(), 30));
    }

    private AgentContext currentContext() {
        List<String> roles = new ArrayList<>();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) auth.getAuthorities().forEach(a -> roles.add(String.valueOf(a).replace("ROLE_", "")));
        return new AgentContext(SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUsername(), roles);
    }
}
