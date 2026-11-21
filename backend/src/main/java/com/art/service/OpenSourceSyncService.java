package com.art.service;

import com.art.entity.DashboardOperationLog;
import com.art.entity.OpenComponent;
import com.art.entity.OpenComponentVersion;
import com.art.mapper.DashboardOperationLogMapper;
import com.art.mapper.OpenComponentMapper;
import com.art.mapper.OpenComponentVersionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开源组件同步服务：对接 GitHub / Gitee 开放 API，
 * 实现组件信息自动拉取、最新版本追踪与更新状态监控。
 * 远端不可达时降级为 SYNC_FAILED 并保留上次已知数据（不伪造数据）。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSourceSyncService {

    private final OpenComponentMapper componentMapper;
    private final OpenComponentVersionMapper versionMapper;
    private final DashboardOperationLogMapper dashLogMapper;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** GitHub 个人访问令牌（可选：未配置时走匿名限流 60次/小时） */
    @Value("${dashboard.opensource.github-token:}")
    private String githubToken;

    /** 是否允许访问外网开源平台（离线环境可关闭） */
    @Value("${dashboard.opensource.enabled:true}")
    private boolean enabled;

    /** 单次远端请求超时（秒） */
    @Value("${dashboard.opensource.timeout-seconds:10}")
    private int timeoutSeconds;

    // ==================== 对外能力 ====================

    /**
     * 同步全部启用同步的组件（逐条容错，单条失败不影响其余）
     */
    public Map<String, Object> syncAll(String operator, String role) {
        List<OpenComponent> list = componentMapper.selectSyncEnabled();
        List<Map<String, Object>> details = new ArrayList<>();
        int success = 0, failed = 0, updatable = 0, skipped = 0;

        for (OpenComponent c : list) {
            Map<String, Object> r = syncOne(c);
            details.add(r);
            String st = String.valueOf(r.get("syncStatus"));
            switch (st) {
                case "OK" -> {
                    success++;
                    if ("UPDATE_AVAILABLE".equals(r.get("updateStatus"))) updatable++;
                }
                case "SKIPPED" -> skipped++;
                default -> failed++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("success", success);
        result.put("failed", failed);
        result.put("skipped", skipped);
        result.put("updateAvailable", updatable);
        result.put("details", details);

        writeLog("SYNC_COMPONENT", "开源组件（" + list.size() + "个）",
                "同步完成：成功" + success + " / 失败" + failed + " / 跳过" + skipped + "，发现可用更新" + updatable + "个",
                failed > 0 && success == 0 ? "FAILED" : "SUCCESS", operator, role);
        return result;
    }

    /**
     * 同步单个组件：拉取仓库概况 + 最新发布版本，回写版本追踪与更新状态
     */
    public Map<String, Object> syncOne(OpenComponent c) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("componentId", c.getComponentId());
        r.put("name", c.getName());
        r.put("platform", c.getPlatform());

        if (!enabled) {
            componentMapper.updateSyncResult(c.getComponentId(), null, null, null, null, null, null,
                    null, null, "SKIPPED", "外网开源平台访问已关闭（dashboard.opensource.enabled=false）");
            r.put("syncStatus", "SKIPPED");
            r.put("message", "外网访问已关闭");
            return r;
        }

        try {
            Map<String, Object> remote = fetchRemote(c);
            String latest = (String) remote.get("latestVersion");
            LocalDate releaseDate = (LocalDate) remote.get("latestReleaseDate");

            String updateStatus = "UNKNOWN";
            if (latest != null && !latest.isBlank()) {
                updateStatus = compareVersion(latest, c.getCurrentVersion()) > 0 ? "UPDATE_AVAILABLE" : "UP_TO_DATE";
            }

            componentMapper.updateSyncResult(c.getComponentId(), latest, releaseDate, updateStatus,
                    (Integer) remote.get("starCount"), (Integer) remote.get("forkCount"),
                    (Integer) remote.get("openIssueCount"), (String) remote.get("description"),
                    (String) remote.get("homepage"), "OK", null);

            // 版本迭代表留痕（同一版本不重复插入）
            if (latest != null && !latest.isBlank()
                    && versionMapper.countByComponentAndVersion(c.getComponentId(), latest) == 0) {
                OpenComponentVersion v = new OpenComponentVersion();
                v.setComponentId(c.getComponentId());
                v.setVersion(latest);
                v.setReleaseDate(releaseDate);
                v.setChangelog("远端 API 拉取到新版本：" + latest);
                v.setIsCurrent(0);
                v.setDetectSource("API");
                versionMapper.insert(v);
            }

            r.put("syncStatus", "OK");
            r.put("latestVersion", latest);
            r.put("currentVersion", c.getCurrentVersion());
            r.put("updateStatus", updateStatus);
            r.put("starCount", remote.get("starCount"));
            r.put("message", "同步成功");
            return r;

        } catch (Exception e) {
            String msg = shorten(e.getMessage(), 300);
            log.warn("开源组件同步失败 [{} / {}]: {}", c.getPlatform(), c.getName(), msg);
            componentMapper.updateSyncResult(c.getComponentId(), null, null, "SYNC_FAILED", null, null, null,
                    null, null, "FAILED", msg);
            r.put("syncStatus", "FAILED");
            r.put("updateStatus", "SYNC_FAILED");
            r.put("message", msg);
            return r;
        }
    }

    /**
     * 数据源连接稳定性探测：GitHub / Gitee / 本地数据库
     */
    public Map<String, Object> checkHealth() {
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(probe("GITHUB", "GitHub 开放API", "https://api.github.com/rate_limit", true));
        sources.add(probe("GITEE", "Gitee 开放API", "https://gitee.com/api/v5/repos/y_project/RuoYi", false));
        sources.add(probeLocalDb());
        sources.add(probeLocalKb());

        long okCount = sources.stream().filter(s -> "UP".equals(s.get("status"))).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", LocalDateTime.now());
        result.put("sources", sources);
        result.put("online", okCount);
        result.put("total", sources.size());
        result.put("enabled", enabled);
        return result;
    }

    // ==================== 远端数据拉取 ====================

    private Map<String, Object> fetchRemote(OpenComponent c) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        boolean github = "GITHUB".equalsIgnoreCase(c.getPlatform());
        String owner = c.getRepoOwner();
        String repo = c.getRepoName();

        String repoApi = github
                ? "https://api.github.com/repos/" + owner + "/" + repo
                : "https://gitee.com/api/v5/repos/" + owner + "/" + repo;

        // 1) 仓库概况
        JsonNode info = getJson(repoApi, github);
        if (info == null || !info.isObject()) throw new IllegalStateException("仓库概况接口无有效响应");
        out.put("starCount", github ? info.path("stargazers_count").asInt(0) : info.path("stargazers_count").asInt(0));
        out.put("forkCount", info.path("forks_count").asInt(0));
        out.put("openIssueCount", info.path("open_issues_count").asInt(0));
        String desc = info.path("description").asText("");
        if (!desc.isBlank()) out.put("description", shorten(desc, 400));
        String home = info.path("homepage").asText("");
        if (!home.isBlank()) out.put("homepage", shorten(home, 250));

        // 2) 最新发布版本
        String releaseApi = repoApi + (github ? "/releases/latest" : "/releases/latest");
        JsonNode rel = null;
        try {
            rel = getJson(releaseApi, github);
        } catch (Exception ignore) {
            // GitHub 仓库无 release 时回退到 tags 列表
            if (github) {
                try {
                    JsonNode tags = getJson(repoApi + "/tags?per_page=1", true);
                    if (tags != null && tags.isArray() && tags.size() > 0) {
                        rel = tags.get(0);
                    }
                } catch (Exception ignore2) {
                    // 保持无版本信息
                }
            }
        }

        if (rel != null && rel.isObject()) {
            String tag = rel.path("tag_name").asText("");
            if (tag.isBlank()) tag = rel.path("name").asText("");
            if (!tag.isBlank()) out.put("latestVersion", normalizeVersion(tag));
            String date = rel.path("published_at").asText("");
            if (date.isBlank()) date = rel.path("created_at").asText("");
            if (!date.isBlank() && date.length() >= 10) {
                try {
                    out.put("latestReleaseDate", LocalDate.parse(date.substring(0, 10)));
                } catch (Exception ignore) {
                    // 日期格式异常则忽略
                }
            }
        }
        return out;
    }

    private JsonNode getJson(String url, boolean github) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(3, timeoutSeconds)))
                .header("Accept", github ? "application/vnd.github+json" : "application/json")
                .header("User-Agent", "art-training-dashboard/1.0")
                .GET();
        if (github && githubToken != null && !githubToken.isBlank()) {
            b.header("Authorization", "Bearer " + githubToken.trim());
        }
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("远端返回 HTTP " + code + "（" + shorten(resp.body(), 120) + "）");
        }
        return MAPPER.readTree(resp.body());
    }

    // ==================== 健康检查 ====================

    private Map<String, Object> probe(String code, String label, String url, boolean github) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("label", label);
        m.put("url", url);
        long start = System.currentTimeMillis();
        if (!enabled) {
            m.put("status", "DISABLED");
            m.put("latencyMs", 0);
            m.put("message", "外网访问已关闭");
            return m;
        }
        try {
            getJson(url, github);
            m.put("status", "UP");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", "连接正常");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", shorten(e.getMessage(), 200));
        }
        return m;
    }

    private Map<String, Object> probeLocalDb() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", "MYSQL");
        m.put("label", "本地数据库 art_training");
        m.put("url", "jdbc:mysql://localhost/art_training");
        long start = System.currentTimeMillis();
        try {
            int n = componentMapper.countAll();
            m.put("status", "UP");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", "组件登记表可访问（" + n + " 条）");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", shorten(e.getMessage(), 200));
        }
        return m;
    }

    private Map<String, Object> probeLocalKb() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", "KB_INGEST");
        m.put("label", "知识库归集通道");
        m.put("url", "/uploads/kb_backup");
        long start = System.currentTimeMillis();
        try {
            int n = versionMapper.countAll();
            m.put("status", "UP");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", "版本迭代表可访问（" + n + " 条）");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", shorten(e.getMessage(), 200));
        }
        return m;
    }

    // ==================== 工具方法 ====================

    /**
     * 版本号比较：忽略前缀 v，按数字段逐段比较，非数字段退化为字符串比较。
     * @return >0 表示 latest 更新
     */
    public static int compareVersion(String latest, String current) {
        if (latest == null || latest.isBlank()) return 0;
        if (current == null || current.isBlank()) return 1;
        String[] a = normalizeVersion(latest).split("[.\\-_]");
        String[] b = normalizeVersion(current).split("[.\\-_]");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            String x = i < a.length ? a[i] : "0";
            String y = i < b.length ? b[i] : "0";
            int cmp;
            if (isNumeric(x) && isNumeric(y)) {
                cmp = Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
            } else {
                cmp = x.compareToIgnoreCase(y);
            }
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static String normalizeVersion(String v) {
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int idx = s.indexOf('+');            // 去掉构建元数据
        if (idx > 0) s = s.substring(0, idx);
        return s;
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    private static String shorten(String s, int max) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private void writeLog(String type, String target, String detail, String result, String operator, String role) {
        try {
            DashboardOperationLog log = new DashboardOperationLog();
            log.setOpType(type);
            log.setOpTarget(shorten(target, 200));
            log.setOpDetail(shorten(detail, 900));
            log.setOpResult(result);
            log.setOpUsername(operator);
            log.setOpRole(role);
            dashLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("仪表盘操作日志写入失败: {}", e.getMessage());
        }
    }
}
