package com.art.service;

import com.art.vo.AchievementStatsVO;
import java.util.List;
import java.util.Map;

/**
 * @author liukang&&heyan
 */
public interface TeacherAchievementService {
    AchievementStatsVO getAchievementStats();
    List<Map<String, Object>> getExamPassRateByClass();
    List<Map<String, Object>> getCompetitionAwardsByClass();
    Map<String, Object> getHomeworkCompletionRate();
}