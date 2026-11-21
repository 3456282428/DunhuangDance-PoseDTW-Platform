package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 工具：营收统计（管理端订单与财务）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class IncomeStatTool implements AgentTool {

    private static final Set<String> DIMENSIONS = Set.of("month", "pay_method", "biz_type");

    private final FinanceService financeService;

    @Override
    public String name() { return "income_stat"; }

    @Override
    public String description() { return "统计机构营收收入，支持按月份、支付方式、业务类型三种维度汇总"; }

    @Override
    public String parametersSchema() {
        return "{\"dimension\":{\"type\":\"string\",\"desc\":\"统计维度：month按月/pay_method按支付方式/biz_type按业务类型，默认month\",\"required\":false}}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        String dim = ToolArgs.str(args, "dimension", "month");
        if (!DIMENSIONS.contains(dim)) {
            return ToolResult.fail("不支持的统计维度 " + dim, "可选维度：month（按月）、pay_method（按支付方式）、biz_type（按业务类型）");
        }
        Map<String, Object> stat = financeService.incomeStat(dim);
        return ToolResult.ok("营收统计（维度 " + dim + "）：" + stat, stat);
    }
}
