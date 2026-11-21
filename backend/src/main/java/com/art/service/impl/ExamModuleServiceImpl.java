package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.BusinessException;
import com.art.common.MobileValidator;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.ExamModuleService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class ExamModuleServiceImpl implements ExamModuleService {

    private final ExamProjectMapper projectMapper;
    private final ExamRegistrationMapper regMapper;
    private final StudentMapper studentMapper;
    private final PaymentMapper paymentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseClassMapper classMapper;
    private final com.art.service.MessageService messageService;

    @Override
    public List<Map<String,Object>> listProjects(String activityType, String category, String status, String keyword) {
        return projectMapper.selectByCondition(activityType, category, status, keyword);
    }

    @Override
    public void createProject(ExamProject p) {
        if (p.getName() == null || p.getName().isBlank()) throw new BusinessException("项目名称不能为空");
        if (p.getActivityType() == null) p.setActivityType("考级");
        if (p.getStatus() == null) p.setStatus("报名中");
        if (p.getFee() == null) p.setFee(BigDecimal.ZERO);
        projectMapper.insert(p);
    }

    @Override
    public void updateProject(ExamProject p) {
        if (p.getProjectId() == null || projectMapper.selectById(p.getProjectId()) == null)
            throw new BusinessException("项目不存在");
        projectMapper.update(p);
    }

    @Override
    public void deleteProject(Integer projectId) {
        List<Map<String,Object>> regs = regMapper.selectByCondition(projectId, null, null, null);
        if (regs != null && !regs.isEmpty()) throw new BusinessException("该项目已有报名记录，不能删除，可改为已结束");
        projectMapper.deleteById(projectId);
    }

    @Override
    public List<Map<String,Object>> listRegistrations(Integer projectId, Integer studentId, String status, String activityType) {
        return regMapper.selectByCondition(projectId, studentId, status, activityType);
    }

    @Override
    public void register(ExamRegistration form) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) throw new BusinessException("请使用学员账号登录");
        if (form.getProjectId() == null) throw new BusinessException("请选择报名项目");
        ExamProject project = projectMapper.selectById(form.getProjectId());
        if (project == null) throw new BusinessException("项目不存在");
        if ("已结束".equals(project.getStatus())) throw new BusinessException("该项目已结束报名");
        if (regMapper.selectByProjectStudent(form.getProjectId(), studentId) != null)
            throw new BusinessException("您已报名该项目，请勿重复报名");

        Student s = studentMapper.selectById(studentId);
        form.setStudentId(studentId);
        form.setRegId(null);
        if (form.getRealName() == null || form.getRealName().isBlank()) form.setRealName(s.getName());
        if (form.getPhone() == null || form.getPhone().isBlank()) form.setPhone(s.getParentPhone());
        MobileValidator.requireNonNull(form.getPhone(), "联系电话");
        if (form.getGender() == null || form.getGender().isBlank()) form.setGender(s.getGender());
        form.setStatus(BizConstants.EXAM_PENDING);
        regMapper.insert(form);
    }

    @Override
    public Map<String,Object> lookupStudent(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) throw new BusinessException("请输入学号");
        Student s = studentMapper.selectByStudentNo(studentNo.trim());
        if (s == null) throw new BusinessException("未找到该学号对应的学员");
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("studentId", s.getStudentId());
        m.put("studentNo", s.getStudentNo());
        m.put("name", s.getName());
        List<Map<String,Object>> classes = new ArrayList<>();
        for (Enrollment e : enrollmentMapper.selectByStudentId(s.getStudentId())) {
            if (e.getStatus() != null && !"在读".equals(e.getStatus())) continue;
            CourseClass cc = classMapper.selectById(e.getClassId());
            Map<String,Object> cm = new LinkedHashMap<>();
            cm.put("classId", e.getClassId());
            cm.put("className", cc != null ? cc.getClassName() : ("班级" + e.getClassId()));
            classes.add(cm);
        }
        m.put("classes", classes);
        return m;
    }

    @Override
    public void recommend(String studentNo, Integer classId, Integer projectId, String comment) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) throw new BusinessException("请使用教师账号登录");
        if (studentNo == null || studentNo.isBlank() || projectId == null) throw new BusinessException("参数不完整");
        if (projectMapper.selectById(projectId) == null) throw new BusinessException("项目不存在");
        Student s = studentMapper.selectByStudentNo(studentNo.trim());
        if (s == null) throw new BusinessException("未找到该学号对应的学员");
        ExamRegistration exist = regMapper.selectByProjectStudent(projectId, s.getStudentId());
        if (exist != null) throw new BusinessException("该学员已报名此项目");
        ExamRegistration r = new ExamRegistration();
        r.setProjectId(projectId);
        r.setStudentId(s.getStudentId());
        r.setClassId(classId);
        r.setRealName(s.getName());
        r.setPhone(s.getParentPhone());
        r.setGender(s.getGender());
        r.setTeacherId(teacherId);
        r.setTeacherComment(comment);
        r.setStatus(BizConstants.EXAM_PENDING);
        regMapper.insert(r);
    }

    @Override
    public void audit(Integer regId, Boolean pass, String remark) {
        ExamRegistration r = mustReg(regId);
        if (!BizConstants.EXAM_PENDING.equals(r.getStatus()) && !BizConstants.EXAM_REJECTED.equals(r.getStatus()))
            throw new BusinessException("当前状态不能审核");
        r.setStatus(Boolean.TRUE.equals(pass) ? BizConstants.EXAM_CONFIRMED : BizConstants.EXAM_REJECTED);
        r.setAuditRemark(remark);
        regMapper.update(r);
        Student s = studentMapper.selectById(r.getStudentId());
        ExamProject p = projectMapper.selectById(r.getProjectId());
        if (s != null && s.getUserId() != null) {
            String title = (Boolean.TRUE.equals(pass) ? "考级/比赛报名已通过" : "考级/比赛报名未通过");
            String content = (p != null ? "【" + p.getName() + "】" : "")
                    + (Boolean.TRUE.equals(pass) ? "审核通过，请及时缴费。" : "审核未通过。")
                    + (remark != null && !remark.isBlank() ? " 备注：" + remark : "");
            messageService.send(s.getUserId(), "考级", title, content, "exam_registration", regId);
        }
    }

    @Override
    public void pay(Integer regId, String payMethod) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        ExamRegistration r = mustReg(regId);
        if (studentId != null && !studentId.equals(r.getStudentId())) throw new BusinessException("不能操作他人报名");
        if (!BizConstants.EXAM_CONFIRMED.equals(r.getStatus())) throw new BusinessException("仅审核通过的报名可缴费");
        ExamProject project = projectMapper.selectById(r.getProjectId());
        r.setStatus(BizConstants.EXAM_PAID);
        regMapper.update(r);
        // 同步财务：生成一笔考级/比赛缴费订单
        Payment pay = new Payment();
        pay.setStudentId(r.getStudentId());
        pay.setClassId(r.getClassId());
        pay.setPayDate(LocalDateTime.now());
        pay.setAmount(project.getFee() != null ? project.getFee() : BigDecimal.ZERO);
        pay.setPayType("考级".equals(project.getActivityType()) ? "考级" : "比赛");
        pay.setPayMethod(payMethod != null ? payMethod : "微信");
        pay.setStatus(BizConstants.PAY_PAID);
        pay.setRegId(regId);
        pay.setRemark(project.getName() + " 报名缴费");
        paymentMapper.insert(pay);
    }

    @Override
    public void arrange(Integer regId) {
        ExamRegistration r = mustReg(regId);
        ExamProject p = projectMapper.selectById(r.getProjectId());
        if (p == null) throw new BusinessException("项目不存在");
        if (p.getExamDate() == null || p.getExamStart() == null || p.getExamEnd() == null
                || p.getExamRoom() == null || p.getExamRoom().isBlank())
            throw new BusinessException("请先在项目管理中完善考试日期、起止时间与考场");
        // 考试时间、考场固定与项目一致
        String examTime = p.getExamDate() + " " + p.getExamStart() + "-" + p.getExamEnd();
        r.setExamTime(examTime);
        r.setExamRoom(p.getExamRoom());
        // 同一项目（同一时间段、同一考场）已占用座位
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (Map<String,Object> m : regMapper.selectByCondition(r.getProjectId(), null, null, null)) {
            Object seat = m.get("seat_no"); if (seat == null) seat = m.get("seatNo");
            if (seat != null) try { used.add(Integer.valueOf(String.valueOf(seat).replaceAll("\\D",""))); } catch(Exception ignore){}
        }
        used.remove(null);
        // 在 1..n+1 顺序号中随机取一个未占用号（顺序范围、随机分配）
        java.util.List<Integer> pool = new java.util.ArrayList<>();
        for (int i=1;i<=used.size()+1;i++) if (!used.contains(i)) pool.add(i);
        int seat = pool.get(new java.util.Random().nextInt(pool.size()));
        r.setSeatNo(String.valueOf(seat));
        regMapper.update(r);
    }

    @Override
    public void enterResult(Integer regId, String result, String award, String certificateNo) {
        ExamRegistration r = mustReg(regId);
        r.setResult(result);
        r.setAward(award);
        r.setCertificateNo(certificateNo);
        if (result != null && !result.isBlank()) r.setStatus(BizConstants.EXAM_FINISHED);
        regMapper.update(r);

        Student s = studentMapper.selectById(r.getStudentId());
        if (s != null && s.getUserId() != null && result != null && !result.isBlank()) {
            messageService.send(s.getUserId(), "考级", "考级/比赛成绩已发布",
                    "您的成绩：" + result + (award != null && !award.isBlank() ? "，奖项：" + award : ""),
                    "exam_registration", regId);
        }
    }

    @Override
    public void cancel(Integer regId) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        ExamRegistration r = mustReg(regId);
        if (studentId != null && !studentId.equals(r.getStudentId())) throw new BusinessException("不能操作他人报名");
        if (BizConstants.EXAM_PAID.equals(r.getStatus()) || BizConstants.EXAM_FINISHED.equals(r.getStatus()))
            throw new BusinessException("已缴费/已完成的报名请联系机构处理");
        r.setStatus(BizConstants.EXAM_CANCELED);
        regMapper.update(r);
    }

    @Override
    public Map<String,Object> stats() {
        List<Map<String,Object>> all = regMapper.selectByCondition(null, null, null, null);
        Map<String,Object> m = new LinkedHashMap<>();
        long total = all.size();
        long evaluated = all.stream().filter(x -> {
            String res = str(x.get("result")); return res != null && !res.isBlank();
        }).count();
        long pass = all.stream().filter(x -> {
            String res = str(x.get("result"));
            return res != null && (res.contains("通过") || res.contains("获奖") || res.contains("优秀") || res.contains("良"));
        }).count();
        long award = all.stream().filter(x -> x.get("award") != null && !str(x.get("award")).isBlank()).count();
        m.put("totalReg", total);
        m.put("finished", evaluated);
        m.put("passCount", pass);
        m.put("awardCount", award);
        m.put("passRate", evaluated > 0 ? Math.round(pass * 1000.0 / evaluated) / 10.0 : 0);
        return m;
    }

    private ExamRegistration mustReg(Integer regId) {
        if (regId == null) throw new BusinessException("报名ID不能为空");
        ExamRegistration r = regMapper.selectById(regId);
        if (r == null) throw new BusinessException("报名记录不存在");
        return r;
    }

    private String str(Object o) { return o == null ? null : String.valueOf(o); }
}
