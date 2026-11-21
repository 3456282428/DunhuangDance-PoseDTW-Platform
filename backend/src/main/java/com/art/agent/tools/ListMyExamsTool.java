package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolResult;
import com.art.service.ExamModuleService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人的考级/比赛报名记录（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyExamsTool implements AgentTool {

    private final ExamModuleService examModuleService;

    @Override
    public String name() { return "list_my_exams"; }

    @Override
    public String description() { return "查询我的考级/比赛报名记录（项目名称/类型/报名时间/审核状态/成绩）"; }

    @Override
    public String parametersSchema() { return "{}"; }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        List<Map<String, Object>> regs = examModuleService.listRegistrations(null, studentId, null, null);
        if (regs == null) regs = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        int show = Math.min(regs.size(), 10);
        for (int i = 0; i < show; i++) {
            Map<String, Object> r = regs.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("regId", r.get("regId"));
            m.put("projectName", r.get("projectName"));
            m.put("activityType", r.get("activityType"));
            m.put("status", r.get("status"));
            m.put("registerTime", r.get("registerTime"));
            m.put("result", r.get("result"));
            m.put("award", r.get("award"));
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("你目前没有考级/比赛报名记录。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("你共有 ").append(regs.size()).append(" 条考级/比赛报名记录，前 ").append(show).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("projectName")).append("（").append(r.get("activityType"))
                    .append("，状态：").append(r.get("status"));
            if (r.get("result") != null) sb.append("，成绩：").append(r.get("result"));
            if (r.get("award") != null) sb.append("，奖项：").append(r.get("award"));
            sb.append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", regs.size(), "rows", rows));
    }
}
