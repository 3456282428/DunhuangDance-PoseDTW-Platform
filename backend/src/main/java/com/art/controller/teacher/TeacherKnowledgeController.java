package com.art.controller.teacher;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.KbDoc;
import com.art.entity.Teacher;
import com.art.mapper.TeacherMapper;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 教师端 - 我的教学知识库
 * 可见范围：本人条目 + 机构共享条目；自动归集条目（作业布置/批改点评）只读
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/knowledge")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherKnowledgeController {

    private final KnowledgeService knowledgeService;
    private final TeacherMapper teacherMapper;

    @GetMapping("/page")
    public Result<PageResult<KbDoc>> pageDocs(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Integer status) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        return Result.success(knowledgeService.pageDocs(KnowledgeService.SCOPE_TEACHER, pageNum, pageSize,
                keyword, category, status, teacherId));
    }

    @GetMapping("/{kbId}")
    public Result<KbDoc> getDoc(@PathVariable Integer kbId) {
        knowledgeService.increaseView(KnowledgeService.SCOPE_TEACHER, kbId);
        return Result.success(knowledgeService.getDoc(KnowledgeService.SCOPE_TEACHER, kbId, SecurityUtil.getCurrentTeacherId()));
    }

    /**
     * 新增教学资料（归属本人）
     */
    @PostMapping
    public Result<Void> createDoc(@RequestBody KbDoc doc) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        Teacher teacher = teacherId != null ? teacherMapper.selectById(teacherId) : null;
        knowledgeService.createDoc(KnowledgeService.SCOPE_TEACHER, doc, teacherId, teacher != null ? teacher.getName() : null);
        return Result.success();
    }

    @PutMapping("/{kbId}")
    public Result<Void> updateDoc(@PathVariable Integer kbId, @RequestBody KbDoc doc) {
        knowledgeService.updateDoc(KnowledgeService.SCOPE_TEACHER, kbId, doc, SecurityUtil.getCurrentTeacherId());
        return Result.success();
    }

    @DeleteMapping("/{kbId}")
    public Result<Void> deleteDoc(@PathVariable Integer kbId) {
        knowledgeService.deleteDoc(KnowledgeService.SCOPE_TEACHER, kbId, SecurityUtil.getCurrentTeacherId());
        return Result.success();
    }
}
