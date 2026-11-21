package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherFeedbackService;
import com.art.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 反馈回复处理
 * 回复学员/家长的留言、建议或投诉
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/feedback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherFeedbackController {

    private final TeacherFeedbackService feedbackService;

    /**
     * 查询与我相关的反馈列表
     */
    @GetMapping("/list")
    public Result<List<FeedbackVO>> listMyFeedback(@RequestParam(required = false) String status) {
        return Result.success(feedbackService.listMyFeedback(status));
    }

    /**
     * 查看反馈详情
     */
    @GetMapping("/{feedbackId}")
    public Result<FeedbackVO> getFeedbackDetail(@PathVariable Integer feedbackId) {
        return Result.success(feedbackService.getFeedbackDetail(feedbackId));
    }

    /**
     * 回复反馈
     */
    @PostMapping("/{feedbackId}/reply")
    public Result<Void> replyFeedback(@PathVariable Integer feedbackId,
                                      @RequestParam String replyContent) {
        feedbackService.replyFeedback(feedbackId, replyContent);
        return Result.success();
    }
}