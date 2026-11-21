package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.Attendance;
import com.art.service.StudentAttendanceService;
import com.art.util.SecurityUtil;
import com.art.vo.AttendanceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前学生本人考勤汇总与最近记录（学生端只读）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyAttendanceTool implements AgentTool {

    private final StudentAttendanceService attendanceService;

    @Override
    public String name() { return "list_my_attendance"; }

    @Override
    public String description() { return "查询我的考勤统计（出勤率/迟到/缺勤/请假）与最近考勤记录"; }

    @Override
    public String parametersSchema() {
        return "{\"pageSize\":{\"type\":\"int\",\"desc\":\"返回最近记录条数，默认5，最大10\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"STUDENT"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        if (SecurityUtil.getCurrentStudentId() == null) return ToolResult.fail("未找到当前学生的档案信息", "请确认账号已关联学生档案");
        int size = ToolArgs.limit(args, "pageSize", 5, 10);
        AttendanceSummaryVO summary = attendanceService.getMyAttendanceSummary();
        PageResult<Attendance> page = attendanceService.pageMyAttendance(1, size, null, null, null);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Attendance a : page.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseDate", String.valueOf(a.getCourseDate()));
            m.put("className", a.getClassName());
            m.put("status", a.getStatus());
            rows.add(m);
        }
        StringBuilder sb = new StringBuilder("考勤汇总：应到 ").append(summary.getTotalSchedules())
                .append(" 次，出勤 ").append(summary.getAttended())
                .append("，迟到 ").append(summary.getLate())
                .append("，缺勤 ").append(summary.getAbsent())
                .append("，请假 ").append(summary.getLeave())
                .append("，出勤率 ").append(summary.getAttendanceRate() == null ? "0" : String.format("%.1f%%", summary.getAttendanceRate()))
                .append("。最近 ").append(rows.size()).append(" 条记录：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("courseDate")).append(" ").append(r.get("className")).append("：").append(r.get("status"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", Map.of(
                "totalSchedules", summary.getTotalSchedules() == null ? 0 : summary.getTotalSchedules(),
                "attended", summary.getAttended() == null ? 0 : summary.getAttended(),
                "late", summary.getLate() == null ? 0 : summary.getLate(),
                "absent", summary.getAbsent() == null ? 0 : summary.getAbsent(),
                "leave", summary.getLeave() == null ? 0 : summary.getLeave(),
                "attendanceRate", summary.getAttendanceRate() == null ? 0 : summary.getAttendanceRate()));
        data.put("rows", rows);
        return ToolResult.ok(sb.toString(), data);
    }
}
