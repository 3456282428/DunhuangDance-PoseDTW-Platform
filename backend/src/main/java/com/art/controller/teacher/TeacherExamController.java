package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.ExamModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 教师端 - 考级与比赛（推荐报名、成绩查看）  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/exam")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExamController {

    private final ExamModuleService service;

    @GetMapping("/projects")
    public Result<?> projects() { return Result.success(service.listProjects(null, null, null, null)); }

    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam(required=false) String status,
                                   @RequestParam(required=false) String activityType) {
        return Result.success(service.listRegistrations(null, null, status, activityType));
    }

    /** 输入学号自动回显姓名与在读班级 */
    @GetMapping("/student-lookup")
    public Result<?> lookup(@RequestParam String studentNo) {
        return Result.success(service.lookupStudent(studentNo));
    }

    /** 教师推荐/代为报名（按学号） */
    @PostMapping("/recommend")
    public Result<Void> recommend(@RequestBody Map<String,Object> body) {
        String studentNo = body.get("studentNo") == null ? null : String.valueOf(body.get("studentNo"));
        Integer classId = body.get("classId") == null ? null : Integer.valueOf(String.valueOf(body.get("classId")));
        Integer projectId = body.get("projectId") == null ? null : Integer.valueOf(String.valueOf(body.get("projectId")));
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment"));
        service.recommend(studentNo, classId, projectId, comment);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<?> stats() { return Result.success(service.stats()); }
}
