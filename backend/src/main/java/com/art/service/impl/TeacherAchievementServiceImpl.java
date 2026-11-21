package com.art.service.impl;

import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.service.TeacherAchievementService;
import com.art.util.SecurityUtil;
import com.art.vo.AchievementStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherAchievementServiceImpl implements TeacherAchievementService {

    private final CourseClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final com.art.mapper.ExamRegistrationMapper regMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;

    private List<Integer> myClassIds() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        List<Integer> ids = new ArrayList<>();
        if (teacherId == null) return ids;
        for (CourseClass cc : classMapper.selectAll()) {
            if (teacherId.equals(cc.getTeacherId())) ids.add(cc.getClassId());
        }
        return ids;
    }

    private long activeStudentCount(Integer classId) {
        return enrollmentMapper.selectByClassId(classId).stream()
                .filter(e -> e.getStatus() == null || "在读".equals(e.getStatus()))
                .count();
    }

    private int[] homeworkProgress(List<Integer> classIds) {
        int expected = 0, submitted = 0;
        for (Integer cid : classIds) {
            List<Homework> hws = homeworkMapper.selectByClassId(cid);
            if (hws.isEmpty()) continue;
            long students = activeStudentCount(cid);
            for (Homework h : hws) {
                expected += students;
                for (HomeworkSubmission s : submissionMapper.selectByHomeworkId(h.getHomeworkId())) {
                    if ("已提交".equals(s.getStatus()) || "已批改".equals(s.getStatus())) submitted++;
                }
            }
        }
        return new int[]{expected, submitted};
    }

    @Override
    public AchievementStatsVO getAchievementStats() {
        AchievementStatsVO vo = new AchievementStatsVO();
        List<Integer> classIds = myClassIds();
        int examCount = 0, examPassed = 0, competitionCount = 0, awardCount = 0;
        Set<Integer> studentSet = new HashSet<>();
        for (Integer cid : classIds) {
            for (Enrollment e : enrollmentMapper.selectByClassId(cid)) {
                if (e.getStatus() == null || "在读".equals(e.getStatus())) studentSet.add(e.getStudentId());
            }
            for (com.art.vo.ExamAchievementVO ec : regMapper.selectAchievementsByClass(cid, null)) {
                if ("考级".equals(ec.getActivityType())) {
                    examCount++;
                    if ("通过".equals(ec.getResult())) examPassed++;
                } else if ("比赛".equals(ec.getActivityType())) {
                    competitionCount++;
                    if (ec.getAward() != null && !ec.getAward().isEmpty()) awardCount++;
                }
            }
        }
        int[] hp = homeworkProgress(classIds);
        vo.setExamCount(examCount);
        vo.setExamPassed(examPassed);
        vo.setExamPassRate(examCount > 0 ? Math.round(examPassed * 100.0 / examCount * 100.0) / 100.0 : 0.0);
        vo.setCompetitionCount(competitionCount);
        vo.setAwardCount(awardCount);
        vo.setHomeworkCompletionRate(hp[0] > 0 ? Math.round(hp[1] * 100.0 / hp[0] * 100.0) / 100.0 : 0.0);
        vo.setTotalStudents(studentSet.size());
        return vo;
    }

    @Override
    public List<Map<String, Object>> getExamPassRateByClass() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer cid : myClassIds()) {
            CourseClass cc = classMapper.selectById(cid);
            if (cc == null) continue;
            List<com.art.vo.ExamAchievementVO> exams = regMapper.selectAchievementsByClass(cid, null);
            long total = exams.stream().filter(e -> "考级".equals(e.getActivityType())).count();
            long passed = exams.stream().filter(e -> "考级".equals(e.getActivityType()) && "通过".equals(e.getResult())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("classId", cid);
            row.put("className", cc.getClassName());
            row.put("examCount", total);
            row.put("passed", passed);
            row.put("passRate", total > 0 ? Math.round(passed * 100.0 / total * 100.0) / 100.0 : 0.0);
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCompetitionAwardsByClass() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer cid : myClassIds()) {
            CourseClass cc = classMapper.selectById(cid);
            if (cc == null) continue;
            List<com.art.vo.ExamAchievementVO> comps = regMapper.selectAchievementsByClass(cid, null);
            long total = comps.stream()
                    .filter(e -> "比赛".equals(e.getActivityType())).count();
            long awarded = comps.stream()
                    .filter(e -> "比赛".equals(e.getActivityType()) && e.getAward() != null && !e.getAward().isEmpty()).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("classId", cid);
            row.put("className", cc.getClassName());
            row.put("competitionCount", total);
            row.put("awardCount", awarded);
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getHomeworkCompletionRate() {
        int[] hp = homeworkProgress(myClassIds());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("homeworkTotal", hp[0]);
        result.put("homeworkSubmitted", hp[1]);
        result.put("completionRate", hp[0] > 0 ? Math.round(hp[1] * 100.0 / hp[0] * 100.0) / 100.0 : 0.0);
        return result;
    }
}
