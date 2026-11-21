package com.art.controller.teacher;

import com.art.common.Result;
import com.art.dto.StudentRequestDTO;
import com.art.service.StudentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 处理学员请假 / 缓考 / 补考申请
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/request")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherRequestController {

    private final StudentRequestService requestService;

    /**
     * 查询发给我的学员申请（请假/缓考/补考）
     */
    @GetMapping("/list")
    public Result<List<StudentRequestDTO>> listRequests() {
        return Result.success(requestService.listForTeacher());
    }

    /**
     * 处理申请（批准或拒绝）
     */
    @PutMapping("/{requestId}/handle")
    public Result<Void> handle(@PathVariable Integer requestId,
                               @RequestParam String action,   // APPROVE / REJECT
                               @RequestParam(required = false) String remark) {
        requestService.handle(requestId, action, remark);
        return Result.success();
    }
}
