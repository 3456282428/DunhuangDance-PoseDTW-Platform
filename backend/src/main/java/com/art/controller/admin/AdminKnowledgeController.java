package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.KbBackup;
import com.art.entity.KbDoc;
import com.art.entity.KbOperationLog;
import com.art.search.KbModelRecommendService;
import com.art.service.KnowledgeBackupService;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营管理端 - 知识库统一管理
 * 三端知识库（管理员/教师/学生）统一管控 + 备份 + 操作日志
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeBackupService backupService;
    /** 3D模型推荐引擎（条目→模型映射，随知识库内容动态更新） */
    private final KbModelRecommendService modelRecommendService;
    /** 跨知识库关联服务（全局关联图谱数据源） */
    private final com.art.search.KbRelationService relationService;
    private final com.art.mapper.KbOperationLogMapper opLogMapper;

    /**
     * 分页查询指定端知识库（scope=ADMIN/TEACHER/STUDENT）
     */
    @GetMapping("/{scope}/page")
    public Result<PageResult<KbDoc>> pageDocs(@PathVariable String scope,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(knowledgeService.pageDocs(scope, pageNum, pageSize, keyword, category, status, null));
    }

    /**
     * 查看知识条目详情
     */
    @GetMapping("/{scope}/{kbId}")
    public Result<KbDoc> getDoc(@PathVariable String scope, @PathVariable Integer kbId) {
        return Result.success(knowledgeService.getDoc(scope, kbId, null));
    }

    /**
     * 跨知识库关联内容联动查询：列出与某条目存在业务关联的其他条目
     */
    @GetMapping("/{scope}/{kbId}/related")
    public Result<List<Map<String, Object>>> related(@PathVariable String scope, @PathVariable Integer kbId) {
        return Result.success(knowledgeService.relatedOf(scope, kbId, null));
    }

    /**
     * 跨知识库关联图谱（节点+关系边），供关联关系可视化
     */
    @GetMapping("/{scope}/{kbId}/graph")
    public Result<Map<String, Object>> graph(@PathVariable String scope, @PathVariable Integer kbId) {
        return Result.success(knowledgeService.graph(scope, kbId, null));
    }

    /**
     * 全局关联图谱数据（3D建模图谱）：实体节点+关联边（类型/维度/强度）+维度统计
     */
    @GetMapping("/relation-graph")
    public Result<Map<String, Object>> relationGraph() {
        return Result.success(relationService.graphOverview());
    }

    /**
     * 条目适配的3D模型推荐（结构化输出：模型信息+匹配分+匹配规则，随知识库内容自动更新）
     */
    @GetMapping("/{scope}/{kbId}/model3d")
    public Result<Map<String, Object>> model3d(@PathVariable String scope, @PathVariable Integer kbId) {
        return Result.success(modelRecommendService.recommendationOf(scope, kbId));
    }

    /**
     * 重建3D模型推荐映射（scope=ALL或指定端别；知识库内容变化后可手动触发重算）
     */
    @PostMapping("/model3d/rebuild")
    public Result<Map<String, Object>> rebuildModel3d(@RequestParam(defaultValue = "ALL") String scope) {
        if ("ALL".equalsIgnoreCase(scope)) {
            return Result.success(modelRecommendService.rebuildAll());
        }
        return Result.success(Map.of(scope, modelRecommendService.rebuildScope(scope)));
    }

    /**
     * 新增知识条目（管理员发布：owner为空=机构级公开）
     */
    @PostMapping("/{scope}")
    public Result<Void> createDoc(@PathVariable String scope, @RequestBody KbDoc doc) {
        knowledgeService.createDoc(scope, doc, null,
                SecurityUtil.getCurrentUsername());
        return Result.success();
    }

    /**
     * 更新知识条目
     */
    @PutMapping("/{scope}/{kbId}")
    public Result<Void> updateDoc(@PathVariable String scope, @PathVariable Integer kbId, @RequestBody KbDoc doc) {
        knowledgeService.updateDoc(scope, kbId, doc, null);
        return Result.success();
    }

    /**
     * 删除知识条目
     */
    @DeleteMapping("/{scope}/{kbId}")
    public Result<Void> deleteDoc(@PathVariable String scope, @PathVariable Integer kbId) {
        knowledgeService.deleteDoc(scope, kbId, null);
        return Result.success();
    }

    /**
     * 三端知识库统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(knowledgeService.stats());
    }

    /**
     * 操作日志查询（可按端别/操作类型过滤）
     */
    @GetMapping("/logs")
    public Result<PageResult<KbOperationLog>> listLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String kbScope,
                                                       @RequestParam(required = false) String opType) {
        PageHelper.startPage(pageNum, pageSize);
        List<KbOperationLog> logs = opLogMapper.selectPage(kbScope, opType);
        com.github.pagehelper.PageInfo<KbOperationLog> info = new com.github.pagehelper.PageInfo<>(logs);
        return Result.success(PageResult.of(info.getTotal(), info.getList(), pageNum, pageSize));
    }

    /**
     * 创建备份（scope=ALL/ADMIN/TEACHER/STUDENT）
     */
    @PostMapping("/backup")
    public Result<KbBackup> createBackup(@RequestParam(defaultValue = "ALL") String scope) {
        return Result.success(backupService.createBackup(scope, "MANUAL", SecurityUtil.getCurrentUsername()));
    }

    /**
     * 备份记录列表
     */
    @GetMapping("/backup/list")
    public Result<List<KbBackup>> listBackups() {
        return Result.success(backupService.listBackups());
    }

    /**
     * 从备份恢复
     */
    @PostMapping("/backup/{backupId}/restore")
    public Result<Map<String, Object>> restoreBackup(@PathVariable Integer backupId) {
        return Result.success(backupService.restoreBackup(backupId, SecurityUtil.getCurrentUsername()));
    }
}
