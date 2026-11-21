package com.art.service.impl;

import com.art.entity.Student;
import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.entity.Feedback;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.FeedbackMapper;
import com.art.mapper.StudentMapper;
import com.art.mapper.TeacherMapper;
import com.art.service.TeacherFeedbackService;
import com.art.util.SecurityUtil;
import com.art.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherFeedbackServiceImpl implements TeacherFeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final CourseClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final com.art.service.MessageService messageService;

    private Set<Integer> myStudentIds() {
        Set<Integer> ids = new HashSet<>();
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return ids;
        for (CourseClass cc : classMapper.selectAll()) {
            if (teacherId.equals(cc.getTeacherId())) {
                for (Enrollment en : enrollmentMapper.selectByClassId(cc.getClassId())) {
                    ids.add(en.getStudentId());
                }
            }
        }
        return ids;
    }

    @Override
    public List<FeedbackVO> listMyFeedback(String status) {
        List<FeedbackVO> result = new ArrayList<>();
        Set<Integer> studentIds = myStudentIds();
        for (Feedback f : feedbackMapper.selectAll()) {
            if (!studentIds.contains(f.getStudentId())) continue;
            if (status != null && !status.isEmpty() && !status.equals(f.getStatus())) continue;
            result.add(toVO(f));
        }
        return result;
    }

    @Override
    public FeedbackVO getFeedbackDetail(Integer feedbackId) {
        Feedback f = feedbackMapper.selectById(feedbackId);
        return f != null ? toVO(f) : new FeedbackVO();
    }

    @Override
    public void replyFeedback(Integer feedbackId, String replyContent) {
        Feedback f = feedbackMapper.selectById(feedbackId);
        if (f == null) return;
        f.setReplyContent(replyContent);
        f.setReplyTime(LocalDateTime.now());
        f.setStatus("已回复");
        Integer tid = SecurityUtil.getCurrentTeacherId();
        com.art.entity.Teacher replyTeacher = tid != null ? teacherMapper.selectById(tid) : null;
        f.setReplyBy(replyTeacher != null ? replyTeacher.getName() : null);
        f.setReplyUserId(replyTeacher != null ? replyTeacher.getUserId() : null);
        feedbackMapper.update(f);
        Student stu = studentMapper.selectById(f.getStudentId());
        if (stu != null && stu.getUserId() != null) {
            messageService.send(stu.getUserId(), "反馈", "您的反馈已有回复",
                    "回复内容：" + replyContent, "feedback", feedbackId);
        }
    }

    private FeedbackVO toVO(Feedback f) {
        FeedbackVO vo = new FeedbackVO();
        vo.setFeedbackId(f.getFeedbackId());
        vo.setStudentId(f.getStudentId());
        vo.setStudentName(studentMapper.selectById(f.getStudentId()) != null
                ? studentMapper.selectById(f.getStudentId()).getName() : null);
        vo.setContent(f.getContent());
        vo.setType(f.getType());
        vo.setCreateTime(f.getCreateTime());
        vo.setStatus(f.getStatus());
        vo.setReplyContent(f.getReplyContent());
        vo.setReplyTime(f.getReplyTime());
        vo.setReplyBy(f.getReplyBy());
        return vo;
    }
}

