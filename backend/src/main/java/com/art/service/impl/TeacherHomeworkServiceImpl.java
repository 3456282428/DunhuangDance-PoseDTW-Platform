package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.dto.AssignHomeworkDTO;
import com.art.dto.GradeHomeworkDTO;
import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.mapper.StudentMapper;
import com.art.entity.Student;
import com.art.service.MessageService;
import com.art.service.KnowledgeAutoIngestService;
import com.art.service.TeacherHomeworkService;
import com.art.util.SecurityUtil;
import com.art.vo.HomeworkSubmissionVO;
import com.art.vo.HomeworkVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherHomeworkServiceImpl implements TeacherHomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseClassMapper classMapper;
    private final MessageService messageService;
    private final KnowledgeAutoIngestService knowledgeAutoIngestService;

    @Override
    public void assignHomework(AssignHomeworkDTO assignDTO) {
        Homework h = new Homework();
        h.setClassId(assignDTO.getClassId());
        h.setScheduleId(assignDTO.getScheduleId());
        h.setTitle(assignDTO.getTitle());
        h.setContent(assignDTO.getContent());
        h.setRefVideoUrl(assignDTO.getRefVideoUrl());
        h.setRefVideoName(assignDTO.getRefVideoName());
        h.setCreateTime(LocalDateTime.now());
        h.setDeadline(assignDTO.getDeadline());
        homeworkMapper.insert(h);
        // 知识库自然增长：布置作业自动归集至教师端知识库（作业教案）
        knowledgeAutoIngestService.ingestTeacherHomework(h);
        // 为该班每位在读学员创建一条"未提交"的提交记录，便于统计完成率
        List<Enrollment> enrollments = enrollmentMapper.selectByClassId(assignDTO.getClassId());
        for (Enrollment en : enrollments) {
            HomeworkSubmission sub = new HomeworkSubmission();
            sub.setHomeworkId(h.getHomeworkId());
            sub.setStudentId(en.getStudentId());
            sub.setStatus("未提交");
            submissionMapper.insert(sub);
        }
    }

    @Override
    public PageResult<HomeworkVO> pageMyHomework(Integer pageNum, Integer pageSize, Integer classId, String status) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        java.util.Set<Integer> myClassIds = new java.util.HashSet<>();
        for (CourseClass cc : classMapper.selectAll()) {
            if (teacherId != null && teacherId.equals(cc.getTeacherId())) myClassIds.add(cc.getClassId());
        }
        LocalDateTime now = LocalDateTime.now();
        List<HomeworkVO> filtered = new ArrayList<>();
        for (Homework h : homeworkMapper.selectByConditions(classId, null)) {
            if (!myClassIds.contains(h.getClassId())) continue;
            String dlStatus = (h.getDeadline() == null || h.getDeadline().isAfter(now)) ? "未截止" : "已截止";
            if (status != null && !status.isEmpty() && !status.equals(dlStatus)) continue;
            HomeworkVO vo = toVO(h);
            vo.setDeadlineStatus(dlStatus);
            filtered.add(vo);
        }
        filtered.sort((a, b) -> b.getHomeworkId().compareTo(a.getHomeworkId()));
        int total = filtered.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<HomeworkVO> pageList = from >= total ? new ArrayList<>() : filtered.subList(from, to);
        return PageResult.of((long) total, new ArrayList<>(pageList), pageNum, pageSize);
    }

    @Override
    public List<HomeworkSubmissionVO> getSubmissions(Integer homeworkId) {
        List<HomeworkSubmissionVO> result = new ArrayList<>();
        Homework base = homeworkMapper.selectById(homeworkId);
        if (base == null) return result;
        for (HomeworkSubmission s : submissionMapper.selectByHomeworkId(homeworkId)) {
            result.add(toSubVO(s, base.getTitle()));
        }
        return result;
    }

    @Override
    public void gradeHomework(Integer submissionId, GradeHomeworkDTO gradeDTO) {
        HomeworkSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException("作业提交记录不存在");
        sub.setScore(gradeDTO.getScore());
        sub.setTeacherComment(gradeDTO.getTeacherComment());
        sub.setStatus(com.art.common.BizConstants.HS_GRADED);
        if (sub.getSubmitTime() == null) sub.setSubmitTime(LocalDateTime.now());
        submissionMapper.update(sub);

        // 知识库自然增长：批改点评自动归集至教师端知识库
        knowledgeAutoIngestService.ingestTeacherGrade(sub);

        // 通知学员作业已批改
        Homework hw = homeworkMapper.selectById(sub.getHomeworkId());
        Student stu = studentMapper.selectById(sub.getStudentId());
        if (stu != null && stu.getUserId() != null) {
            messageService.send(stu.getUserId(), "作业", "作业已批改",
                    (hw != null ? "【" + hw.getTitle() + "】" : "您的作业")
                            + "已批改，得分：" + gradeDTO.getScore()
                            + (gradeDTO.getTeacherComment() != null && !gradeDTO.getTeacherComment().isBlank()
                            ? "，评语：" + gradeDTO.getTeacherComment() : ""),
                    "homework_submission", submissionId);
        }
    }

    @Override
    public HomeworkSubmissionVO getStudentHomework(Integer homeworkId, Integer studentId) {
        HomeworkSubmission sub = submissionMapper.selectByHomeworkAndStudent(homeworkId, studentId);
        if (sub == null) return null;
        Homework base = homeworkMapper.selectById(homeworkId);
        return toSubVO(sub, base != null ? base.getTitle() : null);
    }

    private HomeworkVO toVO(Homework h) {
        HomeworkVO vo = new HomeworkVO();
        vo.setHomeworkId(h.getHomeworkId());
        vo.setClassId(h.getClassId());
        CourseClass cc = h.getClassId() != null ? classMapper.selectById(h.getClassId()) : null;
        vo.setClassName(cc != null ? cc.getClassName() : null);
        vo.setScheduleId(h.getScheduleId());
        vo.setTitle(h.getTitle());
        vo.setContent(h.getContent());
        vo.setRefVideoUrl(h.getRefVideoUrl());
        vo.setRefVideoName(h.getRefVideoName());
        vo.setCreateTime(h.getCreateTime());
        vo.setDeadline(h.getDeadline());
        List<HomeworkSubmission> subs = submissionMapper.selectByHomeworkId(h.getHomeworkId());
        vo.setTotalStudents(subs.size());
        int submitted = 0;
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        int scored = 0;
        for (HomeworkSubmission s : subs) {
            if ("已提交".equals(s.getStatus()) || "已批改".equals(s.getStatus())) submitted++;
            if (s.getScore() != null) { sum = sum.add(s.getScore()); scored++; }
        }
        vo.setSubmittedCount(submitted);
        vo.setAvgScore(scored > 0
                ? sum.divide(java.math.BigDecimal.valueOf(scored), 1, java.math.RoundingMode.HALF_UP)
                : null);
        return vo;
    }

    private HomeworkSubmissionVO toSubVO(HomeworkSubmission s, String title) {
        HomeworkSubmissionVO vo = new HomeworkSubmissionVO();
        vo.setSubmissionId(s.getSubmissionId());
        vo.setHomeworkId(s.getHomeworkId());
        vo.setStudentId(s.getStudentId());
        vo.setStudentName(studentMapper.selectById(s.getStudentId()) != null
                ? studentMapper.selectById(s.getStudentId()).getName() : null);
        vo.setTitle(title);
        vo.setContent(s.getContent());
        vo.setAttachmentUrl(s.getAttachmentUrl());
        vo.setAttachmentName(s.getAttachmentName());
        vo.setSubmitTime(s.getSubmitTime());
        vo.setScore(s.getScore());
        vo.setTeacherComment(s.getTeacherComment());
        vo.setStatus(s.getStatus());
        vo.setAiScore(s.getAiScore());
        vo.setAiReport(s.getAiReport());
        vo.setAiStatus(s.getAiStatus());
        vo.setAiScoredAt(s.getAiScoredAt());
        return vo;
    }
}
