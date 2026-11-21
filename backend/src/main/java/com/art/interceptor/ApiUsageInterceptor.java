package com.art.interceptor;

import com.art.entity.ApiAccessLog;
import com.art.mapper.DashboardStatsMapper;
import com.art.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * 接口访问留痕拦截器：记录 /api/** 每次真实访问的端点、调用人、角色、响应状态与服务端耗时，
 * 作为「API调用与用量」可视化的唯一数据源（非一次性统计表，随业务持续累积）。
 * 说明：留痕失败不影响主业务流程，仅记录告警日志。
  * @author liukang&&heyan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiUsageInterceptor implements HandlerInterceptor {

    /** 监控接口自身不留痕，避免自身调用污染用量统计 */
    public static final String EXCLUDE_PATH = "/api/dashboard/api-usage";

    private static final String START_ATTR = "API_USAGE_START_MS";
    /** 纯数字或长哈希段视为路径参数，归一为 {id} 以便端点聚合 */
    private static final Pattern ID_SEGMENT = Pattern.compile("^\\d+$|^[0-9a-fA-F]{8,}$");

    /** 业务分组规则：按顺序匹配路径包含关系，命中即取该分组 */
    private static final String[][] GROUP_RULES = {
            {"/ai-score", "AI动作评分"},
            {"/api/ai/", "AI模型与调用"},
            {"/ai-model", "AI模型与调用"},
            {"knowledge", "知识库管理"},
            {"/api/dashboard", "可视化仪表盘"},
            {"/api/auth", "身份认证"},
            {"/api/portal", "门户接口"},
            {"/api/message", "消息与待办"},
            {"/api/teacher/", "教师业务接口"},
            {"/api/student/", "学生业务接口"},
            {"/api/admin/", "管理业务接口"},
            {"/api/dunhuang", "敦煌数字展馆"},
            {"/api/common/", "公共业务接口"}
    };

    private final DashboardStatsMapper statsMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            String path = request.getRequestURI();
            if (path == null || EXCLUDE_PATH.equals(path)) return;

            Object start = request.getAttribute(START_ATTR);
            int duration = start == null ? 0 : (int) (System.currentTimeMillis() - (Long) start);

            ApiAccessLog record = new ApiAccessLog();
            record.setEndpoint(normalize(path));
            record.setMethod(request.getMethod());
            record.setApiGroup(groupOf(path));
            record.setUsername(SecurityUtil.getCurrentUsername());
            record.setRoleScope(currentRole());
            record.setStatus(response.getStatus());
            record.setDurationMs(duration);
            statsMapper.insertApiAccessLog(record);
        } catch (Exception e) {
            // 留痕异常隔离：不影响主业务响应
            log.warn("API访问留痕写入失败：{}", e.getMessage());
        }
    }

    /** 数字路径段归一为 {id}，保证同一接口的不同资源ID聚合为同一端点 */
    static String normalize(String path) {
        String[] segs = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (String seg : segs) {
            if (seg.isEmpty()) continue;
            sb.append('/').append(ID_SEGMENT.matcher(seg).matches() ? "{id}" : seg);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    /** 依据路径归类业务分组 */
    static String groupOf(String path) {
        for (String[] rule : GROUP_RULES) {
            if (path.contains(rule[0])) return rule[1];
        }
        return "其他接口";
    }

    /** 当前登录角色（未登录为 ANON） */
    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "ANON";
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = String.valueOf(ga);
            if (a.contains("ADMIN")) return "ADMIN";
            if (a.contains("TEACHER")) return "TEACHER";
            if (a.contains("STUDENT")) return "STUDENT";
        }
        return "ANON";
    }
}
