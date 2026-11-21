package com.art.controller.student;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.KbDoc;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端 - 我的学习知识库（只读浏览：本人条目 + 公开共享条目）
 * 数据来源：学习资料（管理员/教师共享）+ 作业成果自动归集
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/knowledge")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentKnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/page")
    public Result<PageResult<KbDoc>> pageDocs(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Integer status) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        return Result.success(knowledgeService.pageDocs(KnowledgeService.SCOPE_STUDENT, pageNum, pageSize,
                keyword, category, status, studentId));
    }

    @GetMapping("/{kbId}")
    public Result<KbDoc> getDoc(@PathVariable Integer kbId) {
        knowledgeService.increaseView(KnowledgeService.SCOPE_STUDENT, kbId);
        return Result.success(knowledgeService.getDoc(KnowledgeService.SCOPE_STUDENT, kbId, SecurityUtil.getCurrentStudentId()));
    }
}
