package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolResult;
import com.art.service.StudentCourseService;
import com.art.util.SecurityUtil;
import com.art.vo.AppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人的试听/补课预约（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyAppointmentsTool implements AgentTool {

    private final StudentCourseService courseService;

    @Override
    public String name() { return "list_my_appointments"; }

    @Override
    public String description() { return "查询我的试听/补课预约（类型/班级/教师/预约时间/状态）"; }

    @Override
    public String parametersSchema() { return "{}"; }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        if (SecurityUtil.getCurrentStudentId() == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        List<AppointmentDTO> list = courseService.getMyAppointments();
        List<Map<String, Object>> rows = new ArrayList<>();
        int show = Math.min(list.size(), 10);
        for (int i = 0; i < show; i++) {
            AppointmentDTO a = list.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appointmentId", a.getAppointmentId());
            m.put("type", a.getAppointmentType());
            m.put("className", a.getClassName());
            m.put("teacherName", a.getTeacherName());
            m.put("appointmentTime", a.getAppointmentTime() == null ? null : String.valueOf(a.getAppointmentTime()));
            m.put("status", a.getStatus());
            rows.add(m);
        }
        if (rows.isEmpty()) return ToolResult.ok("你目前没有试听/补课预约。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("你共有 ").append(list.size()).append(" 条预约，前 ").append(show).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("type")).append("：").append(r.get("className"))
                    .append("（教师：").append(r.get("teacherName")).append("，").append(r.get("appointmentTime"))
                    .append("，").append(r.get("status")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", list.size(), "rows", rows));
    }
}
