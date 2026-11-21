package com.art.service.impl;

import com.art.entity.Appointment;
import com.art.entity.Attendance;
import com.art.entity.Classroom;
import com.art.entity.Course;
import com.art.entity.CourseCategory;
import com.art.entity.CourseClass;
import com.art.entity.CourseSchedule;
import com.art.entity.Enrollment;
import com.art.entity.ExamProject;
import com.art.entity.ExamRegistration;
import com.art.entity.Feedback;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.entity.KbDoc;
import com.art.entity.KbOperationLog;
import com.art.entity.Message;
import com.art.entity.News;
import com.art.entity.Payment;
import com.art.entity.RescheduleRequest;
import com.art.entity.Student;
import com.art.entity.StudentComment;
import com.art.entity.StudentRequest;
import com.art.entity.SysUser;
import com.art.entity.SystemConfig;
import com.art.entity.Teacher;
import com.art.entity.VisitorBooking;
import com.art.mapper.AppointmentMapper;
import com.art.mapper.AttendanceMapper;
import com.art.mapper.ClassroomMapper;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseMapper;
import com.art.mapper.CourseScheduleMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.ExamProjectMapper;
import com.art.mapper.ExamRegistrationMapper;
import com.art.mapper.FeedbackMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbOperationLogMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.mapper.MessageMapper;
import com.art.mapper.NewsMapper;
import com.art.mapper.PaymentMapper;
import com.art.mapper.RescheduleRequestMapper;
import com.art.mapper.StudentCommentMapper;
import com.art.mapper.StudentMapper;
import com.art.mapper.StudentRequestMapper;
import com.art.mapper.SystemConfigMapper;
import com.art.mapper.TeacherMapper;
import com.art.mapper.UserMapper;
import com.art.mapper.VisitorBookingMapper;
import com.art.search.KbModelRecommendService;
import com.art.search.KbRelationService;
import com.art.search.KbVectorStore;
import com.art.service.KnowledgeAutoIngestService;
import com.art.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 知识库自动归集实现：按 source_ref 幂等去重/刷新，异常隔离不影响主业务。
 * 触发方式：业务实时钩子 + 应用启动首次归集 + 每3小时定时全量归集。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAutoIngestServiceImpl implements KnowledgeAutoIngestService {

    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;
    private final KbOperationLogMapper opLogMapper;
    /** 向量检索引擎：归集完成后增量刷新语义索引 */
    private final KbVectorStore vectorStore;
    /** 跨知识库关联映射：归集完成后重建关系 */
    private final KbRelationService relationService;
    /** 3D模型推荐引擎：归集/关系重建后重算条目-模型映射（推荐结果随内容变化） */
    private final KbModelRecommendService modelRecommendService;

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final CourseClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseCategoryMapper courseCategoryMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final TeacherMapper teacherMapper2;
    private final StudentMapper studentMapper2;
    private final UserMapper userMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final AttendanceMapper attendanceMapper;
    private final ExamProjectMapper examProjectMapper;
    private final ExamRegistrationMapper examRegistrationMapper;
    private final PaymentMapper paymentMapper;
    private final AppointmentMapper appointmentMapper;
    private final FeedbackMapper feedbackMapper;
    private final StudentCommentMapper studentCommentMapper;
    private final StudentRequestMapper studentRequestMapper;
    private final RescheduleRequestMapper rescheduleRequestMapper;
    private final VisitorBookingMapper visitorBookingMapper;
    private final NewsMapper newsMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final MessageMapper messageMapper;
    private final KnowledgeService knowledgeService;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private static final String OWNER_SYSTEM = "SYSTEM";
    private static final String OWNER_SHARED = "机构共享";

    // ==================== 实时钩子（业务触发，保持原有来源类型） ====================

    @Override
    public void ingestTeacherHomework(Homework homework) {
        try {
            if (homework == null || homework.getHomeworkId() == null) return;
            String sourceRef = "homework:" + homework.getHomeworkId();
            if (teacherMapper.selectBySourceRef("AUTO_ASSIGN", sourceRef) != null) return;

            Teacher teacher = teacherOfClass(homework.getClassId());

            KbDoc doc = new KbDoc();
            doc.setTitle("【作业教案】" + homework.getTitle());
            doc.setCategory("作业布置");
            doc.setContent("作业标题：" + homework.getTitle()
                    + "\n作业要求：" + nullSafe(homework.getContent())
                    + "\n参考视频：" + nullSafe(homework.getRefVideoName())
                    + "\n截止时间：" + (homework.getDeadline() != null ? homework.getDeadline().format(DAY) : "未设置"));
            doc.setSummary("班级作业要求归集：" + nullSafe(homework.getTitle()));
            doc.setTags("作业,教案,自动归集");
            doc.setSourceType("AUTO_ASSIGN");
            doc.setSourceRef(sourceRef);
            doc.setOwnerId(teacher != null ? teacher.getTeacherId() : null);
            doc.setOwnerName(teacher != null ? teacher.getName() : null);
            doc.setStatus(1);
            doc.setViewCount(0);
            doc.setCreateTime(LocalDateTime.now());
            teacherMapper.insert(doc);
            ingestLog(KnowledgeService.SCOPE_TEACHER, doc.getKbId(), "布置作业自动归集：" + homework.getTitle());
        } catch (Exception e) {
            log.warn("[知识库归集] 布置作业归集失败：{}", e.getMessage());
        }
    }

    @Override
    public void ingestTeacherGrade(HomeworkSubmission submission) {
        try {
            if (submission == null || submission.getSubmissionId() == null) return;
            String sourceRef = "submission:" + submission.getSubmissionId();
            if (teacherMapper.selectBySourceRef("AUTO_GRADE", sourceRef) != null) return;

            Homework hw = homeworkMapper.selectById(submission.getHomeworkId());
            Student stu = studentMapper2.selectById(submission.getStudentId());
            // 归属由数据链路显式推导：submission -> homework -> course_class.teacherId（不依赖 SecurityContext）
            Teacher teacher = hw != null ? teacherOfClass(hw.getClassId()) : null;

            KbDoc doc = new KbDoc();
            doc.setTitle("【批改点评】" + (hw != null ? hw.getTitle() : "作业")
                    + " · " + (stu != null ? stu.getName() : "学员"));
            doc.setCategory("批改点评");
            doc.setContent("作业标题：" + (hw != null ? hw.getTitle() : "-")
                    + "\n学员：" + (stu != null ? stu.getName() : "-")
                    + "\n得分：" + (submission.getScore() != null ? submission.getScore() : "-")
                    + "\n教师评语：" + nullSafe(submission.getTeacherComment()));
            doc.setSummary("作业批改经验归集：" + (hw != null ? hw.getTitle() : "作业"));
            doc.setTags("批改,点评,自动归集");
            doc.setSourceType("AUTO_GRADE");
            doc.setSourceRef(sourceRef);
            doc.setOwnerId(teacher != null ? teacher.getTeacherId() : null);
            doc.setOwnerName(teacher != null ? teacher.getName() : null);
            doc.setStatus(1);
            doc.setViewCount(0);
            doc.setCreateTime(LocalDateTime.now());
            teacherMapper.insert(doc);
            ingestLog(KnowledgeService.SCOPE_TEACHER, doc.getKbId(), "批改点评自动归集：" + (hw != null ? hw.getTitle() : "作业"));
        } catch (Exception e) {
            log.warn("[知识库归集] 批改点评归集失败：{}", e.getMessage());
        }
    }

    @Override
    public void ingestStudentSubmission(HomeworkSubmission submission) {
        try {
            if (submission == null || submission.getSubmissionId() == null) return;
            String sourceRef = "submission:" + submission.getSubmissionId();
            if (studentMapper.selectBySourceRef("AUTO_SUBMIT", sourceRef) != null) return;

            Homework hw = homeworkMapper.selectById(submission.getHomeworkId());
            Student stu = studentMapper2.selectById(submission.getStudentId());

            KbDoc doc = new KbDoc();
            doc.setTitle("【作业成果】" + (hw != null ? hw.getTitle() : "作业")
                    + " · " + (stu != null ? stu.getName() : "学员"));
            doc.setCategory("作业成果");
            doc.setContent("作业标题：" + (hw != null ? hw.getTitle() : "-")
                    + "\n学员：" + (stu != null ? stu.getName() : "-")
                    + "\n提交内容：" + nullSafe(submission.getContent())
                    + "\n提交附件：" + nullSafe(submission.getAttachmentName())
                    + "\n提交时间：" + (submission.getSubmitTime() != null ? submission.getSubmitTime().format(DAY) : "-"));
            doc.setSummary("学员作业成果归集：" + (hw != null ? hw.getTitle() : "作业"));
            doc.setTags("作业,学习成果,自动归集");
            doc.setSourceType("AUTO_SUBMIT");
            doc.setSourceRef(sourceRef);
            doc.setOwnerId(stu != null ? stu.getStudentId() : null);
            doc.setOwnerName(stu != null ? stu.getName() : null);
            doc.setStatus(1);
            doc.setViewCount(0);
            doc.setCreateTime(LocalDateTime.now());
            studentMapper.insert(doc);
            ingestLog(KnowledgeService.SCOPE_STUDENT, doc.getKbId(), "作业成果自动归集：" + (hw != null ? hw.getTitle() : "作业"));
        } catch (Exception e) {
            log.warn("[知识库归集] 作业成果归集失败：{}", e.getMessage());
        }
    }

    @Override
    public void ingestAdminDailyReport() {
        try {
            LocalDate today = LocalDate.now();
            String sourceRef = "daily_report:" + today.format(DAY_COMPACT);
            if (adminMapper.selectBySourceRef("AUTO_SCHEDULE", sourceRef) != null) return;

            Map<String, Object> stats = knowledgeService.stats();
            long todayIngest = countTodayIngest();

            StringBuilder content = new StringBuilder();
            content.append("报告日期：").append(today.format(DAY)).append("\n");
            content.append("\n== 三端知识库存量 ==\n");
            content.append("管理员端：").append(formatStats(stats, KnowledgeService.SCOPE_ADMIN)).append("\n");
            content.append("教师端：").append(formatStats(stats, KnowledgeService.SCOPE_TEACHER)).append("\n");
            content.append("学生端：").append(formatStats(stats, KnowledgeService.SCOPE_STUDENT)).append("\n");
            content.append("\n== 今日自动归集 ==\n");
            content.append("今日自动归集条目数：").append(todayIngest).append("\n");
            content.append("归集机制：业务实时钩子 + 启动首次归集 + 每3小时全量归集（三端知识库）");

            KbDoc doc = new KbDoc();
            doc.setTitle("【运营日报】知识库增长快照 " + today.format(DAY));
            doc.setCategory("日报快照");
            doc.setContent(content.toString());
            doc.setSummary("三端知识库每日增长快照与自动归集概况");
            doc.setTags("运营日报,知识库,自动归集");
            doc.setSourceType("AUTO_SCHEDULE");
            doc.setSourceRef(sourceRef);
            doc.setOwnerId(null);
            doc.setOwnerName(OWNER_SYSTEM);
            doc.setStatus(1);
            doc.setViewCount(0);
            doc.setCreateTime(LocalDateTime.now());
            adminMapper.insert(doc);
            ingestLog(KnowledgeService.SCOPE_ADMIN, doc.getKbId(), "运营日报自动归集：" + today.format(DAY));
        } catch (Exception e) {
            log.warn("[知识库归集] 运营日报归集失败：{}", e.getMessage());
        }
    }

    // ==================== 全量归集入口 ====================

    @Override
    public void ingestAllData() {
        long start = System.currentTimeMillis();
        int ok = 0;
        ok += run("课程产品", this::ingestCourses);
        ok += run("课程类别", this::ingestCategories);
        ok += run("教室资源", this::ingestClassrooms);
        ok += run("班级档案", this::ingestClasses);
        ok += run("排课课表", this::ingestSchedules);
        ok += run("教师档案", this::ingestTeachers);
        ok += run("学员档案", this::ingestStudents);
        ok += run("账号权限", this::ingestUsers);
        ok += run("在读报名", this::ingestEnrollments);
        ok += run("考勤记录", this::ingestAttendances);
        ok += run("作业教学", this::ingestHomeworks);
        ok += run("作业提交", this::ingestSubmissions);
        ok += run("考级比赛", this::ingestExamProjects);
        ok += run("考级报名", this::ingestExamRegistrations);
        ok += run("财务收支", this::ingestPayments);
        ok += run("预约管理", this::ingestAppointments);
        ok += run("学员反馈", this::ingestFeedbacks);
        ok += run("学员留言", this::ingestComments);
        ok += run("学员申请", this::ingestStudentRequests);
        ok += run("调课申请", this::ingestReschedules);
        ok += run("访客预约", this::ingestVisitorBookings);
        ok += run("公告资讯", this::ingestNews);
        ok += run("系统配置", this::ingestConfigs);
        ok += run("站内消息", this::ingestMessages);
        long ms = System.currentTimeMillis() - start;
        ingestLog(KnowledgeService.SCOPE_ADMIN, null, "全量同步完成：成功归集 " + ok + "/24 个业务域，耗时 " + ms + "ms");
        log.info("[知识库] 全量同步完成：{}/24 个业务域，耗时 {}ms", ok, ms);

        // 归集完成后：重建跨库关联映射 + 增量刷新向量索引 + 重算3D模型推荐（异常隔离，不影响归集主流程）
        try {
            relationService.build();
        } catch (Exception e) {
            log.warn("[知识库] 跨库关联重建失败：{}", e.getMessage());
        }
        try {
            vectorStore.refresh();
        } catch (Exception e) {
            log.warn("[知识库] 向量索引刷新失败：{}", e.getMessage());
        }
        try {
            modelRecommendService.rebuildAll();
        } catch (Exception e) {
            log.warn("[知识库] 3D模型推荐重算失败：{}", e.getMessage());
        }
    }

    private int run(String name, Runnable action) {
        try {
            action.run();
            return 1;
        } catch (Exception e) {
            log.warn("[知识库全量归集] {} 域归集失败：{}", name, e.getMessage());
            return 0;
        }
    }

    private void row(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[知识库全量归集] 单条归集失败：{}", e.getMessage());
        }
    }

    // ==================== 各业务域归集 ====================

    /** 课程产品：管理端明细 + 教师/学生端机构共享 */
    private void ingestCourses() {
        for (Course c : courseMapper.selectByCondition(null, null, null)) {
            row(() -> {
                String ref = "course:" + c.getCourseId();
                StringBuilder sb = new StringBuilder();
                line(sb, "课程编号", c.getCourseNo());
                line(sb, "课程名称", c.getCourseName());
                line(sb, "课程类别", c.getCategory());
                line(sb, "适用年龄段", c.getAgeGroup());
                line(sb, "难度级别", c.getLevel());
                line(sb, "总课时", c.getTotalHours());
                line(sb, "单次时长(分钟)", c.getLessonDuration());
                line(sb, "原价", c.getOriginalPrice());
                line(sb, "现价", c.getPrice());
                line(sb, "状态", c.getStatus() != null && c.getStatus() == 1 ? "上架" : "下架");
                line(sb, "创建日期", ds(c.getCreateTime()));
                line(sb, "课程简介", c.getIntro());
                String title = "【课程产品】" + nullSafe(c.getCourseName());
                String summary = "课程产品档案：" + nullSafe(c.getCourseName()) + "（" + nullSafe(c.getCategory()) + "）";
                String tags = "课程,课程产品,课程介绍,课程价格";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "课程产品", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_TEACHER, "课程产品", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
                put(KnowledgeService.SCOPE_STUDENT, "课程产品", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 课程类别：管理端明细 + 教师/学生端机构共享 */
    private void ingestCategories() {
        for (CourseCategory c : courseCategoryMapper.selectAll()) {
            row(() -> {
                String ref = "course_category:" + c.getCategoryId();
                StringBuilder sb = new StringBuilder();
                line(sb, "类别名称", c.getCategoryName());
                line(sb, "类别说明", c.getDescription());
                String title = "【课程类别】" + nullSafe(c.getCategoryName());
                String summary = "课程类别：" + nullSafe(c.getCategoryName());
                String tags = "课程,课程类别,课程分类";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "课程类别", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_TEACHER, "课程类别", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
                put(KnowledgeService.SCOPE_STUDENT, "课程类别", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 教室资源：管理端明细 + 教师/学生端机构共享 */
    private void ingestClassrooms() {
        for (Classroom r : classroomMapper.selectAll()) {
            row(() -> {
                String ref = "classroom:" + r.getClassroomId();
                StringBuilder sb = new StringBuilder();
                line(sb, "教室名称", r.getClassroomName());
                line(sb, "教室类型", r.getRoomType());
                line(sb, "容纳人数", r.getCapacity());
                line(sb, "状态", r.getStatus() != null && r.getStatus() == 1 ? "可用" : "停用");
                String title = "【教室资源】" + nullSafe(r.getClassroomName());
                String summary = "教室资源：" + nullSafe(r.getClassroomName()) + "（" + nullSafe(r.getRoomType()) + "）";
                String tags = "教室,教室资源,场地";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "教室资源", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_TEACHER, "教室资源", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
                put(KnowledgeService.SCOPE_STUDENT, "教室资源", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 班级档案：管理端明细 + 教师端(owner=teacherId) + 学生端机构共享 */
    private void ingestClasses() {
        for (CourseClass cc : classMapper.selectByCondition(null, null, null)) {
            row(() -> {
                String ref = "class:" + cc.getClassId();
                StringBuilder sb = new StringBuilder();
                line(sb, "班级名称", cc.getClassName());
                line(sb, "所属课程", cc.getCourseName());
                line(sb, "课程类别", cc.getCategory());
                line(sb, "授课教师", cc.getTeacherName());
                line(sb, "教室", cc.getClassroomName());
                line(sb, "学期", cc.getSemester());
                line(sb, "开课日期", ds(cc.getStartDate()));
                line(sb, "结课日期", ds(cc.getEndDate()));
                line(sb, "总课时", cc.getTotalHours());
                line(sb, "单次时长(分钟)", cc.getLessonDuration());
                line(sb, "学费", cc.getTuition());
                line(sb, "最大人数", cc.getMaxStudents());
                line(sb, "在读人数", cc.getCurrentStudents());
                line(sb, "状态", cc.getStatus() != null && cc.getStatus() == 1 ? "正常" : "停用");
                line(sb, "课程简介", cc.getDescription());
                String title = "【班级档案】" + nullSafe(cc.getClassName());
                String summary = "班级档案：" + nullSafe(cc.getClassName()) + "（教师 " + nullSafe(cc.getTeacherName()) + "）";
                String tags = "班级,班级档案,开班";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "班级档案", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                if (cc.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "班级档案", title, content, summary, tags, "AUTO_SYNC",
                            "class:" + cc.getClassId() + ":t" + cc.getTeacherId(), cc.getTeacherId(), teacherName(cc.getTeacherId()));
                }
                put(KnowledgeService.SCOPE_STUDENT, "班级档案", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 排课课表：管理端明细 + 教师端(owner=teacherId) + 学生端(owner=studentId，所在班级排课) */
    private void ingestSchedules() {
        for (CourseSchedule s : scheduleMapper.selectByConditions(null, null, null, null, null)) {
            row(() -> {
                CourseClass cc = s.getClassId() != null ? classMapper.selectById(s.getClassId()) : null;
                String cname = cc != null ? cc.getClassName() : "-";
                Integer teacherId = s.getTeacherId() != null ? s.getTeacherId()
                        : (cc != null ? cc.getTeacherId() : null);
                Teacher teacher = teacherId != null ? teacherMapper2.selectById(teacherId) : null;
                Classroom room = s.getClassroomId() != null ? classroomMapper.selectById(s.getClassroomId()) : null;

                StringBuilder sb = new StringBuilder();
                line(sb, "日期", ds(s.getCourseDate()));
                line(sb, "时间", ts(s.getStartTime()) + " - " + ts(s.getEndTime()));
                line(sb, "班级", cname);
                line(sb, "教师", teacher != null ? teacher.getName() : "-");
                line(sb, "教室", room != null ? room.getClassroomName() : "-");
                line(sb, "课时", s.getHours());
                line(sb, "状态", s.getStatus());
                line(sb, "备注", s.getRemark());
                String title = "【排课课表】" + ds(s.getCourseDate()) + " " + cname;
                String summary = "课程安排：" + ds(s.getCourseDate()) + " " + ts(s.getStartTime()) + "-" + ts(s.getEndTime()) + " " + cname;
                String tags = "课表,课程安排,排课,上课时间,课程时间";
                String content = sb.toString();
                String baseRef = "schedule:" + s.getScheduleId();

                put(KnowledgeService.SCOPE_ADMIN, "排课课表", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "排课课表", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + teacherId, teacherId, teacher != null ? teacher.getName() : null);
                }
                if (s.getClassId() != null) {
                    for (Enrollment e : enrollmentMapper.selectByClassId(s.getClassId())) {
                        row(() -> put(KnowledgeService.SCOPE_STUDENT, "排课课表", title, content, summary, tags, "AUTO_SYNC",
                                baseRef + ":s" + e.getStudentId(), e.getStudentId(), studentName(e.getStudentId())));
                    }
                }
            });
        }
    }

    /** 教师档案：仅管理端 */
    private void ingestTeachers() {
        for (Teacher t : teacherMapper2.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "工号", t.getTeacherNo());
                line(sb, "姓名", t.getName());
                line(sb, "性别", t.getGender());
                line(sb, "手机", t.getPhone());
                line(sb, "邮箱", t.getEmail());
                line(sb, "专业", t.getMajor());
                line(sb, "学历", t.getEducation());
                line(sb, "毕业院校", t.getSchool());
                line(sb, "职称", t.getTitle());
                line(sb, "用工类型", t.getEmployType());
                line(sb, "教龄(年)", t.getTeachYears());
                line(sb, "课时费", t.getHourlyRate());
                line(sb, "基本薪资", t.getBaseSalary());
                line(sb, "入职日期", ds(t.getHireDate()));
                line(sb, "状态", t.getStatus() != null && t.getStatus() == 1 ? "在职" : "离职");
                line(sb, "个人简介", t.getIntro());
                String title = "【教师档案】" + nullSafe(t.getName());
                String summary = "教师档案：" + nullSafe(t.getName()) + "（" + nullSafe(t.getMajor()) + "）";
                String tags = "教师,教师档案,师资";
                put(KnowledgeService.SCOPE_ADMIN, "教师档案", title, sb.toString(), summary, tags,
                        "AUTO_SYNC", "teacher:" + t.getTeacherId(), null, OWNER_SYSTEM);
            });
        }
    }

    /** 学员档案：管理端明细 + 学生端(owner=studentId) */
    private void ingestStudents() {
        for (Student s : studentMapper2.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学号", s.getStudentNo());
                line(sb, "姓名", s.getName());
                line(sb, "性别", s.getGender());
                line(sb, "家长电话", s.getParentPhone());
                line(sb, "邮箱", s.getEmail());
                line(sb, "入学日期", ds(s.getEnrollDate()));
                line(sb, "状态", s.getStatus() != null && s.getStatus() == 1 ? "在读" : "离校");
                String title = "【学员档案】" + nullSafe(s.getName());
                String summary = "学员档案：" + nullSafe(s.getName()) + "（" + nullSafe(s.getStudentNo()) + "）";
                String tags = "学员,学员档案,学生,我的信息,个人信息";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "学员档案", title, content, summary, tags, "AUTO_SYNC",
                        "student:" + s.getStudentId(), null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_STUDENT, "学员档案", title, content, summary, tags, "AUTO_SYNC",
                        "student:" + s.getStudentId() + ":s" + s.getStudentId(), s.getStudentId(), s.getName());
            });
        }
    }

    /** 用户账号：仅管理端，绝不写入 password */
    private void ingestUsers() {
        for (SysUser u : userMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "登录账号", u.getUsername());
                line(sb, "角色", roleLabel(u.getRole()));
                line(sb, "真实姓名", u.getRealName());
                line(sb, "手机", u.getPhone());
                line(sb, "状态", u.getStatus() != null && u.getStatus() == 1 ? "启用" : "禁用");
                String title = "【账号权限】" + nullSafe(u.getUsername()) + "（" + roleLabel(u.getRole()) + "）";
                String summary = "系统账号：" + nullSafe(u.getUsername()) + "，角色 " + roleLabel(u.getRole());
                String tags = "账号,权限,用户";
                put(KnowledgeService.SCOPE_ADMIN, "账号权限", title, sb.toString(), summary, tags,
                        "AUTO_SYNC", "user:" + u.getUserId(), null, OWNER_SYSTEM);
            });
        }
    }

    /** 在读报名：管理端明细 + 教师端(owner=班级teacherId) + 学生端(owner=studentId) */
    private void ingestEnrollments() {
        for (Enrollment e : enrollmentMapper.selectAll()) {
            row(() -> {
                CourseClass cc = e.getClassId() != null ? classMapper.selectById(e.getClassId()) : null;
                String cname = cc != null ? cc.getClassName() : "-";
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(e.getStudentId()));
                line(sb, "班级", cname);
                line(sb, "报名日期", ds(e.getEnrollDate()));
                line(sb, "学费", e.getTuitionAmount());
                line(sb, "已缴金额", e.getTotalPaid());
                line(sb, "出勤次数", e.getAttendedCount());
                line(sb, "缺勤次数", e.getAbsentCount());
                line(sb, "状态", e.getStatus());
                String title = "【在读报名】" + studentName(e.getStudentId()) + " · " + cname;
                String summary = "报名记录：" + studentName(e.getStudentId()) + " 报名 " + cname;
                String tags = "报名,在读报名,入学,报课,课程,班级";
                String content = sb.toString();
                String baseRef = "enrollment:" + e.getEnrollmentId();
                put(KnowledgeService.SCOPE_ADMIN, "在读报名", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (cc != null && cc.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "在读报名", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + cc.getTeacherId(), cc.getTeacherId(), teacherName(cc.getTeacherId()));
                }
                if (e.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "在读报名", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + e.getStudentId(), e.getStudentId(), studentName(e.getStudentId()));
                }
            });
        }
    }

    /** 考勤记录：管理端明细 + 教师端(owner=排课班级teacherId) + 学生端(owner=studentId) */
    private void ingestAttendances() {
        for (Attendance a : attendanceMapper.selectByConditions(null, null, null)) {
            row(() -> {
                CourseSchedule s = a.getScheduleId() != null ? scheduleMapper.selectById(a.getScheduleId()) : null;
                CourseClass cc = s != null && s.getClassId() != null ? classMapper.selectById(s.getClassId()) : null;
                Integer teacherId = s != null ? s.getTeacherId() : (cc != null ? cc.getTeacherId() : null);
                StringBuilder sb = new StringBuilder();
                line(sb, "日期", s != null ? ds(s.getCourseDate()) : ds(a.getRecordTime()));
                line(sb, "学员", studentName(a.getStudentId()));
                line(sb, "班级", cc != null ? cc.getClassName() : "-");
                line(sb, "考勤状态", a.getStatus());
                line(sb, "记录时间", a.getRecordTime() != null ? a.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
                line(sb, "备注", a.getRemark());
                String title = "【考勤记录】" + studentName(a.getStudentId()) + " · " + (s != null ? ds(s.getCourseDate()) : "-");
                String summary = "考勤：" + studentName(a.getStudentId()) + " " + nullSafe(a.getStatus());
                String tags = "考勤,考勤记录,出勤,缺勤";
                String content = sb.toString();
                String baseRef = "attendance:" + a.getAttendanceId();
                put(KnowledgeService.SCOPE_ADMIN, "考勤记录", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "考勤记录", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + teacherId, teacherId, teacherName(teacherId));
                }
                if (a.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "考勤记录", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + a.getStudentId(), a.getStudentId(), studentName(a.getStudentId()));
                }
            });
        }
    }

    /** 作业教学：教师端 AUTO_ASSIGN homework:{id}、学生端 AUTO_ASSIGN homework:{id}:s{studentId}、管理端 AUTO_SYNC homework:{id} */
    private void ingestHomeworks() {
        for (Homework hw : homeworkMapper.selectByConditions(null, null)) {
            row(() -> {
                CourseClass cc = hw.getClassId() != null ? classMapper.selectById(hw.getClassId()) : null;
                StringBuilder sb = new StringBuilder();
                line(sb, "作业标题", hw.getTitle());
                line(sb, "作业要求", hw.getContent());
                line(sb, "参考视频", hw.getRefVideoName());
                line(sb, "布置班级", cc != null ? cc.getClassName() : "-");
                line(sb, "截止时间", hw.getDeadline() != null ? hw.getDeadline().format(DAY) : "未设置");
                line(sb, "布置时间", ds(hw.getCreateTime()));
                String title = "【作业教案】" + nullSafe(hw.getTitle()) + " · " + (cc != null ? cc.getClassName() : "-");
                String summary = "作业布置：" + nullSafe(hw.getTitle());
                String tags = "作业,作业教学,教案";
                String content = sb.toString();
                put(KnowledgeService.SCOPE_ADMIN, "作业教学", title, content, summary, tags, "AUTO_SYNC",
                        "homework:" + hw.getHomeworkId(), null, OWNER_SYSTEM);
                Integer teacherId = cc != null ? cc.getTeacherId() : null;
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "作业教学", title, content, summary, tags, "AUTO_ASSIGN",
                            "homework:" + hw.getHomeworkId(), teacherId, teacherName(teacherId));
                }
                if (hw.getClassId() != null) {
                    for (Enrollment e : enrollmentMapper.selectByClassId(hw.getClassId())) {
                        row(() -> put(KnowledgeService.SCOPE_STUDENT, "作业教学", title, content, summary, tags, "AUTO_ASSIGN",
                                "homework:" + hw.getHomeworkId() + ":s" + e.getStudentId(), e.getStudentId(), studentName(e.getStudentId())));
                    }
                }
            });
        }
    }

    /** 作业提交：教师端 AUTO_GRADE submission:{id}、学生端 AUTO_SUBMIT submission:{id}、管理端 AUTO_SYNC submission:{id} */
    private void ingestSubmissions() {
        for (HomeworkSubmission sub : submissionMapper.selectAll()) {
            row(() -> {
                Homework hw = sub.getHomeworkId() != null ? homeworkMapper.selectById(sub.getHomeworkId()) : null;
                CourseClass cc = hw != null && hw.getClassId() != null ? classMapper.selectById(hw.getClassId()) : null;
                StringBuilder sb = new StringBuilder();
                line(sb, "作业标题", hw != null ? hw.getTitle() : "-");
                line(sb, "学员", studentName(sub.getStudentId()));
                line(sb, "班级", cc != null ? cc.getClassName() : "-");
                line(sb, "提交内容", sub.getContent());
                line(sb, "提交附件", sub.getAttachmentName());
                line(sb, "提交时间", sub.getSubmitTime() != null ? sub.getSubmitTime().format(DAY) : "-");
                line(sb, "得分", sub.getScore());
                line(sb, "教师评语", sub.getTeacherComment());
                line(sb, "状态", sub.getStatus());
                String title = "【作业提交】" + (hw != null ? nullSafe(hw.getTitle()) : "作业") + " · " + studentName(sub.getStudentId());
                String summary = "作业提交：" + (hw != null ? nullSafe(hw.getTitle()) : "作业") + " - " + studentName(sub.getStudentId());
                String tags = "作业,作业提交,作业成果,批改";
                String content = sb.toString();
                String ref = "submission:" + sub.getSubmissionId();
                put(KnowledgeService.SCOPE_ADMIN, "作业提交", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                Integer teacherId = cc != null ? cc.getTeacherId() : null;
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "作业提交", title, content, summary, tags, "AUTO_GRADE", ref, teacherId, teacherName(teacherId));
                }
                if (sub.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "作业提交", title, content, summary, tags, "AUTO_SUBMIT", ref, sub.getStudentId(), studentName(sub.getStudentId()));
                }
            });
        }
    }

    /** 考级比赛项目：管理端明细 + 教师/学生端机构共享 */
    private void ingestExamProjects() {
        for (ExamProject p : examProjectMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "活动名称", p.getName());
                line(sb, "活动类型", p.getActivityType());
                line(sb, "类别", p.getCategory());
                line(sb, "主办方", p.getOrganizer());
                line(sb, "报名开始", ds(p.getSignupStart()));
                line(sb, "报名截止", ds(p.getSignupEnd()));
                line(sb, "考试日期", ds(p.getExamDate()));
                line(sb, "考试时间", nullSafe(p.getExamStart()) + " - " + nullSafe(p.getExamEnd()));
                line(sb, "考场", p.getExamRoom());
                line(sb, "地点", p.getLocation());
                line(sb, "费用", p.getFee());
                line(sb, "报名要求", p.getRequirement());
                line(sb, "奖项说明", p.getAwardDesc());
                line(sb, "状态", p.getStatus());
                String title = "【考级比赛】" + nullSafe(p.getName());
                String summary = "考级比赛项目：" + nullSafe(p.getName()) + "（" + nullSafe(p.getActivityType()) + "）";
                String tags = "考级,比赛,考级比赛,报名";
                String content = sb.toString();
                String ref = "exam_project:" + p.getProjectId();
                put(KnowledgeService.SCOPE_ADMIN, "考级比赛", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_TEACHER, "考级比赛", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
                put(KnowledgeService.SCOPE_STUDENT, "考级比赛", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 考级报名：管理端明细 + 教师端(owner=teacherId) + 学生端(owner=studentId) */
    private void ingestExamRegistrations() {
        for (ExamRegistration r : examRegistrationMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "报考项目ID", r.getProjectId());
                line(sb, "学员", studentName(r.getStudentId()));
                line(sb, "报考姓名", r.getRealName());
                line(sb, "班级", className(r.getClassId()));
                line(sb, "报名时间", ds(r.getCreateTime()));
                line(sb, "状态", r.getStatus());
                line(sb, "考试时间", r.getExamTime());
                line(sb, "考场", r.getExamRoom());
                line(sb, "座位号", r.getSeatNo());
                line(sb, "成绩", r.getResult());
                line(sb, "奖项", r.getAward());
                line(sb, "证书编号", r.getCertificateNo());
                String title = "【考级报名】" + studentName(r.getStudentId()) + " · 项目" + r.getProjectId();
                String summary = "考级比赛报名：" + studentName(r.getStudentId()) + "（" + nullSafe(r.getStatus()) + "）";
                String tags = "考级,比赛,考级报名,报名";
                String content = sb.toString();
                String baseRef = "exam_reg:" + r.getRegId();
                put(KnowledgeService.SCOPE_ADMIN, "考级比赛", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (r.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "考级比赛", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + r.getTeacherId(), r.getTeacherId(), teacherName(r.getTeacherId()));
                }
                if (r.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "考级比赛", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + r.getStudentId(), r.getStudentId(), studentName(r.getStudentId()));
                }
            });
        }
    }

    /** 财务收支：管理端明细 + 学生端(owner=studentId) */
    private void ingestPayments() {
        for (Payment p : paymentMapper.selectByConditions(null, null, null, null, null, null)) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(p.getStudentId()));
                line(sb, "班级", className(p.getClassId()));
                line(sb, "缴费日期", ds(p.getPayDate()));
                line(sb, "金额", p.getAmount());
                line(sb, "费用类型", p.getPayType());
                line(sb, "支付方式", p.getPayMethod());
                line(sb, "状态", p.getStatus());
                line(sb, "备注", p.getRemark());
                String title = "【财务收支】" + studentName(p.getStudentId()) + " " + nullSafe(p.getPayType()) + " " + (p.getAmount() != null ? p.getAmount() : "-");
                String summary = "财务记录：" + studentName(p.getStudentId()) + " " + nullSafe(p.getPayType()) + " " + (p.getAmount() != null ? p.getAmount() + "元" : "");
                String tags = "财务,财务收支,缴费,金额";
                String content = sb.toString();
                String baseRef = "payment:" + p.getPaymentId();
                put(KnowledgeService.SCOPE_ADMIN, "财务收支", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (p.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "财务收支", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + p.getStudentId(), p.getStudentId(), studentName(p.getStudentId()));
                }
            });
        }
    }

    /** 预约：管理端明细 + 教师端(owner=teacherId) + 学生端(owner=studentId) */
    private void ingestAppointments() {
        for (Appointment a : appointmentMapper.selectByTeacher(null, null)) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(a.getStudentId()));
                line(sb, "教师", teacherName(a.getTeacherId()));
                line(sb, "班级", className(a.getClassId()));
                line(sb, "预约类型", a.getAppointmentType());
                line(sb, "预约时间", a.getAppointmentTime() != null ? a.getAppointmentTime().format(DAY) : "-");
                line(sb, "状态", a.getStatus());
                line(sb, "备注", a.getRemark());
                String title = "【预约管理】" + nullSafe(a.getAppointmentType()) + " " + studentName(a.getStudentId());
                String summary = "预约记录：" + studentName(a.getStudentId()) + " " + nullSafe(a.getAppointmentType()) + " " + nullSafe(a.getStatus());
                String tags = "预约,预约管理,试听,补课";
                String content = sb.toString();
                String baseRef = "appointment:" + a.getAppointmentId();
                put(KnowledgeService.SCOPE_ADMIN, "预约管理", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (a.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "预约管理", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + a.getTeacherId(), a.getTeacherId(), teacherName(a.getTeacherId()));
                }
                if (a.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "预约管理", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + a.getStudentId(), a.getStudentId(), studentName(a.getStudentId()));
                }
            });
        }
    }

    /** 学员反馈：管理端明细 + 教师端(owner=学员班级教师) + 学生端(owner=studentId) */
    private void ingestFeedbacks() {
        for (Feedback f : feedbackMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(f.getStudentId()));
                line(sb, "类型", f.getType());
                line(sb, "反馈内容", f.getContent());
                line(sb, "提交时间", ds(f.getCreateTime()));
                line(sb, "状态", f.getStatus());
                line(sb, "回复内容", f.getReplyContent());
                String title = "【学员反馈】" + nullSafe(f.getType()) + " · " + studentName(f.getStudentId());
                String summary = "学员反馈：" + studentName(f.getStudentId()) + " " + nullSafe(f.getType());
                String tags = "反馈,学员反馈,建议,投诉";
                String content = sb.toString();
                String baseRef = "feedback:" + f.getFeedbackId();
                put(KnowledgeService.SCOPE_ADMIN, "学员反馈", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                Integer teacherId = teacherIdOfStudent(f.getStudentId());
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "学员反馈", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + teacherId, teacherId, teacherName(teacherId));
                }
                if (f.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "学员反馈", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + f.getStudentId(), f.getStudentId(), studentName(f.getStudentId()));
                }
            });
        }
    }

    /** 学员留言：管理端明细 + 教师端(owner=teacherId) + 学生端(owner=studentId) */
    private void ingestComments() {
        for (StudentComment c : studentCommentMapper.selectByStudent(null, null)) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(c.getStudentId()));
                line(sb, "班级", className(c.getClassId()));
                line(sb, "教师", teacherName(c.getTeacherId()));
                line(sb, "留言内容", c.getContent());
                line(sb, "留言时间", ds(c.getCreateTime()));
                String title = "【学员留言】" + studentName(c.getStudentId()) + " · " + (c.getContent() != null && c.getContent().length() > 12 ? c.getContent().substring(0, 12) : nullSafe(c.getContent()));
                String summary = "学员留言：" + nullSafe(c.getContent());
                String tags = "留言,学员留言,师生互动";
                String content = sb.toString();
                String baseRef = "comment:" + c.getCommentId();
                put(KnowledgeService.SCOPE_ADMIN, "学员反馈", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (c.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "学员反馈", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + c.getTeacherId(), c.getTeacherId(), teacherName(c.getTeacherId()));
                }
                if (c.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "学员反馈", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + c.getStudentId(), c.getStudentId(), studentName(c.getStudentId()));
                }
            });
        }
    }

    /** 学员申请(请假/缓考)：管理端明细 + 教师端(owner=teacherId) + 学生端(owner=studentId) */
    private void ingestStudentRequests() {
        for (StudentRequest r : studentRequestMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "学员", studentName(r.getStudentId()));
                line(sb, "教师", teacherName(r.getTeacherId()));
                line(sb, "班级", className(r.getClassId()));
                line(sb, "申请类型", r.getRequestType());
                line(sb, "申请日期", ds(r.getRequestDate()));
                line(sb, "申请原因", r.getReason());
                line(sb, "状态", r.getStatus());
                line(sb, "处理意见", r.getReplyRemark());
                String title = "【学员申请】" + nullSafe(r.getRequestType()) + " · " + studentName(r.getStudentId());
                String summary = "学员申请：" + studentName(r.getStudentId()) + " " + nullSafe(r.getRequestType()) + " " + nullSafe(r.getStatus());
                String tags = "申请,学员申请,请假,缓考";
                String content = sb.toString();
                String baseRef = "srequest:" + r.getRequestId();
                put(KnowledgeService.SCOPE_ADMIN, "学员申请", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (r.getTeacherId() != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "学员申请", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + r.getTeacherId(), r.getTeacherId(), teacherName(r.getTeacherId()));
                }
                if (r.getStudentId() != null) {
                    put(KnowledgeService.SCOPE_STUDENT, "学员申请", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":s" + r.getStudentId(), r.getStudentId(), studentName(r.getStudentId()));
                }
            });
        }
    }

    /** 调课申请：管理端明细 + 教师端(owner=排课teacherId) */
    private void ingestReschedules() {
        for (RescheduleRequest r : rescheduleRequestMapper.selectAll()) {
            row(() -> {
                CourseSchedule s = r.getScheduleId() != null ? scheduleMapper.selectById(r.getScheduleId()) : null;
                Integer teacherId = s != null ? s.getTeacherId() : null;
                StringBuilder sb = new StringBuilder();
                line(sb, "原排课日期", s != null ? ds(s.getCourseDate()) : "-");
                line(sb, "申请类型", r.getRequestType());
                line(sb, "新日期", ds(r.getNewDate()));
                line(sb, "新时间", ts(r.getNewStartTime()) + " - " + ts(r.getNewEndTime()));
                line(sb, "申请原因", r.getReason());
                line(sb, "状态", r.getStatus());
                line(sb, "申请时间", ds(r.getApplyTime()));
                String title = "【调课申请】" + nullSafe(r.getRequestType()) + " " + (s != null ? ds(s.getCourseDate()) : "-");
                String summary = "调课申请：" + nullSafe(r.getRequestType()) + "（" + nullSafe(r.getStatus()) + "）";
                String tags = "调课,调课申请,换课";
                String content = sb.toString();
                String baseRef = "reschedule:" + r.getRequestId();
                put(KnowledgeService.SCOPE_ADMIN, "调课申请", title, content, summary, tags, "AUTO_SYNC", baseRef, null, OWNER_SYSTEM);
                if (teacherId != null) {
                    put(KnowledgeService.SCOPE_TEACHER, "调课申请", title, content, summary, tags, "AUTO_SYNC",
                            baseRef + ":t" + teacherId, teacherId, teacherName(teacherId));
                }
            });
        }
    }

    /** 访客预约：仅管理端 */
    private void ingestVisitorBookings() {
        for (VisitorBooking v : visitorBookingMapper.selectByCondition(null, null)) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "访客姓名", v.getName());
                line(sb, "联系电话", v.getPhone());
                line(sb, "意向班级", className(v.getClassId()));
                line(sb, "意向教师", teacherName(v.getTeacherId()));
                line(sb, "期望时间", v.getPreferredTime() != null ? v.getPreferredTime().format(DAY) : "-");
                line(sb, "备注", v.getRemark());
                line(sb, "状态", v.getStatus());
                line(sb, "提交时间", ds(v.getCreateTime()));
                String title = "【预约管理】访客预约 " + nullSafe(v.getName());
                String summary = "访客预约：" + nullSafe(v.getName()) + " " + nullSafe(v.getStatus());
                String tags = "预约,访客预约,试听";
                put(KnowledgeService.SCOPE_ADMIN, "预约管理", title, sb.toString(), summary, tags,
                        "AUTO_SYNC", "visitor:" + v.getBookingId(), null, OWNER_SYSTEM);
            });
        }
    }

    /** 公告资讯：管理端明细 + 教师/学生端机构共享 */
    private void ingestNews() {
        for (News n : newsMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "标题", n.getTitle());
                line(sb, "发布人", n.getPublisher());
                line(sb, "发布时间", ds(n.getPublishTime()));
                line(sb, "浏览量", n.getViewCount());
                line(sb, "内容", n.getContent());
                String title = "【公告资讯】" + nullSafe(n.getTitle());
                String summary = "公告资讯：" + nullSafe(n.getTitle());
                String tags = "公告,资讯,通知";
                String content = sb.toString();
                String ref = "news:" + n.getNewsId();
                put(KnowledgeService.SCOPE_ADMIN, "公告资讯", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SYSTEM);
                put(KnowledgeService.SCOPE_TEACHER, "公告资讯", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
                put(KnowledgeService.SCOPE_STUDENT, "公告资讯", title, content, summary, tags, "AUTO_SYNC", ref, null, OWNER_SHARED);
            });
        }
    }

    /** 系统配置：仅管理端，敏感值(key/secret/password)脱敏 */
    private void ingestConfigs() {
        for (SystemConfig c : systemConfigMapper.selectAll()) {
            row(() -> {
                String value = maskConfig(c.getConfigKey(), c.getConfigValue());
                StringBuilder sb = new StringBuilder();
                line(sb, "配置键", c.getConfigKey());
                line(sb, "配置值", value);
                String title = "【系统配置】" + nullSafe(c.getConfigKey());
                String summary = "系统配置项：" + nullSafe(c.getConfigKey());
                String tags = "配置,系统配置,参数";
                put(KnowledgeService.SCOPE_ADMIN, "系统配置", title, sb.toString(), summary, tags,
                        "AUTO_SYNC", "config:" + c.getConfigKey(), null, OWNER_SYSTEM);
            });
        }
    }

    /** 站内消息：仅管理端 */
    private void ingestMessages() {
        for (Message m : messageMapper.selectAll()) {
            row(() -> {
                StringBuilder sb = new StringBuilder();
                line(sb, "接收人ID", m.getReceiverUserId());
                line(sb, "消息类型", m.getMsgType());
                line(sb, "标题", m.getTitle());
                line(sb, "内容", m.getContent());
                line(sb, "是否已读", m.getIsRead() != null && m.getIsRead() == 1 ? "已读" : "未读");
                line(sb, "时间", ds(m.getCreateTime()));
                String title = "【站内消息】" + nullSafe(m.getTitle());
                String summary = "站内消息：" + nullSafe(m.getTitle());
                String tags = "消息,站内消息,通知";
                put(KnowledgeService.SCOPE_ADMIN, "站内消息", title, sb.toString(), summary, tags,
                        "AUTO_SYNC", "message:" + m.getMessageId(), null, OWNER_SYSTEM);
            });
        }
    }

    // ==================== 归属/投影/写入 基础设施 ====================

    /** 幂等 upsert：命中按 (sourceType, sourceRef) 更新刷新，未命中插入 */
    private void upsert(String scope, KbDoc doc) {
        if (doc == null || doc.getSourceType() == null || doc.getSourceRef() == null) return;
        KbDoc exist = selectByRef(scope, doc.getSourceType(), doc.getSourceRef());
        if (exist != null) {
            doc.setKbId(exist.getKbId());
            updateByScope(scope, doc);
        } else {
            doc.setCreateTime(LocalDateTime.now());
            insertByScope(scope, doc);
        }
    }

    private KbDoc selectByRef(String scope, String sourceType, String sourceRef) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectBySourceRef(sourceType, sourceRef);
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectBySourceRef(sourceType, sourceRef);
            default -> studentMapper.selectBySourceRef(sourceType, sourceRef);
        };
    }

    private void insertByScope(String scope, KbDoc doc) {
        switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.insert(doc);
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.insert(doc);
            default -> studentMapper.insert(doc);
        }
    }

    private void updateByScope(String scope, KbDoc doc) {
        switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.update(doc);
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.update(doc);
            default -> studentMapper.update(doc);
        }
    }

    /** 构造并 upsert 一条知识条目 */
    private void put(String scope, String category, String title, String content, String summary, String tags,
                     String sourceType, String sourceRef, Integer ownerId, String ownerName) {
        KbDoc doc = new KbDoc();
        doc.setCategory(category);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSummary(summary);
        doc.setTags(tags);
        doc.setSourceType(sourceType);
        doc.setSourceRef(sourceRef);
        doc.setOwnerId(ownerId);
        doc.setOwnerName(ownerName);
        doc.setStatus(1);
        doc.setViewCount(0);
        upsert(scope, doc);
    }

    private void line(StringBuilder sb, String label, Object value) {
        String v = value == null ? "-" : String.valueOf(value);
        if (v.isBlank()) v = "-";
        sb.append(label).append('：').append(v).append('\n');
    }

    private String ds(LocalDate v) {
        return v == null ? "-" : v.format(DAY);
    }

    private String ds(LocalDateTime v) {
        return v == null ? "-" : v.format(DAY);
    }

    private String ts(LocalTime v) {
        return v == null ? "-" : v.format(HM);
    }

    private Teacher teacherOfClass(Integer classId) {
        if (classId == null) return null;
        CourseClass cc = classMapper.selectById(classId);
        if (cc == null || cc.getTeacherId() == null) return null;
        return teacherMapper2.selectById(cc.getTeacherId());
    }

    /** 由学员在读报名链路推导其班级授课教师（第一个非空） */
    private Integer teacherIdOfStudent(Integer studentId) {
        if (studentId == null) return null;
        for (Enrollment e : enrollmentMapper.selectByStudentId(studentId)) {
            if (e.getClassId() == null) continue;
            CourseClass cc = classMapper.selectById(e.getClassId());
            if (cc != null && cc.getTeacherId() != null) return cc.getTeacherId();
        }
        return null;
    }

    private String studentName(Integer studentId) {
        if (studentId == null) return "-";
        Student s = studentMapper2.selectById(studentId);
        return s != null ? s.getName() : "-";
    }

    private String teacherName(Integer teacherId) {
        if (teacherId == null) return "-";
        Teacher t = teacherMapper2.selectById(teacherId);
        return t != null ? t.getName() : "-";
    }

    private String className(Integer classId) {
        if (classId == null) return "-";
        CourseClass c = classMapper.selectById(classId);
        return c != null ? c.getClassName() : "-";
    }

    private String roleLabel(String role) {
        if (role == null) return "-";
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学员";
            case "FINANCE" -> "财务";
            case "ACADEMIC" -> "教务";
            default -> role;
        };
    }

    /** 系统配置值脱敏：键名含 key/secret/password/token 时不落明文 */
    private String maskConfig(String key, String value) {
        if (value == null || value.isBlank()) return "-";
        if (key == null) return value;
        String k = key.toLowerCase();
        if (k.contains("password") || k.contains("secret") || k.contains("token") || k.contains("key")) {
            return "******（已脱敏）";
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private String formatStats(Map<String, Object> stats, String scope) {
        Object raw = stats.get(scope);
        if (!(raw instanceof Map)) return String.valueOf(raw);
        Map<String, Object> m = (Map<String, Object>) raw;
        return "共 " + m.getOrDefault("total", 0) + " 条（今日新增 " + m.getOrDefault("todayNew", 0)
                + "，累计自动归集 " + m.getOrDefault("autoCount", 0) + "）";
    }

    private long countTodayIngest() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<KbOperationLog> logs = opLogMapper.selectPage(null, "AUTO_INGEST");
        return logs.stream().filter(l -> l.getOpTime() != null && l.getOpTime().isAfter(todayStart)).count();
    }

    private void ingestLog(String scope, Integer kbId, String detail) {
        KbOperationLog logEntry = new KbOperationLog();
        logEntry.setKbScope(scope);
        logEntry.setKbId(kbId);
        logEntry.setOpType("AUTO_INGEST");
        logEntry.setOpUserId(null);
        logEntry.setOpUsername("SYSTEM");
        logEntry.setOpDetail(detail);
        logEntry.setOpTime(LocalDateTime.now());
        opLogMapper.insert(logEntry);
    }

    private String nullSafe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
