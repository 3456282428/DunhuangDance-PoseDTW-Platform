package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.dto.DashboardOverviewDTO;
import com.art.entity.Attendance;
import com.art.entity.Classroom;
import com.art.entity.CourseSchedule;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.entity.Payment;
import com.art.entity.Teacher;
import com.art.mapper.*;
import com.art.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class StatisticsServiceImpl implements StatisticsService {

    private final CourseScheduleMapper scheduleMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final AttendanceMapper attendanceMapper;
    private final PaymentMapper paymentMapper;
    private final ClassroomMapper classroomMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final ExamRegistrationMapper regMapper;

    @Override
    public DashboardOverviewDTO getDashboardOverview() {
        DashboardOverviewDTO dto = new DashboardOverviewDTO();
        LocalDate today = LocalDate.now();
        int todayCourse = 0;
        for (CourseSchedule cs : scheduleMapper.selectByDateRange(today, today)) todayCourse++;
        dto.setTodayCourseCount(todayCourse);

        int pending = 0;
        for (HomeworkSubmission h : submissionMapper.selectByStatus("已提交")) pending++;
        dto.setPendingTodoCount(pending);

        Object ar = getAttendanceRate(null, null, null).get("attendanceRate");
        dto.setAttendanceRate(ar instanceof Number ? ((Number) ar).doubleValue() : 0.0);

        BigDecimal todayRevenue = BigDecimal.ZERO;
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        for (Payment p : paymentMapper.selectByConditions(null, null, start, end, null, null)) {
            todayRevenue = todayRevenue.add(signedAmount(p));
        }
        dto.setTodayRevenue(todayRevenue);

        BigDecimal monthRevenue = BigDecimal.ZERO;
        LocalDateTime mStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime mEnd = today.plusDays(1).atStartOfDay();
        for (Payment p : paymentMapper.selectByConditions(null, null, mStart, mEnd, null, null)) {
            monthRevenue = monthRevenue.add(signedAmount(p));
        }
        dto.setMonthRevenue(monthRevenue);
        Object rv = getRenewalRate(null, null).get("renewalRate");
        dto.setRenewalRate(rv instanceof Number ? ((Number) rv).doubleValue() : 0.0);

        int totalStudents = 0, totalTeachers = 0;
        for (com.art.entity.Student s : studentMapper.selectAll()) if (s.getStatus() != null && s.getStatus() == 1) totalStudents++;
        for (Teacher t : teacherMapper.selectAll()) if (t.getStatus() != null && t.getStatus() == 1) totalTeachers++;
        dto.setTotalStudents(totalStudents);
        dto.setTotalTeachers(totalTeachers);

        // 本月新增学员（按入学日期）
        LocalDate monthStart = today.withDayOfMonth(1);
        int newStudents = 0;
        for (com.art.entity.Student s : studentMapper.selectAll()) {
            if (s.getEnrollDate() != null
                    && !s.getEnrollDate().isBefore(monthStart)
                    && !s.getEnrollDate().isAfter(today)) newStudents++;
        }
        dto.setNewStudentsThisMonth(newStudents);

        // 本月教师课时汇总 & 授课最多教师
        java.util.Map<Integer, Double> hoursByTeacher = new java.util.HashMap<>();
        double monthTeacherHours = 0;
        for (CourseSchedule cs : scheduleMapper.selectByDateRange(monthStart, today)) {
            if (cs.getTeacherId() == null || cs.getHours() == null
                    || "已取消".equals(cs.getStatus())) continue;
            double h = cs.getHours();
            hoursByTeacher.merge(cs.getTeacherId(), h, Double::sum);
            monthTeacherHours += h;
        }
        dto.setMonthTeacherHours(Math.round(monthTeacherHours * 10) / 10.0);
        java.util.Map.Entry<Integer, Double> top = null;
        for (java.util.Map.Entry<Integer, Double> e : hoursByTeacher.entrySet()) {
            if (top == null || e.getValue() > top.getValue()) top = e;
        }
        if (top != null) {
            Teacher tt = teacherMapper.selectById(top.getKey());
            String label = tt == null ? "教师" + top.getKey()
                    : ((tt.getTeacherNo() != null ? tt.getTeacherNo() + " " : "") + tt.getName());
            dto.setTopTeacherName(label);
            dto.setTopTeacherHours(Math.round(top.getValue() * 10) / 10.0);
        }
        return dto;
    }

    private BigDecimal signedAmount(Payment p) {
        if (p == null || p.getAmount() == null) return BigDecimal.ZERO;
        String s = p.getStatus();
        if ("已退款".equals(s)) return p.getAmount().negate();
        if ("已支付".equals(s)) return p.getAmount();
        return BigDecimal.ZERO; // 待支付/退款中/已取消 不计入收入
    }

    /**
     * 续课率（真实续报口径）
     * 分母：统计区间内到达续报节点的学员（其某门报名记录的班级最后一课日期，即课程到期日落入区间；退学不计）。
     * 分子：分母学员中，在到期日前后窗口（前30天~后60天）内再次报名其他班级，或产生“续费”已支付记录的学员。
     * 续课率 = 已续学员数 / 应续学员数 × 100%（按学员去重，同一学员只计一次）。
     * 不传日期区间时按全部历史计算。
     * 另附“多课学员占比”作为辅助指标（报读2个及以上班级的学员/有报名记录学员）。
     */
    @Override
    public Map<String, Object> getRenewalRate(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        final int BEFORE_DAYS = 30, AFTER_DAYS = 60;

        // 1. 每个班级的最后一课日期（课程到期日，已取消排课不计）
        java.util.Map<Integer, LocalDate> classLastDate = new java.util.HashMap<>();
        for (CourseSchedule cs : scheduleMapper.selectByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31))) {
            if (cs.getClassId() == null || cs.getCourseDate() == null
                    || BizConstants.SCHED_CANCELED.equals(cs.getStatus())) continue;
            LocalDate exist = classLastDate.get(cs.getClassId());
            if (exist == null || cs.getCourseDate().isAfter(exist)) classLastDate.put(cs.getClassId(), cs.getCourseDate());
        }

        // 2. 学员 -> 报名记录；学员 -> 全部续费支付时间
        java.util.Map<Integer, List<com.art.entity.Enrollment>> enByStudent = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> enrollCount = new java.util.HashMap<>();
        for (com.art.entity.Enrollment en : enrollmentMapper.selectAll()) {
            enByStudent.computeIfAbsent(en.getStudentId(), k -> new ArrayList<>()).add(en);
            enrollCount.merge(en.getStudentId(), 1, Integer::sum);
        }
        java.util.Map<Integer, List<LocalDate>> renewPayByStudent = new java.util.HashMap<>();
        for (Payment p : paymentMapper.selectByConditions(null, null, null, null, null, null)) {
            if (!"已支付".equals(p.getStatus()) || !"续费".equals(p.getPayType())
                    || p.getPayDate() == null) continue;
            renewPayByStudent.computeIfAbsent(p.getStudentId(), k -> new ArrayList<>()).add(p.getPayDate().toLocalDate());
        }

        // 3. 判定每位学员是否“应续”及“已续”
        // 不传区间时默认统计“截至今天已到期”的学员，避免把未结课的在读学员算入应续
        LocalDate effEnd = endDate != null ? endDate : LocalDate.now();
        java.util.Set<Integer> dueStudents = new java.util.TreeSet<>();
        java.util.Set<Integer> renewedStudents = new java.util.TreeSet<>();
        for (java.util.Map.Entry<Integer, List<com.art.entity.Enrollment>> e : enByStudent.entrySet()) {
            Integer sid = e.getKey();
            boolean due = false, renewed = false;
            for (com.art.entity.Enrollment en : e.getValue()) {
                if ("退学".equals(en.getStatus())) continue;
                LocalDate dueDate = en.getClassId() != null ? classLastDate.get(en.getClassId()) : null;
                if (dueDate == null) dueDate = en.getEnrollDate(); // 无排课时退回报名日期
                // 已标记结课但班级仍有后续排课时，以统计截止日作为到期日
                if ("结课".equals(en.getStatus()) && dueDate != null && dueDate.isAfter(effEnd)) dueDate = effEnd;
                if (dueDate == null) continue;
                if (startDate != null && dueDate.isBefore(startDate)) continue;
                if (dueDate.isAfter(effEnd)) continue;
                due = true;
                // 续报判定：窗口内报名了其他班级，或有续费支付
                LocalDate winStart = dueDate.minusDays(BEFORE_DAYS);
                LocalDate winEnd = dueDate.plusDays(AFTER_DAYS);
                for (com.art.entity.Enrollment other : e.getValue()) {
                    if (other.getEnrollmentId().equals(en.getEnrollmentId()) || other.getEnrollDate() == null) continue;
                    if (!other.getEnrollDate().isBefore(winStart) && !other.getEnrollDate().isAfter(winEnd)) {
                        renewed = true; break;
                    }
                }
                if (!renewed) {
                    for (LocalDate pd : renewPayByStudent.getOrDefault(sid, java.util.Collections.emptyList())) {
                        if (!pd.isBefore(winStart) && !pd.isAfter(winEnd)) { renewed = true; break; }
                    }
                }
            }
            if (due) {
                dueStudents.add(sid);
                if (renewed) renewedStudents.add(sid);
            }
        }

        int multi = 0;
        for (int c : enrollCount.values()) if (c >= 2) multi++;
        double rate = dueStudents.isEmpty() ? 0.0
                : Math.round(renewedStudents.size() * 100.0 / dueStudents.size() * 100.0) / 100.0;
        result.put("renewalRate", rate);
        result.put("dueStudents", dueStudents.size());
        result.put("renewedStudents", renewedStudents.size());
        result.put("renewWindowBeforeDays", BEFORE_DAYS);
        result.put("renewWindowAfterDays", AFTER_DAYS);
        result.put("multiCourseStudents", multi);
        result.put("totalEnrolledStudents", enrollCount.size());
        double multiRate = enrollCount.isEmpty() ? 0.0
                : Math.round(multi * 100.0 / enrollCount.size() * 100.0) / 100.0;
        result.put("multiCourseRate", multiRate);
        return result;
    }

    @Override
    public Map<String, Object> getAttendanceRate(Integer classId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<CourseSchedule> schedules = (startDate != null && endDate != null)
                ? scheduleMapper.selectByDateRange(startDate, endDate)
                : scheduleMapper.selectByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31));
        int total = 0, present = 0, late = 0, absent = 0, leave = 0;
        for (CourseSchedule cs : schedules) {
            if (classId != null && !classId.equals(cs.getClassId())) continue;
            for (Attendance a : attendanceMapper.selectByScheduleId(cs.getScheduleId())) {
                total++;
                String st = a.getStatus();
                if ("出勤".equals(st)) {
                    present++;
                } else if ("迟到".equals(st)) {
                    present++;
                    late++;
                } else if ("缺勤".equals(st)) {
                    absent++;
                } else if ("请假".equals(st)) {
                    leave++;
                }
            }
        }
        result.put("totalRecords", total);
        result.put("presentRecords", present);
        result.put("lateRecords", late);
        result.put("absentRecords", absent);
        result.put("leaveRecords", leave);
        result.put("attendanceRate", total > 0 ? Math.round(present * 100.0 / total * 100.0) / 100.0 : 0.0);
        return result;
    }

    @Override
    public Map<String, Object> getClassroomUtilization(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<CourseSchedule> schedules = (startDate != null && endDate != null)
                ? scheduleMapper.selectByDateRange(startDate, endDate)
                : scheduleMapper.selectByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31));
        java.util.Map<Integer, Integer> roomCount = new java.util.HashMap<>();
        java.util.Map<Integer, Double> roomHours = new java.util.HashMap<>();
        int validCount = 0;
        for (CourseSchedule cs : schedules) {
            if (BizConstants.SCHED_CANCELED.equals(cs.getStatus())) continue;
            validCount++;
            if (cs.getClassroomId() != null) {
                roomCount.merge(cs.getClassroomId(), 1, Integer::sum);
                roomHours.merge(cs.getClassroomId(), cs.getHours() == null ? 0 : cs.getHours(), Double::sum);
            }
        }
        List<Map<String, Object>> rooms = new ArrayList<>();
        for (Classroom cr : classroomMapper.selectAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            int cnt = roomCount.getOrDefault(cr.getClassroomId(), 0);
            row.put("classroomId", cr.getClassroomId());
            row.put("classroomName", cr.getClassroomName());
            row.put("scheduleCount", cnt);
            row.put("totalHours", Math.round(roomHours.getOrDefault(cr.getClassroomId(), 0.0) * 10) / 10.0);
            row.put("share", validCount > 0 ? Math.round(cnt * 1000.0 / validCount) / 10.0 : 0.0);
            rooms.add(row);
        }
        rooms.sort((a, b) -> Integer.compare((int) b.get("scheduleCount"), (int) a.get("scheduleCount")));
        result.put("rooms", rooms);
        result.put("totalSchedules", validCount);
        return result;
    }

    @Override
    public Map<String, Object> getTeacherHours(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<CourseSchedule> schedules = (startDate != null && endDate != null)
                ? scheduleMapper.selectByDateRange(startDate, endDate)
                : scheduleMapper.selectByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31));
        java.util.Map<Integer, Double> hours = new java.util.HashMap<>();
        int validSchedules = 0;
        for (CourseSchedule cs : schedules) {
            if (BizConstants.SCHED_CANCELED.equals(cs.getStatus())) continue;
            validSchedules++;
            if (cs.getTeacherId() != null) hours.merge(cs.getTeacherId(), cs.getHours() == null ? 0 : cs.getHours(), Double::sum);
        }
        result.put("totalSchedules", validSchedules);
        List<Map<String, Object>> teachers = new ArrayList<>();
        double totalHours = 0;
        for (Teacher t : teacherMapper.selectAll()) {
            double h = Math.round(hours.getOrDefault(t.getTeacherId(), 0.0) * 10) / 10.0;
            totalHours += h;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("teacherId", t.getTeacherId());
            row.put("teacherNo", t.getTeacherNo());
            row.put("teacherName", t.getName());
            row.put("displayName", (t.getTeacherNo() != null ? t.getTeacherNo() + " " : "") + t.getName());
            row.put("totalHours", h);
            teachers.add(row);
        }
        teachers.sort((a, b) -> Double.compare((double) b.get("totalHours"), (double) a.get("totalHours")));
        result.put("teachers", teachers);
        result.put("totalHours", Math.round(totalHours * 10) / 10.0);
        return result;
    }

    @Override
    public Map<String, Object> getStudentProgress(Integer studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<com.art.entity.Enrollment> enrolls = enrollmentMapper.selectByStudentId(studentId);
        result.put("courseCount", enrolls.size());
        int attended = 0, absent = 0;
        for (Attendance a : attendanceMapper.selectByConditions(null, null, studentId)) {
            if ("出勤".equals(a.getStatus()) || "迟到".equals(a.getStatus())) attended++;
            else if ("缺勤".equals(a.getStatus()) || "请假".equals(a.getStatus())) absent++; // 与触发器口径一致：缺勤/请假
        }
        result.put("attended", attended);
        result.put("absent", absent);
        List<HomeworkSubmission> hws = submissionMapper.selectByStudentId(studentId);
        long submitted = hws.stream().filter(h -> "已提交".equals(h.getStatus()) || "已批改".equals(h.getStatus())).count();
        result.put("homeworkTotal", hws.size());
        result.put("homeworkSubmitted", submitted);
        List<com.art.vo.ExamAchievementVO> exams = regMapper.selectAchievementsByStudent(studentId, null);
        long passed = exams.stream().filter(e -> "通过".equals(e.getResult()) || (e.getAward() != null && !e.getAward().isEmpty())).count();
        result.put("examTotal", exams.size());
        result.put("examPassed", passed);
        return result;
    }
}
