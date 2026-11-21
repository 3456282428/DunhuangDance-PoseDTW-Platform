package com.art.service.impl;

import com.art.common.MobileValidator;
import com.art.dto.FeedbackDTO;
import com.art.dto.StudentProfileDTO;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.StudentProfileService;
import com.art.util.SecurityUtil;
import com.art.vo.*;
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
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentMapper studentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseClassMapper classMapper;
    private final AttendanceMapper attendanceMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final ExamRegistrationMapper regMapper;
    private final NewsMapper newsMapper;
    private final FeedbackMapper feedbackMapper;
    private final SystemConfigMapper configMapper;

    @Override
    public StudentProfileDTO getMyProfile() {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        StudentProfileDTO dto = new StudentProfileDTO();
        if (studentId == null) return dto;
        Student s = studentMapper.selectById(studentId);
        if (s == null) return dto;
        dto.setStudentId(s.getStudentId());
        dto.setStudentNo(s.getStudentNo());
        dto.setName(s.getName());
        dto.setGender(s.getGender());
        if (s.getBirthDate() != null) dto.setBirthDate(s.getBirthDate());
        dto.setParentPhone(s.getParentPhone());
        dto.setEmail(s.getEmail());
        return dto;
    }

    @Override
    public void updateProfile(StudentProfileDTO profileDTO) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return;
        Student s = studentMapper.selectById(studentId);
        if (s == null) return;
        if (profileDTO.getName() != null) s.setName(profileDTO.getName());
        if (profileDTO.getGender() != null) s.setGender(profileDTO.getGender());
        if (profileDTO.getBirthDate() != null) s.setBirthDate(profileDTO.getBirthDate());
        if (profileDTO.getParentPhone() != null) {
            MobileValidator.require(profileDTO.getParentPhone(), "家长电话");
            s.setParentPhone(profileDTO.getParentPhone());
        }
        if (profileDTO.getEmail() != null) s.setEmail(profileDTO.getEmail());
        studentMapper.update(s);
    }

    @Override
    public LearningProgressVO getLearningProgress() {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        LearningProgressVO vo = new LearningProgressVO();
        if (studentId == null) return vo;
        List<Enrollment> enrolls = enrollmentMapper.selectByStudentId(studentId);
        vo.setTotalCourses(enrolls.size());
        int totalHours = 0, attendedHours = 0, hwTotal = 0, hwSubmitted = 0;
        for (Enrollment en : enrolls) {
            CourseClass cc = classMapper.selectById(en.getClassId());
            if (cc != null && cc.getTotalHours() != null) totalHours += cc.getTotalHours();
            if (en.getAttendedCount() != null) attendedHours += en.getAttendedCount();
        }
        vo.setTotalHours(totalHours);
        vo.setAttendedHours(attendedHours);

        int attended = 0, late = 0, absent = 0, leave = 0;
        for (Attendance a : attendanceMapper.selectByConditions(null, null, studentId)) {
            switch (a.getStatus() == null ? "" : a.getStatus()) {
                case "出勤": attended++; break;
                case "迟到": late++; break;
                case "缺勤": absent++; break;
                case "请假": leave++; break;
                default: break;
            }
        }
        int attTotal = attended + late + absent + leave;
        vo.setAttendanceRate(attTotal > 0 ? Math.round((attended + late) * 100.0 / attTotal * 100.0) / 100.0 : 0.0);

        List<HomeworkSubmission> hws = submissionMapper.selectByStudentId(studentId);
        hwTotal = hws.size();
        for (HomeworkSubmission h : hws) {
            if ("已提交".equals(h.getStatus()) || "已批改".equals(h.getStatus())) hwSubmitted++;
        }
        vo.setHomeworkTotal(hwTotal);
        vo.setHomeworkSubmitted(hwSubmitted);

        List<com.art.vo.ExamAchievementVO> exams = regMapper.selectAchievementsByStudent(studentId, null);
        long passed = exams.stream().filter(e -> "通过".equals(e.getResult()) || (e.getAward() != null && !e.getAward().isEmpty())).count();
        vo.setExamTotal(exams.size());
        vo.setExamPassed((int) passed);

        List<Map<String, Object>> recent = new ArrayList<>();
        for (Attendance a : attendanceMapper.selectByConditions(null, null, studentId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", "考勤");
            row.put("detail", a.getStatus());
            row.put("time", a.getRecordTime());
            recent.add(row);
        }
        for (com.art.vo.ExamAchievementVO ec : exams) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", ec.getActivityType());
            row.put("detail", ec.getActivityName() + " " + (ec.getResult() == null ? "" : ec.getResult()));
            row.put("time", ec.getActivityDate());
            recent.add(row);
        }
        vo.setRecentActivities(recent);
        return vo;
    }

    @Override
    public List<NewsVO> getNewsList() {
        List<NewsVO> result = new ArrayList<>();
        for (News n : newsMapper.selectAll()) {
            NewsVO vo = new NewsVO();
            vo.setNewsId(n.getNewsId());
            vo.setTitle(n.getTitle());
            vo.setContent(n.getContent());
            vo.setPublisher(n.getPublisher());
            vo.setPublishTime(n.getPublishTime());
            vo.setViewCount(n.getViewCount());
            result.add(vo);
        }
        return result;
    }

    @Override
    public OrganizationVO getOrganizationInfo() {
        OrganizationVO vo = new OrganizationVO();
        // 初始种子数据使用独立键：org_name/org_address/org_phone/org_intro/logo_url/org_wechat
        vo.setName(cfg("org_name"));
        vo.setAddress(cfg("org_address"));
        vo.setPhone(cfg("org_phone"));
        vo.setEmail(cfg("org_email"));
        vo.setWechat(cfg("org_wechat"));
        vo.setIntroduction(cfg("org_intro"));
        vo.setLogoUrl(cfg("logo_url"));
        // 管理员端“系统基础设置-机构信息”保存的 organization JSON 优先覆盖
        try {
            SystemConfig orgCfg = configMapper.selectByKey("organization");
            if (orgCfg != null && orgCfg.getConfigValue() != null && !orgCfg.getConfigValue().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> org = mapper.readValue(orgCfg.getConfigValue(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (notBlank(org.get("name"))) vo.setName(str(org.get("name")));
                if (notBlank(org.get("shortName"))) vo.setShortName(str(org.get("shortName")));
                if (notBlank(org.get("address"))) vo.setAddress(str(org.get("address")));
                if (notBlank(org.get("phone"))) vo.setPhone(str(org.get("phone")));
                if (notBlank(org.get("email"))) vo.setEmail(str(org.get("email")));
                if (notBlank(org.get("wechat"))) vo.setWechat(str(org.get("wechat")));
                if (notBlank(org.get("introduction"))) vo.setIntroduction(str(org.get("introduction")));
                if (notBlank(org.get("logoUrl"))) vo.setLogoUrl(str(org.get("logoUrl")));
            }
        } catch (Exception ignored) {
        }
        return vo;
    }

    private boolean notBlank(Object o) {
        return o != null && !String.valueOf(o).trim().isEmpty();
    }

    private String cfg(String key) {
        SystemConfig c = configMapper.selectByKey(key);
        if (c == null) return null;
        String v = c.getConfigValue();
        return (v == null || v.isEmpty()) ? null : v;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public void submitFeedback(FeedbackDTO feedbackDTO) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        Feedback f = new Feedback();
        f.setStudentId(studentId);
        f.setContent(feedbackDTO.getContent());
        f.setType(feedbackDTO.getType() == null ? "建议" : feedbackDTO.getType());
        f.setStatus("待处理");
        f.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(f);
    }

    @Override
    public List<FeedbackVO> listMyFeedback() {
        List<FeedbackVO> result = new ArrayList<>();
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (studentId == null) return result;
        for (Feedback f : feedbackMapper.selectByStudentId(studentId)) {
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
            result.add(vo);
        }
        return result;
    }
}
