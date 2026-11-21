package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.dto.SubmitHomeworkDTO;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.service.KnowledgeAutoIngestService;
import com.art.service.StudentHomeworkService;
import com.art.util.SecurityUtil;
import com.art.vo.StudentHomeworkVO;
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
public class StudentHomeworkServiceImpl implements StudentHomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final KnowledgeAutoIngestService knowledgeAutoIngestService;

    @Override
    public PageResult<StudentHomeworkVO> pageMyHomeworks(Integer pageNum, Integer pageSize, String status) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        List<StudentHomeworkVO> all = homeworkMapper.selectStudentHomeworks(studentId);
        List<StudentHomeworkVO> filtered = new ArrayList<>();
        for (StudentHomeworkVO vo : all) {
            if (status != null && !status.isEmpty() && !status.equals(vo.getStatus())) {
                continue;
            }
            filtered.add(vo);
        }
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of((long) filtered.size(), filtered, pageNum, pageSize);
    }

    @Override
    public void submitHomework(Integer homeworkId, SubmitHomeworkDTO submitDTO) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        HomeworkSubmission sub = submissionMapper.selectByHomeworkAndStudent(homeworkId, studentId);
        if (sub == null) {
            sub = new HomeworkSubmission();
            sub.setHomeworkId(homeworkId);
            sub.setStudentId(studentId);
            sub.setStatus("已提交");
            sub.setSubmitTime(LocalDateTime.now());
            sub.setContent(submitDTO.getContent());
            sub.setAttachmentUrl(submitDTO.getAttachmentUrl());
            sub.setAttachmentName(submitDTO.getAttachmentName());
            submissionMapper.insert(sub);
        } else {
            sub.setContent(submitDTO.getContent());
            sub.setAttachmentUrl(submitDTO.getAttachmentUrl());
            sub.setAttachmentName(submitDTO.getAttachmentName());
            sub.setSubmitTime(LocalDateTime.now());
            sub.setStatus("已提交");
            submissionMapper.update(sub);
        }
        // 知识库自然增长：作业提交自动归集至学生端知识库（作业成果）
        HomeworkSubmission latest = submissionMapper.selectByHomeworkAndStudent(homeworkId, studentId);
        if (latest != null) {
            knowledgeAutoIngestService.ingestStudentSubmission(latest);
        }
    }

    @Override
    public StudentHomeworkVO getHomeworkDetail(Integer homeworkId) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        List<StudentHomeworkVO> all = homeworkMapper.selectStudentHomeworks(studentId);
        for (StudentHomeworkVO vo : all) {
            if (vo.getHomeworkId().equals(homeworkId)) {
                return vo;
            }
        }
        return null;
    }
}
