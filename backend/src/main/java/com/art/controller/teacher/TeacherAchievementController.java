package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherAchievementService;
import com.art.vo.AchievementStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 - 教学成果统计
 * 统计学员考级通过率、比赛获奖、作业完成情况
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/achievement")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAchievementController {

    private final TeacherAchievementService achievementService;

    /**
     * 获取教学成果总览（各项统计数字）
     */
    @GetMapping("/stats")
    public Result<AchievementStatsVO> getAchievementStats() {
        return Result.success(achievementService.getAchievementStats());
    }

    /**
     * 按班级统计考级通过率
     */
    @GetMapping("/exam-pass-rate")
    public Result<List<Map<String, Object>>> getExamPassRateByClass() {
        return Result.success(achievementService.getExamPassRateByClass());
    }

    /**
     * 按班级统计比赛获奖情况
     */
    @GetMapping("/competition-awards")
    public Result<List<Map<String, Object>>> getCompetitionAwardsByClass() {
        return Result.success(achievementService.getCompetitionAwardsByClass());
    }

    /**
     * 作业完成率统计
     */
    @GetMapping("/homework-completion")
    public Result<Map<String, Object>> getHomeworkCompletionRate() {
        return Result.success(achievementService.getHomeworkCompletionRate());
    }
}