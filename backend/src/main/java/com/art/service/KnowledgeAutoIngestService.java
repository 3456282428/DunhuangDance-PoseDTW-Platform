package com.art.service;

import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;

/**
 * 知识库自动归集服务：业务数据自动化、常态化汇入三端知识库（自然增长机制）
 * 归集失败不影响主业务流程
  * @author liukang&&heyan
 */
public interface KnowledgeAutoIngestService {

    /** 教师布置作业 → 教师端知识库（作业教案归集） */
    void ingestTeacherHomework(Homework homework);

    /** 教师批改作业 → 教师端知识库（批改点评经验归集） */
    void ingestTeacherGrade(HomeworkSubmission submission);

    /** 学员提交作业 → 学生端知识库（作业成果归集） */
    void ingestStudentSubmission(HomeworkSubmission submission);

    /** 管理员端运营日报 → 管理员端知识库（每日定时归集） */
    void ingestAdminDailyReport();

    /** 全量归集：按业务域将系统各业务表数据幂等归集进三端知识库（启动首次 + 每3小时定时） */
    void ingestAllData();
}
