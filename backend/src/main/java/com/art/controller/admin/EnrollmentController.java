package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.EnrollmentDTO;
import com.art.entity.Enrollment;
import com.art.entity.Student;
import com.art.service.EnrollmentService;
import com.art.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - 报名与分班管理
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/enrollment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC','FINANCE')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final com.art.mapper.UserMapper userMapper;

    /**
     * 学员下拉选项
     */
    @GetMapping("/student-options")
    public Result<List<Student>> studentOptions() {
        java.util.Map<Integer, String> uname = new java.util.HashMap<>();
        for (com.art.entity.SysUser u : userMapper.selectAll()) uname.put(u.getUserId(), u.getUsername());
        List<Student> list = studentService.listAll();
        for (Student s : list) s.setUsername(s.getUserId() == null ? null : uname.get(s.getUserId()));
        return Result.success(list);
    }

    /**
     * 分页查询报名记录
     */
    @GetMapping("/list")
    public Result<PageResult<Enrollment>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String studentName,
                                               @RequestParam(required = false) Integer classId,
                                               @RequestParam(required = false) String status) {
        return Result.success(enrollmentService.pageEnrollments(pageNum, pageSize, studentName, classId, status));
    }

    /**
     * 新增报名（同时可完成缴费）
     */
    @PostMapping
    public Result<Void> create(@RequestBody EnrollmentDTO enrollmentDTO) {
        enrollmentService.createEnrollment(enrollmentDTO);
        return Result.success();
    }

    /**
     * 修改报名状态（如退学、结课）
     */
    @PutMapping("/{enrollmentId}/status")
    public Result<Void> updateStatus(@PathVariable Integer enrollmentId, @RequestParam String status) {
        enrollmentService.updateStatus(enrollmentId, status);
        return Result.success();
    }

    @DeleteMapping("/{enrollmentId}")
    public Result<Void> delete(@PathVariable Integer enrollmentId) {
        enrollmentService.delete(enrollmentId);
        return Result.success();
    }
}