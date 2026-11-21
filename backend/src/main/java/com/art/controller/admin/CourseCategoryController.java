package com.art.controller.admin;

import com.art.common.Result;
import com.art.entity.CourseCategory;
import com.art.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - 课程产品配置（课程类别）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class CourseCategoryController {

    private final CourseCategoryService categoryService;

    /**
     * 查询所有课程类别
     */
    @GetMapping("/list")
    public Result<List<CourseCategory>> list() {
        return Result.success(categoryService.listAll());
    }

    /**
     * 新增课程类别
     */
    @PostMapping
    public Result<Void> create(@RequestBody CourseCategory category) {
        categoryService.create(category);
        return Result.success();
    }

    /**
     * 修改课程类别
     */
    @PutMapping("/{categoryId}")
    public Result<Void> update(@PathVariable Integer categoryId, @RequestBody CourseCategory category) {
        category.setCategoryId(categoryId);
        categoryService.update(category);
        return Result.success();
    }

    /**
     * 删除课程类别（需检查是否有关联班级）
     */
    @DeleteMapping("/{categoryId}")
    public Result<Void> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);
        return Result.success();
    }
}