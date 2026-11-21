package com.art.controller.admin;

import com.art.common.Result;
import com.art.entity.ExamProject;
import com.art.service.ExamModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 运营管理端 - 考级与比赛管理  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/exam-module")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExamModuleAdminController {

    private final ExamModuleService service;

    @GetMapping("/projects")
    public Result<?> projects(@RequestParam(required=false) String activityType,
                              @RequestParam(required=false) String category,
                              @RequestParam(required=false) String status,
                              @RequestParam(required=false) String keyword) {
        return Result.success(service.listProjects(activityType, category, status, keyword));
    }

    @PostMapping("/project")
    public Result<Void> create(@RequestBody ExamProject p) { service.createProject(p); return Result.success(); }

    @PutMapping("/project/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody ExamProject p) {
        p.setProjectId(id); service.updateProject(p); return Result.success();
    }

    @DeleteMapping("/project/{id}")
    public Result<Void> delete(@PathVariable Integer id) { service.deleteProject(id); return Result.success(); }

    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam(required=false) Integer projectId,
                                   @RequestParam(required=false) String status,
                                   @RequestParam(required=false) String activityType) {
        return Result.success(service.listRegistrations(projectId, null, status, activityType));
    }

    /** 审核：pass=true 确认，false 驳回 */
    @PostMapping("/registration/{id}/audit")
    public Result<Void> audit(@PathVariable Integer id, @RequestBody Map<String,Object> body) {
        Boolean pass = Boolean.valueOf(String.valueOf(body.get("pass")));
        service.audit(id, pass, body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        return Result.success();
    }

    /** 确认缴费 */
    @PostMapping("/registration/{id}/paid")
    public Result<Void> paid(@PathVariable Integer id) { service.pay(id, "线下"); return Result.success(); }

    /** 考场安排 */
    @PostMapping("/registration/{id}/arrange")
    public Result<Void> arrange(@PathVariable Integer id) {
        service.arrange(id);
        return Result.success();
    }

    /** 成绩录入 */
    @PostMapping("/registration/{id}/result")
    public Result<Void> result(@PathVariable Integer id, @RequestBody Map<String,Object> body) {
        service.enterResult(id, str(body.get("result")), str(body.get("award")), str(body.get("certificateNo")));
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<?> stats() { return Result.success(service.stats()); }

    private String str(Object o){ return o==null?null:String.valueOf(o); }
}
