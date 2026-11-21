package com.art.controller.student;

import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.SubmitHomeworkDTO;
import com.art.service.AiSemanticService;
import com.art.service.DanceAiService;
import com.art.service.KnowledgeService;
import com.art.service.StudentHomeworkService;
import com.art.vo.StudentHomeworkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学员端 - 作业提交与学习进度
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/homework")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentHomeworkController {

    private final StudentHomeworkService homeworkService;
    private final DanceAiService danceAiService;
    private final AiSemanticService aiSemanticService;

    /**
     * 分页查询我的作业列表（含提交状态）
     */
    @GetMapping("/list")
    public Result<PageResult<StudentHomeworkVO>> listHomeworks(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                               @RequestParam(required = false) String status) {
        return Result.success(homeworkService.pageMyHomeworks(pageNum, pageSize, status));
    }

    /**
     * 提交作业（支持文字、图片/视频/PDF附件）
     */
    @PostMapping("/{homeworkId}/submit")
    public Result<Void> submitHomework(@PathVariable Integer homeworkId,
                                       @RequestBody SubmitHomeworkDTO submitDTO) {
        homeworkService.submitHomework(homeworkId, submitDTO);
        return Result.success();
    }

    /**
     * 查看作业详情与教师点评
     */
    @GetMapping("/{homeworkId}")
    public Result<StudentHomeworkVO> getHomeworkDetail(@PathVariable Integer homeworkId) {
        return Result.success(homeworkService.getHomeworkDetail(homeworkId));
    }

    /**
     * 触发舞蹈AI智能评分（异步，按当前学员在该作业下的提交）
     */
    @PostMapping("/{homeworkId}/ai-score")
    public Result<Map<String, Object>> triggerAiScore(@PathVariable Integer homeworkId) {
        Integer submissionId = findMySubmissionId(homeworkId);
        return Result.success(danceAiService.triggerScore(submissionId, false));
    }

    /**
     * 查询舞蹈AI智能评分结果（当前学员在该作业下的提交）
     */
    @GetMapping("/{homeworkId}/ai-score")
    public Result<Map<String, Object>> getAiScore(@PathVariable Integer homeworkId) {
        Integer submissionId = findMySubmissionId(homeworkId);
        return Result.success(danceAiService.getAiResult(submissionId));
    }

    /** 按当前登录学员查询该作业的提交记录ID */
    private Integer findMySubmissionId(Integer homeworkId) {
        StudentHomeworkVO detail = homeworkService.getHomeworkDetail(homeworkId);
        if (detail == null || detail.getSubmissionId() == null) {
            throw new BusinessException("未找到该作业的提交记录");
        }
        return detail.getSubmissionId();
    }

    /**
     * 生成AI教练语义点评（通道二·豆包多模态语义增强，限本人提交；force=true强制重新分析）
     */
    @PostMapping("/{homeworkId}/ai-semantics")
    public Result<Map<String, Object>> enhanceSemantics(@PathVariable Integer homeworkId,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        Integer submissionId = findMySubmissionId(homeworkId);
        return Result.success(aiSemanticService.enhanceSemantics(submissionId, KnowledgeService.SCOPE_STUDENT, force));
    }
}
