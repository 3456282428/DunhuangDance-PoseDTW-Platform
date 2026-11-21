package com.art.controller.common;

import com.art.common.Result;
import com.art.dto.AiQueryRequest;
import com.art.dto.AiVideoRequest;
import com.art.service.AiModelService;
import com.art.service.AiVideoService;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.art.vo.AiQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI统一调用入口
 * 调用约定：AI模型回答前优先检索并使用知识库中的已有数据作为回答依据
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiInvokeController {

    private final AiModelService aiModelService;
    private final AiVideoService aiVideoService;

    /**
     * AI知识问答统一入口
     * 请求规范：{ scope: ADMIN/TEACHER/STUDENT, question: 必填≤1000字, topK: 1-20默认5, category: 可选 }
     */
    @PostMapping("/invoke")
    public Result<AiQueryResponse> invoke(@RequestBody AiQueryRequest request) {
        String callerScope = resolveCallerScope();
        return Result.success(aiModelService.invoke(request, callerScope, SecurityUtil.getCurrentUserId()));
    }

    /**
     * 豆包视频模型调用入口（预留）：提交视频生成任务
     * 请求规范：{ prompt: 必填≤500字, resolution: 480p/720p/1080p默认720p,
     *           durationSeconds: 5/10默认5, fps: 默认24, refVideoUrl: 可选, modelId: 可选 }
     * 模型默认DISABLED，返回PENDING_AI_INTEGRATION；启用后无缝对接
     */
    @PostMapping("/video/tasks")
    public Result<Map<String, Object>> submitVideoTask(@RequestBody AiVideoRequest request) {
        String callerScope = resolveCallerScope();
        return Result.success(aiVideoService.submitVideoTask(request, callerScope, SecurityUtil.getCurrentUserId()));
    }

    /**
     * 豆包视频模型调用入口（预留）：查询视频生成任务状态
     */
    @GetMapping("/video/tasks/{taskId}")
    public Result<Map<String, Object>> getVideoTask(@PathVariable String taskId) {
        String callerScope = resolveCallerScope();
        return Result.success(aiVideoService.getVideoTask(taskId, callerScope, SecurityUtil.getCurrentUserId()));
    }

    private String resolveCallerScope() {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null) return KnowledgeService.SCOPE_STUDENT;
        // 依据角色映射调用端别
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (var ga : auth.getAuthorities()) {
                String a = String.valueOf(ga);
                if (a.contains("ADMIN")) return KnowledgeService.SCOPE_ADMIN;
                if (a.contains("TEACHER")) return KnowledgeService.SCOPE_TEACHER;
                if (a.contains("STUDENT")) return KnowledgeService.SCOPE_STUDENT;
            }
        }
        return KnowledgeService.SCOPE_STUDENT;
    }
}
