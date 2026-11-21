package com.art.controller.student;

import com.art.common.Result;
import com.art.dto.FeedbackDTO;
import com.art.dto.StudentProfileDTO;
import com.art.service.StudentProfileService;
import com.art.vo.FeedbackVO;
import com.art.vo.LearningProgressVO;
import com.art.vo.NewsVO;
import com.art.vo.OrganizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员端 - 个人信息、学习进度、机构咨询与互动反馈
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService profileService;

    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Result<StudentProfileDTO> getProfile() {
        return Result.success(profileService.getMyProfile());
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/info")
    public Result<Void> updateProfile(@RequestBody StudentProfileDTO profileDTO) {
        profileService.updateProfile(profileDTO);
        return Result.success();
    }

    /**
     * 获取学习进度总览（出勤、作业、考级/比赛等）
     */
    @GetMapping("/progress")
    public Result<LearningProgressVO> getLearningProgress() {
        return Result.success(profileService.getLearningProgress());
    }

    /**
     * 获取机构发布的资讯列表（机构咨询）
     */
    @GetMapping("/news")
    public Result<List<NewsVO>> getNewsList() {
        return Result.success(profileService.getNewsList());
    }

    /**
     * 获取机构基本信息（简介、联系方式等）
     */
    @GetMapping("/organization")
    public Result<OrganizationVO> getOrganizationInfo() {
        return Result.success(profileService.getOrganizationInfo());
    }

    /**
     * 提交建议或投诉（互动反馈）
     */
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@RequestBody FeedbackDTO feedbackDTO) {
        profileService.submitFeedback(feedbackDTO);
        return Result.success();
    }

    /**
     * 查询我的反馈记录及处理状态
     */
    @GetMapping("/feedback/list")
    public Result<List<FeedbackVO>> listMyFeedback() {
        return Result.success(profileService.listMyFeedback());
    }
}