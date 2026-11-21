package com.art.controller.admin;

import com.art.common.Result;
import com.art.entity.AiInvokeLog;
import com.art.entity.AiModel;
import com.art.entity.AiModelKbBinding;
import com.art.mapper.AiInvokeLogMapper;
import com.art.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - AI模型管理（预留）
 * 模型注册 / 模型-知识库关联约定 / 调用日志查看
 * 说明：本期仅提供对接预留（接口定义、参数规范、调用入口），不实现具体AI业务逻辑
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/ai-model")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiModelController {

    private final AiModelService aiModelService;
    private final AiInvokeLogMapper invokeLogMapper;

    // ============ 模型注册管理（预留） ============

    @GetMapping("/list")
    public Result<List<AiModel>> listModels() {
        return Result.success(aiModelService.listModels());
    }

    @GetMapping("/{modelId}")
    public Result<AiModel> getModel(@PathVariable Integer modelId) {
        return Result.success(aiModelService.getModel(modelId));
    }

    @PostMapping
    public Result<Void> createModel(@RequestBody AiModel model) {
        aiModelService.createModel(model);
        return Result.success();
    }

    @PutMapping("/{modelId}")
    public Result<Void> updateModel(@PathVariable Integer modelId, @RequestBody AiModel model) {
        aiModelService.updateModel(modelId, model);
        return Result.success();
    }

    @DeleteMapping("/{modelId}")
    public Result<Void> deleteModel(@PathVariable Integer modelId) {
        aiModelService.deleteModel(modelId);
        return Result.success();
    }

    // ============ 模型-知识库关联约定 ============

    @GetMapping("/binding/list")
    public Result<List<AiModelKbBinding>> listBindings() {
        return Result.success(aiModelService.listBindings());
    }

    @PostMapping("/binding")
    public Result<Void> createBinding(@RequestBody AiModelKbBinding binding) {
        aiModelService.createBinding(binding);
        return Result.success();
    }

    @PutMapping("/binding/{bindingId}/enabled")
    public Result<Void> updateBindingEnabled(@PathVariable Integer bindingId, @RequestParam Integer enabled) {
        aiModelService.updateBindingEnabled(bindingId, enabled);
        return Result.success();
    }

    @DeleteMapping("/binding/{bindingId}")
    public Result<Void> deleteBinding(@PathVariable Integer bindingId) {
        aiModelService.deleteBinding(bindingId);
        return Result.success();
    }

    // ============ AI调用日志 ============

    @GetMapping("/invoke-logs")
    public Result<List<AiInvokeLog>> listInvokeLogs() {
        return Result.success(invokeLogMapper.selectAll());
    }
}
