package com.art.service;

import com.art.vo.FeedbackVO;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface TeacherFeedbackService {
    List<FeedbackVO> listMyFeedback(String status);
    FeedbackVO getFeedbackDetail(Integer feedbackId);
    void replyFeedback(Integer feedbackId, String replyContent);
}