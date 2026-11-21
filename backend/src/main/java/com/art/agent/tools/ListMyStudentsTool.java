package com.art.agent.tools;

import com.art.agent.AgentContext;
import com.art.agent.AgentTool;
import com.art.agent.ToolArgs;
import com.art.agent.ToolResult;
import com.art.common.PageResult;
import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.mapper.CourseClassMapper;
import com.art.service.EnrollmentService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：查询当前教师本人班级的在读学员（教师端，数据范围仅限本人班级）
 * @author liukang&&heyan
 */
@Component
@RequiredArgsConstructor
public class ListMyStudentsTool implements AgentTool {

    private final CourseClassMapper classMapper;
    private final EnrollmentService enrollmentService;

    @Override
    public String name() { return "list_my_students"; }

    @Override
    public String description() { return "查询我（当前教师）班级里的学员名单（姓名/学号/班级/出勤），可按班级ID或姓名筛选"; }

    @Override
    public String parametersSchema() {
        return "{\"classId\":{\"type\":\"int\",\"desc\":\"班级ID（可用list_my_classes获取），不填则查全部本人班级\",\"required\":false},"
                + "\"studentName\":{\"type\":\"string\",\"desc\":\"学员姓名关键词\",\"required\":false},"
                + "\"pageSize\":{\"type\":\"int\",\"desc\":\"返回条数，默认10，最大20\",\"required\":false}}";
    }

    @Override
    public String[] allowedRoles() { return new String[]{"TEACHER"}; }

    @Override
    public ToolResult execute(Map<String, Object> args, AgentContext ctx) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return ToolResult.fail("未找到当前教师的档案信息", "请确认账号已关联教师档案");
        Integer classId = ToolArgs.integer(args, "classId");
        String studentName = ToolArgs.str(args, "studentName");
        int size = ToolArgs.limit(args, "pageSize", 10, 20);

        List<Integer> myClassIds = new ArrayList<>();
        Map<Integer, String> classNames = new LinkedHashMap<>();
        for (CourseClass c : classMapper.selectAll()) {
            if (teacherId.equals(c.getTeacherId())) {
                myClassIds.add(c.getClassId());
                classNames.put(c.getClassId(), c.getClassName());
            }
        }
        if (classId != null && !myClassIds.contains(classId)) {
            return ToolResult.fail("班级 " + classId + " 不属于您的授课范围",
                    "请先用 list_my_classes 查看您的班级ID");
        }
        List<Integer> targets = classId != null ? List.of(classId) : myClassIds;
        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0;
        for (Integer cid : targets) {
            PageResult<Enrollment> page = enrollmentService.pageEnrollments(1, 100, studentName, cid, "在读");
            total += page.getTotal().intValue();
            for (Enrollment e : page.getRows()) {
                if (rows.size() >= size) break;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("studentId", e.getStudentId());
                m.put("studentName", e.getStudentName());
                m.put("username", e.getStudentUsername());
                m.put("className", e.getClassName() != null ? e.getClassName() : classNames.get(cid));
                m.put("attendance", (e.getAttendedCount() == null ? 0 : e.getAttendedCount()) + "次出勤/"
                        + (e.getAbsentCount() == null ? 0 : e.getAbsentCount()) + "次缺勤");
                rows.add(m);
            }
            if (rows.size() >= size) break;
        }
        if (rows.isEmpty()) return ToolResult.ok("未查询到符合条件的在读学员。", Map.of("total", 0, "rows", rows));
        StringBuilder sb = new StringBuilder("共匹配 ").append(total).append(" 名在读学员，前 ").append(rows.size()).append(" 条：");
        for (Map<String, Object> r : rows) {
            sb.append("\n· ").append(r.get("studentName")).append("（").append(r.get("username"))
                    .append("，").append(r.get("className")).append("，").append(r.get("attendance")).append("）");
        }
        return ToolResult.ok(sb.toString(), Map.of("total", total, "rows", rows));
    }
}
