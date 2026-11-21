package com.art.controller.admin;

import com.art.common.Result;
import com.art.entity.Classroom;
import com.art.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营管理端 - 系统基础设置
 * 管理教室资源、学期、课节时间模板等
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    // ============ 教室管理 ============

    /**
     * 查询所有教室
     */
    @GetMapping("/classroom/list")
    public Result<List<Classroom>> listClassrooms() {
        return Result.success(systemConfigService.listClassrooms());
    }

    /**
     * 新增教室
     */
    @PostMapping("/classroom")
    public Result<Void> addClassroom(@RequestBody Classroom classroom) {
        systemConfigService.addClassroom(classroom);
        return Result.success();
    }

    /**
     * 修改教室
     */
    @PutMapping("/classroom/{classroomId}")
    public Result<Void> updateClassroom(@PathVariable Integer classroomId, @RequestBody Classroom classroom) {
        classroom.setClassroomId(classroomId);
        systemConfigService.updateClassroom(classroom);
        return Result.success();
    }

    /**
     * 删除教室
     */
    @DeleteMapping("/classroom/{classroomId}")
    public Result<Void> deleteClassroom(@PathVariable Integer classroomId) {
        systemConfigService.deleteClassroom(classroomId);
        return Result.success();
    }

    // ============ 学期管理 ============

    /**
     * 获取当前学期列表
     */
    @GetMapping("/semester/list")
    public Result<List<String>> listSemesters() {
        return Result.success(systemConfigService.listSemesters());
    }

    /**
     * 新增学期
     */
    @PostMapping("/semester")
    public Result<Void> addSemester(@RequestParam String semester) {
        systemConfigService.addSemester(semester);
        return Result.success();
    }

    // ============ 课节时间模板 ============

    /**
     * 获取课节时间模板
     */
    @GetMapping("/time-template")
    public Result<Map<String, Object>> getTimeTemplate() {
        return Result.success(systemConfigService.getTimeTemplate());
    }

    /**
     * 更新课节时间模板
     */
    @PutMapping("/time-template")
    public Result<Void> updateTimeTemplate(@RequestBody Map<String, Object> template) {
        systemConfigService.updateTimeTemplate(template);
        return Result.success();
    }

    // ============ 通用配置（机构信息/支付/通知等） ============

    @GetMapping("/config/{key}")
    public Result<Object> getConfig(@PathVariable String key) {
        return Result.success(systemConfigService.getConfig(key));
    }

    @PutMapping("/config/{key}")
    public Result<Void> saveConfig(@PathVariable String key, @RequestBody Object body) {
        systemConfigService.saveConfig(key, body);
        return Result.success();
    }
}