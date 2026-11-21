package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.ClassDTO;
import com.art.entity.CourseClass;
import com.art.entity.Teacher;
import com.art.mapper.TeacherMapper;
import com.art.service.CourseClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营管理端 - 班级管理
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/class")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class CourseClassController {

    private final CourseClassService classService;
    private final TeacherMapper teacherMapper;

    /**
     * 教师下拉选项（供班级表单选择授课教师）
     */
    @GetMapping("/teacher-options")
    public Result<List<Map<String, Object>>> teacherOptions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Teacher t : teacherMapper.selectAll()) {
            if (t.getStatus() != null && t.getStatus() == 0) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("teacherId", t.getTeacherId());
            m.put("name", t.getName());
            list.add(m);
        }
        return Result.success(list);
    }

    /**
     * 分页查询班级列表
     */
    @GetMapping("/list")
    public Result<PageResult<CourseClass>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String className,
                                                @RequestParam(required = false) Integer categoryId) {
        return Result.success(classService.pageClasses(pageNum, pageSize, className, categoryId));
    }

    /**
     * 新增班级
     */
    @PostMapping
    public Result<Void> create(@RequestBody ClassDTO classDTO) {
        classService.createClass(classDTO);
        return Result.success();
    }

    /**
     * 修改班级信息
     */
    @PutMapping("/{classId}")
    public Result<Void> update(@PathVariable Integer classId, @RequestBody ClassDTO classDTO) {
        classService.updateClass(classId, classDTO);
        return Result.success();
    }

    /**
     * 删除班级（检查是否有学员在读）
     */
    @DeleteMapping("/{classId}")
    public Result<Void> delete(@PathVariable Integer classId) {
        classService.deleteClass(classId);
        return Result.success();
    }
}