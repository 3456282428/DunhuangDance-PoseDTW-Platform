package com.art.controller.student;

import com.art.common.Result;
import com.art.entity.ExamRegistration;
import com.art.service.ExamModuleService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 学员端 - 考级与比赛  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/exam")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    private final ExamModuleService service;

    /** 浏览可报名项目 */
    @GetMapping("/projects")
    public Result<?> projects(@RequestParam(required=false) String activityType,
                              @RequestParam(required=false) String category) {
        return Result.success(service.listProjects(activityType, category, null, null));
    }

    /** 在线报名 */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody ExamRegistration form) {
        service.register(form); return Result.success();
    }

    /** 我的报名 */
    @GetMapping("/my")
    public Result<?> my() {
        Integer sid = SecurityUtil.getCurrentStudentId();
        return Result.success(service.listRegistrations(null, sid, null, null));
    }

    /** 取消报名 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Integer id) { service.cancel(id); return Result.success(); }

    /** 在线缴费 */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Integer id, @RequestParam(defaultValue="微信") String payMethod) {
        service.pay(id, payMethod); return Result.success();
    }
}
