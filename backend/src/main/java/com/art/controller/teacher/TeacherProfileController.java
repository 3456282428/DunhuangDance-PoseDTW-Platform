package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherProfileService;
import com.art.vo.ClassVO;
import com.art.vo.TeacherProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 个人授课主页
 * 展示教师个人信息、授课班级、累计课时等
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherProfileController {

    private final TeacherProfileService profileService;

    /**
     * 获取个人授课主页信息
     */
    @GetMapping("/info")
    public Result<TeacherProfileVO> getProfile() {
        return Result.success(profileService.getMyProfile());
    }

    /**
     * 更新个人基本信息（如联系电话）
     */
    @PutMapping("/info")
    public Result<Void> updateProfile(@RequestBody TeacherProfileVO profileVO) {
        profileService.updateProfile(profileVO);
        return Result.success();
    }

    /**
     * 获取授课班级列表
     */
    @GetMapping("/classes")
    public Result<List<ClassVO>> getMyClasses() {
        return Result.success(profileService.getMyClasses());
    }

    /**
     * 获取累计课时数
     */
    @GetMapping("/total-hours")
    public Result<Long> getTotalHours() {
        return Result.success(profileService.getTotalHours());
    }
}