package com.art.controller.common;

import com.art.common.Result;
import com.art.entity.DashboardOperationLog;
import com.art.service.AiModelMetricService;
import com.art.service.TechDashboardService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 技术可视化仪表盘 - 统一接口
 * 覆盖：知识库存储统计 / 文档数量趋势 / AI模型与版本迭代 / 模型性能指标 / 组件依赖拓扑 / 数据源健康
 * 权限：类级仅要求登录；数据范围按角色在 Service 内过滤；同步组件/模型升级/导出报表为管理员专属
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TechDashboardController {

    private final TechDashboardService dashboardService;
    private final AiModelMetricService metricService;

    /** 仪表盘总览（含权限声明、存储统计、文档趋势、模型列表、组件列表、拓扑、日志） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(dashboardService.overview(resolveRole(), days == null ? 30 : days));
    }

    /** 当前登录角色的仪表盘权限声明（模块显隐/按钮可用性） */
    @GetMapping("/permissions")
    public Result<Map<String, Object>> permissions() {
        return Result.success(dashboardService.permissions(resolveRole()));
    }

    /** 开源组件列表（支持平台/分类/更新状态/关键词筛选） */
    @GetMapping("/components")
    public Result<List<Map<String, Object>>> components(@RequestParam(required = false) String platform,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String updateStatus,
                                                        @RequestParam(required = false) String keyword) {
        return Result.success(dashboardService.buildComponents(resolveRole(), platform, category, updateStatus, keyword));
    }

    /** 开源组件详情（基础信息 + 版本迭代历史 + 依赖上下游） */
    @GetMapping("/components/{componentId}")
    public Result<Map<String, Object>> componentDetail(@PathVariable Integer componentId) {
        return Result.success(dashboardService.componentDetail(componentId, resolveRole()));
    }

    /** 触发开源组件同步（拉取远端最新版本与统计），仅管理员 */
    @PostMapping("/components/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> syncComponents() {
        return Result.success(dashboardService.syncComponents(resolveRole(), SecurityUtil.getCurrentUsername()));
    }

    /** 模型性能指标实时曲线（含汇总） */
    @GetMapping("/models/{modelId}/metrics")
    public Result<Map<String, Object>> modelMetrics(@PathVariable Integer modelId,
                                                    @RequestParam(defaultValue = "60") Integer limit) {
        return Result.success(dashboardService.modelMetrics(modelId, limit == null ? 60 : limit));
    }

    /** 触发模型更新（登记升级意向并留痕），仅管理员 */
    @PostMapping("/models/{modelId}/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> upgradeModel(@PathVariable Integer modelId,
                                                    @RequestParam(required = false) String version) {
        return Result.success(dashboardService.upgradeModel(modelId, version,
                SecurityUtil.getCurrentUsername(), resolveRole()));
    }

    /** 手动触发一次模型指标采样（便于演示/校准），仅管理员 */
    @PostMapping("/metrics/collect")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> collectMetrics() {
        java.util.List<com.art.entity.AiModelMetric> collected = metricService.collectAll();
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("collected", collected.size());
        out.put("metrics", collected);
        return Result.success(out);
    }

    /** API 调用与用量（真实访问留痕聚合：流量/分布/端点排行/占比/汇总），仅管理员 */
    @GetMapping("/api-usage")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> apiUsage(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(dashboardService.apiUsage(resolveRole(), days == null ? 30 : days));
    }

    /** 数据源连接健康检查（GitHub / Gitee / 本地库） */
    @GetMapping("/sources/health")
    public Result<Map<String, Object>> sourcesHealth() {
        return Result.success(dashboardService.sourcesHealth());
    }

    /** 仪表盘操作日志 */
    @GetMapping("/logs")
    public Result<List<DashboardOperationLog>> logs(@RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(dashboardService.recentLogs(limit == null ? 20 : limit));
    }

    /** 导出统计报表（CSV，UTF-8 BOM），仅管理员 */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export() {
        String csv = dashboardService.exportReport(resolveRole(), SecurityUtil.getCurrentUsername());
        String filename = "技术仪表盘统计报表_" + LocalDate.now() + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, org.springframework.http.HttpStatus.OK);
    }

    /** 解析当前登录角色（ADMIN / TEACHER / STUDENT） */
    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (var ga : auth.getAuthorities()) {
                String a = String.valueOf(ga);
                if (a.contains("ADMIN")) return TechDashboardService.ROLE_ADMIN;
                if (a.contains("TEACHER")) return TechDashboardService.ROLE_TEACHER;
                if (a.contains("STUDENT")) return TechDashboardService.ROLE_STUDENT;
            }
        }
        return null;
    }
}
