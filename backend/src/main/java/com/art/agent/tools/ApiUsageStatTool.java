package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.mapper.DashboardStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工具：系统 API 调用与用量统计（管理端可视化仪表盘数据源）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ApiUsageStatTool implements AgentTool {

    private final DashboardStatsMapper statsMapper;

    @Override
    public String name() { return "api_usage_stat"; }

    @Override
    public String description() { return "统计系统接口调用量/平均耗时/成功率/热门端点，回答系统运行情况类问题"; }

    @Override
    public String parametersSchema() {
        return "{\"days\":{\"type\":\"int\",\"desc\":\"统计最近N天，默认7，最大30\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        int days = ToolArgs.limit(args, "days", 7, 30);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Object> summary = statsMapper.selectApiUsageSummary(since);
        List<Map<String, Object>> top = statsMapper.selectApiTopEndpoints(since, 5);
        StringBuilder sb = new StringBuilder("最近 ").append(days).append(" 天 API 用量：").append(summary);
        sb.append("\n调用量Top端点：");
        for (Map<String, Object> t : top) sb.append("\n· ").append(t);
        return ToolResult.ok(sb.toString(), Map.of("summary", summary, "topEndpoints", top));
    }
}
