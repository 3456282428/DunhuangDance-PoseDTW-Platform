package com.art.controller.student;

import com.art.common.Result;
import com.art.dto.StudentRequestDTO;
import com.art.service.StudentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员端 - 请假 / 缓考 / 补考申请
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/request")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentRequestController {

    private final StudentRequestService requestService;

    /**
     * 提交请假 / 缓考 / 补考申请
     */
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody StudentRequestDTO dto) {
        requestService.submit(dto);
        return Result.success();
    }

    /**
     * 查询我的申请记录及处理状态
     */
    @GetMapping("/list")
    public Result<List<StudentRequestDTO>> listMine() {
        return Result.success(requestService.listMine());
    }
}
