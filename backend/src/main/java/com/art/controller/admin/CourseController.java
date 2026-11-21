package com.art.controller.admin;

import com.art.common.Result;
import com.art.entity.Course;
import com.art.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - 课程产品配置（类别与具体课程整合）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/course")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/list")
    public Result<List<Course>> list(@RequestParam(required = false) String courseName,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) Integer status) {
        return Result.success(courseService.list(courseName, category, status));
    }

    @GetMapping("/{courseId}")
    public Result<Course> detail(@PathVariable Integer courseId) {
        return Result.success(courseService.getById(courseId));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Course course) {
        courseService.create(course);
        return Result.success();
    }

    @PutMapping("/{courseId}")
    public Result<Void> update(@PathVariable Integer courseId, @RequestBody Course course) {
        courseService.update(courseId, course);
        return Result.success();
    }

    @DeleteMapping("/{courseId}")
    public Result<Void> delete(@PathVariable Integer courseId) {
        courseService.delete(courseId);
        return Result.success();
    }
}
