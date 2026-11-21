package com.art.service.impl;

import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.TeacherStudentProgressService;
import com.art.util.SecurityUtil;
import com.art.vo.AttendanceStudentVO;
import com.art.vo.HomeworkSubmissionVO;
import com.art.vo.StudentProgressDetailVO;
import com.art.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherStudentProgressServiceImpl implements TeacherStudentProgressService {

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseClassMapper classMapper;
    private final AttendanceMapper attendanceMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final ExamRegistrationMapper regMapper;
    private final StudentCommentMapper commentMapper;

    @Override
    public List<StudentProgressVO> getClassStudentsProgress(Integer classId) {
        List<StudentProgressVO> result = new ArrayList<>();
        CourseClass cc = classMapper.selectById(classId);
        if (cc == null) return result;
        for (Enrollment en : enrollmentMapper.selectByClassId(classId)) {
            Integer sid = en.getStudentId();
            StudentProgressVO vo = new StudentProgressVO();
            vo.setStudentId(sid);
            vo.setStudentName(studentMapper.selectById(sid) != null ? studentMapper.selectById(sid).getName() : null);
            vo.setClassId(classId);
            vo.setClassName(cc.getClassName());
            vo.setTotalHours(cc.getTotalHours());
            vo.setAttendedHours(en.getAttendedCount() != null ? en.getAttendedCount() : 0);
            vo.setAbsentCount(en.getAbsentCount() != null ? en.getAbsentCount() : 0);
            int totalAttend = en.getAttendedCount() + en.getAbsentCount();
            vo.setAttendanceRate(totalAttend > 0 ? Math.round(en.getAttendedCount() * 100.0 / totalAttend * 100.0) / 100.0 : 0.0);
            int hwTotal = 0, submitted = 0;
            for (Homework h : homeworkMapper.selectByClassId(classId)) {
                hwTotal++;
                HomeworkSubmission s = submissionMapper.selectByHomeworkAndStudent(h.getHomeworkId(), sid);
                if (s != null && ("已提交".equals(s.getStatus()) || "已批改".equals(s.getStatus()))) submitted++;
            }
            vo.setHomeworkTotal(hwTotal);
            vo.setHomeworkSubmitted(submitted);
            vo.setHomeworkRate(hwTotal > 0 ? Math.round(submitted * 100.0 / hwTotal * 100.0) / 100.0 : 0.0);
            List<com.art.vo.ExamAchievementVO> exams = regMapper.selectAchievementsByStudent(sid, null);
            long passed = exams.stream().filter(e -> "通过".equals(e.getResult()) || (e.getAward() != null && !e.getAward().isEmpty())).count();
            vo.setExamTotal(exams.size());
            vo.setExamPassed((int) passed);
            List<StudentComment> comments = commentMapper.selectByStudent(sid, classId);
            vo.setLatestComment(!comments.isEmpty() ? comments.get(0).getContent() : null);
            result.add(vo);
        }
        return result;
    }

    @Override
    public StudentProgressDetailVO getStudentProgressDetail(Integer studentId, Integer classId) {
        StudentProgressDetailVO vo = new StudentProgressDetailVO();
        Student st = studentMapper.selectById(studentId);
        CourseClass cc = classMapper.selectById(classId);
        vo.setStudentId(studentId);
        vo.setStudentName(st != null ? st.getName() : null);
        vo.setClassId(classId);
        vo.setClassName(cc != null ? cc.getClassName() : null);

        List<AttendanceStudentVO> attList = new ArrayList<>();
        for (Attendance a : attendanceMapper.selectByConditions(classId, null, studentId)) {
            AttendanceStudentVO aVo = new AttendanceStudentVO();
            aVo.setStudentId(studentId);
            aVo.setStudentName(st != null ? st.getName() : null);
            aVo.setStatus(a.getStatus());
            aVo.setRemark(a.getRemark());
            attList.add(aVo);
        }
        vo.setAttendanceRecords(attList);

        List<HomeworkSubmissionVO> hwList = new ArrayList<>();
        for (Homework h : homeworkMapper.selectByClassId(classId)) {
            HomeworkSubmission s = submissionMapper.selectByHomeworkAndStudent(h.getHomeworkId(), studentId);
            HomeworkSubmissionVO hVo = new HomeworkSubmissionVO();
            hVo.setSubmissionId(s != null ? s.getSubmissionId() : null);
            hVo.setHomeworkId(h.getHomeworkId());
            hVo.setStudentId(studentId);
            hVo.setStudentName(st != null ? st.getName() : null);
            hVo.setTitle(h.getTitle());
            hVo.setContent(s != null ? s.getContent() : null);
            hVo.setAttachmentUrl(s != null ? s.getAttachmentUrl() : null);
            hVo.setAttachmentName(s != null ? s.getAttachmentName() : null);
            hVo.setSubmitTime(s != null ? s.getSubmitTime() : null);
            hVo.setScore(s != null ? s.getScore() : null);
            hVo.setTeacherComment(s != null ? s.getTeacherComment() : null);
            hVo.setStatus(s != null ? s.getStatus() : "未提交");
            hwList.add(hVo);
        }
        vo.setHomeworkRecords(hwList);

        List<Map<String, Object>> examList = new ArrayList<>();
        for (com.art.vo.ExamAchievementVO ec : regMapper.selectAchievementsByStudent(studentId, null)) {
            if (classId != null && !classId.equals(ec.getClassId())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("activityId", ec.getRegId());
            row.put("activityType", ec.getActivityType());
            row.put("activityName", ec.getActivityName());
            row.put("activityDate", ec.getActivityDate());
            row.put("result", ec.getResult());
            row.put("award", ec.getAward());
            examList.add(row);
        }
        vo.setExamCompetitionRecords(examList);

        List<String> comments = new ArrayList<>();
        for (StudentComment c : commentMapper.selectByStudent(studentId, classId)) {
            comments.add(c.getContent());
        }
        vo.setComments(comments);
        return vo;
    }

    @Override
    public void addProgressComment(Integer studentId, Integer classId, String comment) {
        StudentComment sc = new StudentComment();
        sc.setStudentId(studentId);
        sc.setClassId(classId);
        sc.setTeacherId(SecurityUtil.getCurrentTeacherId());
        sc.setContent(comment);
        sc.setCreateTime(LocalDateTime.now());
        commentMapper.insert(sc);
    }
}
