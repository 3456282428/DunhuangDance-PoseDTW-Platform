package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.Payment;
import com.art.service.StudentOrderService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人订单/缴费记录（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyOrdersTool implements AgentTool {

    private final StudentOrderService orderService;

    @Override
    public String name() { return "list_my_orders"; }

    @Override
    public String description() { return "查询我的缴费/订单记录（类型/金额/支付方式/状态/时间），可按状态筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"status\":{\"type\":\"string\",\"desc\":\"状态筛选：已支付/退款中/已退款，不填查全部\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        if (SecurityUtil.getCurrentStudentId() == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        String status = ToolArgs.str(args, "status");
        if (status != null && !List.of("已支付", "退款中", "已退款").contains(status)) {
            return ToolResult.fail("状态参数不合法", "可选值：已支付 / 退款中 / 已退款");
        }
        PageResult<Payment> page = orderService.pageMyOrders(1, size, status);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Payment p : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", p.getPaymentId());
            m.put("payType", p.getPayType());
            m.put("className", p.getClassName());
            m.put("amount", p.getAmount());
            m.put("payMethod", p.getPayMethod());
            m.put("status", p.getStatus());
            m.put("payDate", p.getPayDate() == null ? null : String.valueOf(p.getPayDate()));
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("没有找到符合条件的订单记录。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("共 ").append(page.getTotal()).append(" 条订单记录，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· [").append(r.get("paymentId")).append("] ").append(r.get("payType"))
                    .append(" ").append(r.get("className")).append("，").append(r.get("amount")).append(" 元（")
                    .append(r.get("payMethod")).append("，").append(r.get("status")).append("，").append(r.get("payDate")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", page.getTotal(), "rows", rows));
    }
}
