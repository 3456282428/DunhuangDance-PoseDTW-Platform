package com.art.service;

import com.art.common.BusinessException;
import com.art.entity.AiModel;
import com.art.entity.AiModelKbBinding;
import com.art.entity.AiModelMetric;
import com.art.entity.AiModelVersion;
import com.art.entity.DashboardOperationLog;
import com.art.entity.DashboardTopology;
import com.art.entity.OpenComponent;
import com.art.entity.OpenComponentVersion;
import com.art.mapper.AiModelKbBindingMapper;
import com.art.mapper.AiModelMapper;
import com.art.mapper.AiModelVersionMapper;
import com.art.mapper.DashboardOperationLogMapper;
import com.art.mapper.DashboardStatsMapper;
import com.art.mapper.DashboardTopologyMapper;
import com.art.mapper.OpenComponentMapper;
import com.art.mapper.OpenComponentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 技术可视化仪表盘聚合服务：
 * 知识库存储统计 / 文档数量趋势 / AI模型与版本迭代 / 模型性能指标 / 组件依赖拓扑 / 数据源健康。
 * 内置角色权限过滤：管理员=全量；教师=教师端知识库+只读组件模型；学员=仅本端知识库。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechDashboardService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    /** 知识库端别 -> 物理表（白名单，防止 SQL 注入） */
    private static final Map<String, String> KB_TABLES = Map.of(
            "ADMIN", "kb_admin_doc",
            "TEACHER", "kb_teacher_doc",
            "STUDENT", "kb_student_doc");

    private static final Map<String, String> SCOPE_LABEL = Map.of(
            "ADMIN", "管理员端知识库",
            "TEACHER", "教师端知识库",
            "STUDENT", "学生端知识库");

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DashboardStatsMapper statsMapper;
    private final OpenComponentMapper componentMapper;
    private final OpenComponentVersionMapper componentVersionMapper;
    private final AiModelMapper aiModelMapper;
    private final AiModelVersionMapper aiModelVersionMapper;
    private final AiModelKbBindingMapper bindingMapper;
    private final DashboardTopologyMapper topologyMapper;
    private final DashboardOperationLogMapper dashLogMapper;
    private final AiModelMetricService metricService;
    private final OpenSourceSyncService syncService;

    // ==================== 权限模型 ====================

    /** 当前角色可查看的知识库范围 */
    public List<String> allowedScopes(String role) {
        return switch (role == null ? "" : role) {
            case ROLE_ADMIN -> List.of("ADMIN", "TEACHER", "STUDENT");
            case ROLE_TEACHER -> List.of("TEACHER");
            case ROLE_STUDENT -> List.of("STUDENT");
            default -> List.of();
        };
    }

    /** 角色权限声明（前端据此控制模块显隐与按钮可用性） */
    public Map<String, Object> permissions(String role) {
        boolean admin = ROLE_ADMIN.equals(role);
        boolean student = ROLE_STUDENT.equals(role);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("role", role);
        p.put("roleLabel", admin ? "系统管理员" : student ? "学员" : "教师");
        p.put("allowedScopes", allowedScopes(role));
        p.put("canViewAllScopes", admin);
        p.put("canViewComponents", !student);
        p.put("canViewModels", !student);
        p.put("canViewTopology", !student);
        p.put("canViewLogs", admin);
        p.put("canSyncComponent", admin);
        p.put("canUpgradeModel", admin);
        p.put("canExportReport", admin);
        p.put("dataScopeNote", admin
                ? "管理员可见全平台知识库、开源组件、AI模型与操作日志"
                : student
                ? "学员仅可查看本人学习知识库的存储与增长数据"
                : "教师可查看教师端知识库数据，开源组件与AI模型为只读视图");
        return p;
    }

    private void requireRole(String role, String feature) {
        if (!ROLE_ADMIN.equals(role)) {
            throw new BusinessException("当前角色无「" + feature + "」操作权限，仅系统管理员可执行");
        }
    }

    // ==================== 概览聚合 ====================

    /**
     * 仪表盘总览数据（按角色过滤）
     * @param days 文档趋势天数（默认30）
     */
    public Map<String, Object> overview(String role, int days) {
        int trendDays = Math.min(Math.max(days, 7), 90);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", LocalDateTime.now().format(TS));
        out.put("permissions", permissions(role));

        List<String> scopes = allowedScopes(role);
        out.put("kbStorage", buildKbStorage(scopes));
        out.put("docTrend", buildDocTrend(scopes, trendDays));
        // 贡献热力图：近一年日粒度知识条目新增序列（GitHub 风格日历热力图数据源）
        out.put("kbActivity", buildDocTrend(scopes, 365));

        boolean canViewModels = !ROLE_STUDENT.equals(role) || false;
        out.put("models", canViewModels ? buildModels(role) : List.of());
        out.put("components", canViewComponents(role) ? buildComponents(role, null, null, null, null) : List.of());
        out.put("componentSummary", canViewComponents(role) ? componentSummary() : null);
        out.put("topology", canViewTopology(role) ? buildTopology(role) : null);
        out.put("logs", ROLE_ADMIN.equals(role) ? recentLogs(12) : List.of());
        return out;
    }

    private boolean canViewComponents(String role) {
        return !ROLE_STUDENT.equals(role);
    }

    private boolean canViewTopology(String role) {
        return !ROLE_STUDENT.equals(role);
    }

    // ==================== 1. 知识库存储使用量 ====================

    private Map<String, Object> buildKbStorage(List<String> scopes) {
        List<Map<String, Object>> items = new ArrayList<>();
        long totalChars = 0, totalDocs = 0, totalPhysical = 0;

        for (String scope : scopes) {
            String table = KB_TABLES.get(scope);
            Map<String, Object> stat = statsMapper.selectDocStat(table);
            long contentChars = toLong(stat == null ? null : stat.get("contentChars"));
            long docs = toLong(stat == null ? null : stat.get("total"));
            long physical = 0;
            List<Map<String, Object>> st = statsMapper.selectTableStorage(List.of(table));
            if (st != null && !st.isEmpty()) physical = toLong(st.get(0).get("totalBytes"));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scope", scope);
            item.put("label", SCOPE_LABEL.get(scope));
            item.put("table", table);
            item.put("docCount", docs);
            item.put("contentChars", contentChars);
            item.put("physicalBytes", physical);
            item.put("autoCount", toLong(stat == null ? null : stat.get("autoCount")));
            item.put("manualCount", toLong(stat == null ? null : stat.get("manualCount")));
            item.put("activeCount", toLong(stat == null ? null : stat.get("activeCount")));
            item.put("fileCount", toLong(stat == null ? null : stat.get("fileCount")));
            item.put("viewTotal", toLong(stat == null ? null : stat.get("viewTotal")));
            items.add(item);

            totalChars += contentChars;
            totalDocs += docs;
            totalPhysical += physical;
        }

        // 内容量占比（供环形图）
        for (Map<String, Object> item : items) {
            long c = (long) item.get("contentChars");
            double pct = totalChars == 0 ? 0 : Math.round(c * 10000.0 / totalChars) / 100.0;
            item.put("contentSharePercent", pct);
            item.put("physicalReadable", readableSize((long) item.get("physicalBytes")));
        }

        Map<String, Object> schema = statsMapper.selectSchemaStorage("art_training");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scopes", items);
        out.put("totalDocs", totalDocs);
        out.put("totalContentChars", totalChars);
        out.put("totalPhysicalBytes", totalPhysical);
        out.put("totalPhysicalReadable", readableSize(totalPhysical));
        out.put("schemaBytes", toLong(schema == null ? null : schema.get("totalBytes")));
        out.put("schemaReadable", readableSize(toLong(schema == null ? null : schema.get("totalBytes"))));
        out.put("schemaTableCount", toLong(schema == null ? null : schema.get("tableCount")));
        return out;
    }

    // ==================== 2. 文档数量趋势 ====================

    private Map<String, Object> buildDocTrend(List<String> scopes, int days) {
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) dates.add(today.minusDays(i).format(DAY));

        List<Map<String, Object>> series = new ArrayList<>();
        long[] total = new long[days];
        Map<String, List<Map<String, Object>>> sourceFilter = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> categoryFilter = new LinkedHashMap<>();

        for (String scope : scopes) {
            String table = KB_TABLES.get(scope);
            Map<String, Long> dayMap = new LinkedHashMap<>();
            List<Map<String, Object>> rows = statsMapper.selectDailyTrend(table, days);
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    dayMap.put(String.valueOf(r.get("statDate")), toLong(r.get("docCount")));
                }
            }
            List<Long> data = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                long v = dayMap.getOrDefault(dates.get(i), 0L);
                data.add(v);
                total[i] += v;
            }
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("scope", scope);
            s.put("label", SCOPE_LABEL.get(scope));
            s.put("data", data);
            series.add(s);

            sourceFilter.put(scope, statsMapper.selectSourceTypeStat(table));
            categoryFilter.put(scope, statsMapper.selectCategoryStat(table));
        }

        List<Long> totalList = new ArrayList<>();
        for (long v : total) totalList.add(v);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("dates", dates);
        out.put("series", series);
        out.put("totalSeries", totalList);
        out.put("sourceTypeFilters", sourceFilter);
        out.put("categoryFilters", categoryFilter);
        out.put("granularity", "day");
        return out;
    }

    // ==================== 3. AI模型与版本迭代 ====================

    /**
     * 可见模型列表（教师仅可见绑定到 TEACHER/ALL 的模型）
     */
    private List<Map<String, Object>> buildModels(String role) {
        List<AiModel> models = aiModelMapper.selectAll();
        Set<Integer> visible = null;
        if (ROLE_TEACHER.equals(role)) {
            visible = bindingMapper.selectAll().stream()
                    .filter(b -> "ALL".equals(b.getKbScope()) || "TEACHER".equals(b.getKbScope()))
                    .filter(b -> b.getEnabled() != null && b.getEnabled() == 1)
                    .map(AiModelKbBinding::getModelId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (visible.isEmpty()) visible = models.stream().map(AiModel::getModelId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (AiModel m : models) {
            if (visible != null && !visible.contains(m.getModelId())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelId", m.getModelId());
            item.put("modelName", m.getModelName());
            item.put("modelType", m.getModelType());
            item.put("provider", m.getProvider());
            item.put("status", m.getStatus());
            item.put("description", m.getDescription());
            item.put("currentVersion", m.getCurrentVersion());
            item.put("latestVersion", m.getLatestVersion());
            item.put("updateStatus", m.getUpdateStatus());
            item.put("lastUpgradeTime", m.getLastUpgradeTime() == null ? null : m.getLastUpgradeTime().format(TS));
            item.put("versions", aiModelVersionMapper.selectByModel(m.getModelId()));
            item.put("bindings", bindingMapper.selectEnabledByModel(m.getModelId()));
            item.put("metricSummary", metricService.summary(m.getModelId()));
            list.add(item);
        }
        return list;
    }

    /** 模型性能指标序列（实时曲线） */
    public Map<String, Object> modelMetrics(Integer modelId, int limit) {
        AiModel aiModel = aiModelMapper.selectById(modelId);
        if (aiModel == null) throw new BusinessException("AI模型不存在");
        List<AiModelMetric> list = metricService.recent(modelId, limit);
        List<String> times = new ArrayList<>();
        List<BigDecimal> latency = new ArrayList<>();
        List<BigDecimal> accuracy = new ArrayList<>();
        List<BigDecimal> success = new ArrayList<>();
        List<BigDecimal> throughput = new ArrayList<>();
        List<Integer> calls = new ArrayList<>();
        List<BigDecimal> kbHit = new ArrayList<>();
        for (AiModelMetric m : list) {
            times.add(m.getMetricTime() == null ? "" : m.getMetricTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            latency.add(m.getLatencyMs());
            accuracy.add(m.getAccuracy());
            success.add(m.getSuccessRate());
            throughput.add(m.getThroughputQps());
            calls.add(m.getCallCount());
            kbHit.add(m.getKbHitAvg());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("modelId", modelId);
        out.put("modelName", aiModel.getModelName());
        out.put("times", times);
        out.put("latencyMs", latency);
        out.put("accuracy", accuracy);
        out.put("successRate", success);
        out.put("throughput", throughput);
        out.put("callCount", calls);
        out.put("kbHitAvg", kbHit);
        out.put("summary", metricService.summary(modelId));
        return out;
    }

    /**
     * 触发模型更新（AI能力尚未接入 → 仅登记升级意向并留痕，不执行真实部署）
     */
    public Map<String, Object> upgradeModel(Integer modelId, String targetVersion, String operator, String role) {
        requireRole(role, "触发模型更新");
        AiModel m = aiModelMapper.selectById(modelId);
        if (m == null) throw new BusinessException("AI模型不存在");
        String target = (targetVersion == null || targetVersion.isBlank())
                ? (m.getLatestVersion() != null ? m.getLatestVersion() : "1.1.0")
                : targetVersion.trim();
        if (target.equals(m.getCurrentVersion())) {
            throw new BusinessException("目标版本与当前运行版本一致，无需升级");
        }

        aiModelVersionMapper.deletePendingByModel(modelId);
        AiModelVersion v = new AiModelVersion();
        v.setModelId(modelId);
        v.setVersion(target);
        v.setReleaseDate(LocalDate.now());
        v.setChangelog("管理员通过可视化仪表盘登记升级意向（等待AI模型接入后执行）");
        v.setIsCurrent(0);
        v.setUpgradeStatus("PENDING");
        aiModelVersionMapper.insert(v);
        aiModelMapper.updateVersionInfo(modelId, null, target, "UPDATE_AVAILABLE");

        writeLog("UPGRADE_MODEL", m.getModelName() + " → " + target,
                "登记模型升级意向（当前实现为大模型未接入阶段，仅登记不执行部署）", "PENDING", operator, role);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("modelId", modelId);
        out.put("modelName", m.getModelName());
        out.put("targetVersion", target);
        out.put("currentVersion", m.getCurrentVersion());
        out.put("status", "PENDING_AI_INTEGRATION");
        out.put("message", "升级意向已登记并留痕。当前阶段AI模型业务逻辑尚未接入，接口与参数规范已预留，接入后可直接执行版本切换。");
        return out;
    }

    // ==================== 4. 开源组件（GitHub / Gitee） ====================

    private Map<String, Object> componentSummary() {
        int total = componentMapper.countAll();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("github", componentMapper.countByPlatform("GITHUB"));
        out.put("gitee", componentMapper.countByPlatform("GITEE"));
        out.put("upToDate", componentMapper.countByUpdateStatus("UP_TO_DATE"));
        out.put("updateAvailable", componentMapper.countByUpdateStatus("UPDATE_AVAILABLE"));
        out.put("syncFailed", componentMapper.countByUpdateStatus("SYNC_FAILED"));
        out.put("unknown", componentMapper.countByUpdateStatus("UNKNOWN"));
        out.put("versionRecordCount", componentVersionMapper.countAll());
        return out;
    }

    /** 组件列表（按角色只读开放；筛选参数：platform/category/updateStatus/keyword） */
    public List<Map<String, Object>> buildComponents(String role, String platform, String category,
                                                     String updateStatus, String keyword) {
        if (!canViewComponents(role)) {
            throw new BusinessException("当前角色无开源组件查看权限");
        }
        List<OpenComponent> list = componentMapper.selectList(platform, category, updateStatus, keyword);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OpenComponent c : list) {
            out.add(toComponentVO(c, false));
        }
        return out;
    }

    /** 组件详情：基础信息 + 版本迭代历史 + 依赖上下游 */
    public Map<String, Object> componentDetail(Integer componentId, String role) {
        if (!canViewComponents(role)) {
            throw new BusinessException("当前角色无开源组件查看权限");
        }
        OpenComponent c = componentMapper.selectById(componentId);
        if (c == null) throw new BusinessException("开源组件不存在");
        Map<String, Object> vo = toComponentVO(c, true);
        List<OpenComponentVersion> versions = componentVersionMapper.selectByComponent(componentId);
        vo.put("versions", versions);

        String selfId = nodeId("COMPONENT", c.getName());
        List<DashboardTopology> edges = topologyMapper.selectAll();
        List<Map<String, Object>> upstream = new ArrayList<>();   // 本组件依赖的对象
        List<Map<String, Object>> downstream = new ArrayList<>(); // 依赖本组件的对象
        for (DashboardTopology e : edges) {
            String sid = nodeId(e.getSourceType(), e.getSourceKey());
            String tid = nodeId(e.getTargetType(), e.getTargetKey());
            if (sid.equals(selfId)) upstream.add(edgeVO(e, e.getTargetType(), e.getTargetKey()));
            if (tid.equals(selfId)) downstream.add(edgeVO(e, e.getSourceType(), e.getSourceKey()));
        }
        vo.put("dependsOn", upstream);
        vo.put("dependedBy", downstream);
        return vo;
    }

    private Map<String, Object> toComponentVO(OpenComponent c, boolean detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("componentId", c.getComponentId());
        m.put("name", c.getName());
        m.put("platform", c.getPlatform());
        m.put("repoOwner", c.getRepoOwner());
        m.put("repoName", c.getRepoName());
        m.put("repoUrl", c.getRepoUrl());
        m.put("category", c.getCategory());
        m.put("language", c.getLanguage());
        m.put("license", c.getLicense());
        m.put("usedByModule", c.getUsedByModule());
        m.put("currentVersion", c.getCurrentVersion());
        m.put("latestVersion", c.getLatestVersion());
        m.put("latestReleaseDate", c.getLatestReleaseDate() == null ? null : c.getLatestReleaseDate().toString());
        m.put("updateStatus", c.getUpdateStatus());
        m.put("starCount", c.getStarCount());
        m.put("forkCount", c.getForkCount());
        m.put("openIssueCount", c.getOpenIssueCount());
        m.put("description", c.getDescription());
        m.put("homepage", c.getHomepage());
        m.put("syncEnabled", c.getSyncEnabled());
        m.put("lastSyncStatus", c.getLastSyncStatus());
        m.put("lastSyncMessage", c.getLastSyncMessage());
        m.put("lastSyncTime", c.getLastSyncTime() == null ? null : c.getLastSyncTime().format(TS));
        if (detail) {
            m.put("versionGap", OpenSourceSyncService.compareVersion(c.getLatestVersion(), c.getCurrentVersion()));
            m.put("updateAvailable", OpenSourceSyncService.compareVersion(c.getLatestVersion(), c.getCurrentVersion()) > 0);
        }
        return m;
    }

    /** 触发组件同步 */
    public Map<String, Object> syncComponents(String role, String operator) {
        requireRole(role, "同步开源组件");
        return syncService.syncAll(operator, role);
    }

    /** 数据源连接健康检查 */
    public Map<String, Object> sourcesHealth() {
        return syncService.checkHealth();
    }

    // ==================== 6. API 调用与用量（api_access_log 真实访问留痕） ====================

    /**
     * API 调用与用量聚合：日流量趋势 / 星期×小时调用分布 / 端点调用排行 / 业务分组与角色占比 / 汇总指标。
     * 数据随业务访问持续累积，非静态统计表；仅系统管理员可查看。
     */
    public Map<String, Object> apiUsage(String role, int days) {
        requireRole(role, "查看API调用与用量");
        int d = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(d - 1L).atStartOfDay();

        Map<String, Object> row = statsMapper.selectApiUsageSummary(since);
        long total = toLong(row == null ? null : row.get("totalCalls"));
        long okCalls = toLong(row == null ? null : row.get("okCalls"));

        // ---- 日流量趋势（缺失日期补零） ----
        List<String> dates = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) dates.add(today.minusDays(i).format(DAY));
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> r : statsMapper.selectApiDailyTraffic(since)) {
            byDay.put(String.valueOf(r.get("statDate")), r);
        }
        List<Long> calls = new ArrayList<>();
        List<BigDecimal> avgDuration = new ArrayList<>();
        List<Long> failCalls = new ArrayList<>();
        for (String day : dates) {
            Map<String, Object> r = byDay.get(day);
            calls.add(toLong(r == null ? null : r.get("calls")));
            avgDuration.add(toDecimal(r == null ? null : r.get("avgDurationMs")));
            failCalls.add(toLong(r == null ? null : r.get("failCalls")));
        }

        // ---- 星期×小时调用分布（行=周一..周日，列=0..23时） ----
        List<List<Long>> punch = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            List<Long> line = new ArrayList<>();
            for (int h = 0; h < 24; h++) line.add(0L);
            punch.add(line);
        }
        long punchMax = 0L;
        int peakWeekday = -1, peakHour = -1;
        for (Map<String, Object> r : statsMapper.selectApiPunchCard(since)) {
            int wd = (int) toLong(r.get("weekday"));
            int hr = (int) toLong(r.get("hourOfDay"));
            long c = toLong(r.get("calls"));
            if (wd < 0 || wd > 6 || hr < 0 || hr > 23) continue;
            punch.get(wd).set(hr, c);
            if (c > punchMax) {
                punchMax = c;
                peakWeekday = wd;
                peakHour = hr;
            }
        }

        // ---- 端点调用排行（次数 + 平均耗时 + 失败数） ----
        List<Map<String, Object>> topEndpoints = new ArrayList<>();
        for (Map<String, Object> r : statsMapper.selectApiTopEndpoints(since, 8)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("endpoint", r.get("endpoint"));
            item.put("method", r.get("method"));
            item.put("apiGroup", r.get("apiGroup"));
            item.put("calls", toLong(r.get("calls")));
            item.put("avgDurationMs", toDecimal(r.get("avgDurationMs")));
            item.put("failCalls", toLong(r.get("failCalls")));
            topEndpoints.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", d);
        out.put("since", since.format(TS));
        out.put("generatedAt", LocalDateTime.now().format(TS));
        out.put("dates", dates);
        out.put("calls", calls);
        out.put("avgDuration", avgDuration);
        out.put("failCalls", failCalls);
        out.put("punchCard", punch);
        out.put("punchMax", punchMax);
        out.put("peakWeekday", peakWeekday);
        out.put("peakHour", peakHour);
        out.put("topEndpoints", topEndpoints);
        out.put("groupShare", withPercent(statsMapper.selectApiGroupShare(since), total));
        out.put("roleShare", withPercent(statsMapper.selectApiRoleShare(since), total));
        out.put("summary", buildApiSummary(row, okCalls, total, peakWeekday, peakHour));

        // ---- AI 统一入口调用的知识库检索明细（ai_invoke_log 真实留痕） ----
        Map<String, Object> inv = statsMapper.selectInvokeAgg(since);
        long invTotal = toLong(inv == null ? null : inv.get("callCount"));
        Map<String, Object> aiInvoke = new LinkedHashMap<>();
        aiInvoke.put("total", invTotal);
        aiInvoke.put("hitCalls", toLong(inv == null ? null : inv.get("hitCalls")));
        aiInvoke.put("avgHit", toDecimal(inv == null ? null : inv.get("kbHitAvg")));
        aiInvoke.put("hitRate", invTotal == 0 ? 0.0
                : Math.round(toLong(inv == null ? null : inv.get("hitCalls")) * 10000.0 / invTotal) / 100.0);
        aiInvoke.put("scopeShare", withPercent(statsMapper.selectInvokeScopeStat(since), invTotal));
        out.put("aiInvoke", aiInvoke);
        return out;
    }

    private Map<String, Object> buildApiSummary(Map<String, Object> row, long okCalls, long total,
                                                int peakWeekday, int peakHour) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalCalls", total);
        s.put("todayCalls", toLong(row == null ? null : row.get("todayCalls")));
        s.put("avgDurationMs", toDecimal(row == null ? null : row.get("avgDurationMs")));
        s.put("okCalls", okCalls);
        s.put("failCalls", toLong(row == null ? null : row.get("failCalls")));
        s.put("successRate", total == 0 ? 0.0 : Math.round(okCalls * 10000.0 / total) / 100.0);
        s.put("endpointCount", toLong(row == null ? null : row.get("endpointCount")));
        s.put("userCount", toLong(row == null ? null : row.get("userCount")));
        s.put("peakLabel", peakWeekday < 0 ? "—"
                : "周" + "一二三四五六日".charAt(peakWeekday) + " " + String.format("%02d:00", peakHour));
        return s;
    }

    /** 为分组统计补充百分比（GitHub 语言条渲染需要） */
    private List<Map<String, Object>> withPercent(List<Map<String, Object>> rows, long total) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) return out;
        for (Map<String, Object> r : rows) {
            long v = toLong(r.get("value"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", r.get("name"));
            item.put("value", v);
            item.put("percent", total == 0 ? 0.0 : Math.round(v * 10000.0 / total) / 100.0);
            out.add(item);
        }
        return out;
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ==================== 5. 组件依赖拓扑图 ====================

    private Map<String, Object> buildTopology(String role) {
        List<String> scopes = allowedScopes(role);
        List<OpenComponent> components = componentMapper.selectList(null, null, null, null);
        List<AiModel> models = ROLE_STUDENT.equals(role) ? List.of() : aiModelMapper.selectAll();

        Set<String> allowedIds = new LinkedHashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();

        for (OpenComponent c : components) {
            String id = nodeId("COMPONENT", c.getName());
            allowedIds.add(id);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id);
            n.put("name", c.getName());
            n.put("nodeType", "COMPONENT");
            n.put("platform", c.getPlatform());
            n.put("category", c.getCategory());
            n.put("version", c.getCurrentVersion());
            n.put("updateStatus", c.getUpdateStatus());
            n.put("metricValue", c.getStarCount() == null ? 0 : c.getStarCount());
            n.put("metricLabel", "Stars " + (c.getStarCount() == null ? 0 : c.getStarCount()));
            n.put("symbolSize", symbolSizeFor(c.getStarCount()));
            nodes.add(n);
        }
        for (AiModel m : models) {
            String id = nodeId("AI_MODEL", m.getModelName());
            allowedIds.add(id);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id);
            n.put("name", m.getModelName());
            n.put("nodeType", "AI_MODEL");
            n.put("category", m.getModelType());
            n.put("version", m.getCurrentVersion());
            n.put("updateStatus", m.getUpdateStatus());
            n.put("metricValue", 1);
            n.put("metricLabel", "AI模型");
            n.put("symbolSize", 46);
            nodes.add(n);
        }
        for (String scope : scopes) {
            Map<String, Object> stat = statsMapper.selectDocStat(KB_TABLES.get(scope));
            long docs = toLong(stat == null ? null : stat.get("total"));
            String id = nodeId("KB_SCOPE", scope);
            allowedIds.add(id);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id);
            n.put("name", SCOPE_LABEL.get(scope));
            n.put("nodeType", "KB_SCOPE");
            n.put("category", scope);
            n.put("version", docs + " 条");
            n.put("updateStatus", null);
            n.put("metricValue", docs);
            n.put("metricLabel", docs + " 条知识");
            n.put("symbolSize", 34 + Math.min(26, docs * 2));
            nodes.add(n);
        }

        List<Map<String, Object>> links = new ArrayList<>();
        for (DashboardTopology e : topologyMapper.selectAll()) {
            String sid = nodeId(e.getSourceType(), e.getSourceKey());
            String tid = nodeId(e.getTargetType(), e.getTargetKey());
            if (!allowedIds.contains(sid) || !allowedIds.contains(tid)) continue;
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("source", sid);
            l.put("target", tid);
            l.put("relation", e.getRelation());
            l.put("depType", e.getDepType());
            l.put("versionConstraint", e.getVersionConstraint());
            l.put("remark", e.getRemark());
            links.add(l);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodes", nodes);
        out.put("links", links);
        out.put("nodeCount", nodes.size());
        out.put("linkCount", links.size());
        out.put("categories", List.of(
                Map.of("name", "开源组件"),
                Map.of("name", "AI模型"),
                Map.of("name", "知识库")));
        return out;
    }

    private Map<String, Object> edgeVO(DashboardTopology e, String otherType, String otherKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodeType", otherType);
        m.put("key", otherKey);
        m.put("relation", e.getRelation());
        m.put("depType", e.getDepType());
        m.put("versionConstraint", e.getVersionConstraint());
        m.put("remark", e.getRemark());
        return m;
    }

    private static int symbolSizeFor(Integer stars) {
        int s = stars == null ? 0 : stars;
        // 按 Star 量对数缩放，保证视觉可辨识
        return (int) Math.round(24 + Math.min(26, Math.log10(Math.max(s, 1)) * 6));
    }

    private static String nodeId(String type, String key) {
        return type + ":" + key;
    }

    // ==================== 6. 操作日志 / 报表导出 ====================

    public List<DashboardOperationLog> recentLogs(int limit) {
        return dashLogMapper.selectRecent(Math.min(Math.max(limit, 1), 100));
    }

    /**
     * 导出统计报表（CSV，含 UTF-8 BOM 便于 Excel 直接打开）
     */
    public String exportReport(String role, String operator) {
        requireRole(role, "导出统计报表");
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');

        // 一、知识库存储统计
        Map<String, Object> storage = buildKbStorage(allowedScopes(role));
        sb.append("知识库存储使用量统计\n");
        sb.append("端别,物理表,条目数,自动归集,手动维护,启用条目,附件数,内容字符数,内容占比(%),表物理占用,浏览总量\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scopes = (List<Map<String, Object>>) storage.get("scopes");
        for (Map<String, Object> s : scopes) {
            sb.append(csv(s.get("label"))).append(',')
                    .append(csv(s.get("table"))).append(',')
                    .append(s.get("docCount")).append(',')
                    .append(s.get("autoCount")).append(',')
                    .append(s.get("manualCount")).append(',')
                    .append(s.get("activeCount")).append(',')
                    .append(s.get("fileCount")).append(',')
                    .append(s.get("contentChars")).append(',')
                    .append(s.get("contentSharePercent")).append(',')
                    .append(csv(s.get("physicalReadable"))).append(',')
                    .append(s.get("viewTotal")).append('\n');
        }
        sb.append("合计文档数,").append(storage.get("totalDocs"))
                .append(",内容字符数,").append(storage.get("totalContentChars"))
                .append(",表物理占用,").append(storage.get("totalPhysicalReadable"))
                .append(",数据库总占用,").append(storage.get("schemaReadable")).append('\n');

        // 二、文档数量趋势
        Map<String, Object> trend = buildDocTrend(allowedScopes(role), 30);
        sb.append("\n文档数量趋势（近30天，按日新增）\n");
        sb.append("日期");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) trend.get("series");
        for (Map<String, Object> s : series) sb.append(',').append(csv(s.get("label")));
        sb.append(",合计\n");
        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) trend.get("dates");
        @SuppressWarnings("unchecked")
        List<Long> totals = (List<Long>) trend.get("totalSeries");
        for (int i = 0; i < dates.size(); i++) {
            sb.append(dates.get(i));
            for (Map<String, Object> s : series) {
                @SuppressWarnings("unchecked")
                List<Long> data = (List<Long>) s.get("data");
                sb.append(',').append(data.get(i));
            }
            sb.append(',').append(totals.get(i)).append('\n');
        }

        // 三、开源组件与更新状态
        sb.append("\n开源组件版本追踪\n");
        sb.append("组件名称,来源平台,用途分类,当前版本,最新版本,更新状态,Star数,所属模块,仓库地址\n");
        for (Map<String, Object> c : buildComponents(role, null, null, null, null)) {
            sb.append(csv(c.get("name"))).append(',')
                    .append(csv(c.get("platform"))).append(',')
                    .append(csv(c.get("category"))).append(',')
                    .append(csv(c.get("currentVersion"))).append(',')
                    .append(csv(c.get("latestVersion"))).append(',')
                    .append(csv(c.get("updateStatus"))).append(',')
                    .append(c.get("starCount")).append(',')
                    .append(csv(c.get("usedByModule"))).append(',')
                    .append(csv(c.get("repoUrl"))).append('\n');
        }

        // 四、AI模型与版本迭代
        sb.append("\nAI模型与版本迭代\n");
        sb.append("模型名称,类型,提供方,运行状态,当前版本,可升级版本,更新状态,版本记录数\n");
        for (Map<String, Object> m : buildModels(role)) {
            @SuppressWarnings("unchecked")
            List<AiModelVersion> vs = (List<AiModelVersion>) m.get("versions");
            sb.append(csv(m.get("modelName"))).append(',')
                    .append(csv(m.get("modelType"))).append(',')
                    .append(csv(m.get("provider"))).append(',')
                    .append(csv(m.get("status"))).append(',')
                    .append(csv(m.get("currentVersion"))).append(',')
                    .append(csv(m.get("latestVersion"))).append(',')
                    .append(csv(m.get("updateStatus"))).append(',')
                    .append(vs == null ? 0 : vs.size()).append('\n');
        }

        sb.append("\n导出时间,").append(LocalDateTime.now().format(TS));
        sb.append(",导出人,").append(csv(operator)).append('\n');

        writeLog("EXPORT_REPORT", "统计报表（含三端知识库/趋势/组件/模型）",
                "导出CSV统计报表", "SUCCESS", operator, role);
        return sb.toString();
    }

    private void writeLog(String type, String target, String detail, String result, String operator, String role) {
        try {
            DashboardOperationLog log = new DashboardOperationLog();
            log.setOpType(type);
            log.setOpTarget(trunc(target, 200));
            log.setOpDetail(trunc(detail, 900));
            log.setOpResult(result);
            log.setOpUsername(operator);
            log.setOpRole(role);
            dashLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("仪表盘操作日志写入失败: {}", e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private static String csv(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }
}
