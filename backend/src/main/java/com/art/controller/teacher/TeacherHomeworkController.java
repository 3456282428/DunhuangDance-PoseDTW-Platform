package com.art.controller.teacher;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.AssignHomeworkDTO;
import com.art.dto.GradeHomeworkDTO;
import com.art.service.AiSemanticService;
import com.art.service.DanceAiService;
import com.art.service.KnowledgeService;
import com.art.service.TeacherHomeworkService;
import com.art.vo.HomeworkVO;
import com.art.vo.HomeworkSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 - 作业管理
 * 布置作业、查看作业提交情况、批改作业与点评
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/homework")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherHomeworkController {

    private final TeacherHomeworkService homeworkService;
    private final DanceAiService danceAiService;
    private final AiSemanticService aiSemanticService;

    /**
     * 布置作业（关联班级与某次课程）
     */
    @PostMapping("/assign")
    public Result<Void> assignHomework(@RequestBody AssignHomeworkDTO assignDTO) {
        homeworkService.assignHomework(assignDTO);
        return Result.success();
    }

    /**
     * 查询我布置的作业列表（可按班级筛选）
     */
    @GetMapping("/list")
    public Result<PageResult<HomeworkVO>> listMyHomework(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) Integer classId,
                                                         @RequestParam(required = false) String status) {
        return Result.success(homeworkService.pageMyHomework(pageNum, pageSize, classId, status));
    }

    /**
     * 查看某个作业的学员提交情况
     */
    @GetMapping("/{homeworkId}/submissions")
    public Result<List<HomeworkSubmissionVO>> getSubmissions(@PathVariable Integer homeworkId) {
        return Result.success(homeworkService.getSubmissions(homeworkId));
    }

    /**
     * 批改作业（按提交记录 submissionId 评分与点评）
     */
    @PutMapping("/{submissionId}/grade")
    public Result<Void> gradeHomework(@PathVariable Integer submissionId,
                                      @RequestBody GradeHomeworkDTO gradeDTO) {
        homeworkService.gradeHomework(submissionId, gradeDTO);
        return Result.success();
    }

    /**
     * 查看某个学生在某作业下的提交详情
     */
    @GetMapping("/{homeworkId}/student/{studentId}")
    public Result<HomeworkSubmissionVO> getStudentHomework(@PathVariable Integer homeworkId,
                                                           @PathVariable Integer studentId) {
        return Result.success(homeworkService.getStudentHomework(homeworkId, studentId));
    }

    /**
     * 触发舞蹈AI智能评分（异步，按提交记录submissionId；force=true强制重新评分）
     */
    @PostMapping("/{submissionId}/ai-score")
    public Result<Map<String, Object>> triggerAiScore(@PathVariable Integer submissionId,
                                                      @RequestParam(defaultValue = "false") boolean force) {
        return Result.success(danceAiService.triggerScore(submissionId, force));
    }

    /**
     * 查询舞蹈AI智能评分结果（按提交记录submissionId）
     */
    @GetMapping("/{submissionId}/ai-score")
    public Result<Map<String, Object>> getAiScore(@PathVariable Integer submissionId) {
        return Result.success(danceAiService.getAiResult(submissionId));
    }

    /**
     * 生成AI教练语义点评（通道二·豆包多模态语义增强；force=true强制重新分析）
     */
    @PostMapping("/{submissionId}/ai-semantics")
    public Result<Map<String, Object>> enhanceSemantics(@PathVariable Integer submissionId,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        return Result.success(aiSemanticService.enhanceSemantics(submissionId, KnowledgeService.SCOPE_TEACHER, force));
    }
}
