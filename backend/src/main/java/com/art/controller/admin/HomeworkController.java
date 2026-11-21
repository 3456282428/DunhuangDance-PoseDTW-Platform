package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.service.HomeworkService;
import com.art.vo.HomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 运营管理端 - 作业管理（查看统计）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/homework")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class HomeworkController {

    private final HomeworkService homeworkService;

    /**
     * 分页查询作业记录
     */
    @GetMapping("/list")
    public Result<PageResult<HomeworkVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer classId,
                                               @RequestParam(required = false) String status) {
        return Result.success(homeworkService.pageHomeworks(pageNum, pageSize, classId, status));
    }
}