package com.art.service;

import com.art.dto.FeedbackDTO;
import com.art.dto.StudentProfileDTO;
import com.art.vo.LearningProgressVO;
import com.art.vo.NewsVO;
import com.art.vo.OrganizationVO;
import com.art.vo.FeedbackVO;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface StudentProfileService {
    StudentProfileDTO getMyProfile();
    void updateProfile(StudentProfileDTO profileDTO);
    LearningProgressVO getLearningProgress();
    List<NewsVO> getNewsList();
    OrganizationVO getOrganizationInfo();
    void submitFeedback(FeedbackDTO feedbackDTO);
    List<FeedbackVO> listMyFeedback();
}