package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.*;
import com.art.mapper.*;
import com.art.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class FinanceServiceImpl implements FinanceService {

    private final PaymentMapper paymentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseClassMapper courseClassMapper;
    private final TeacherMapper teacherMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private Map<Integer, String> studentNames() {
        Map<Integer, String> m = new HashMap<>();
        for (Student s : studentMapper.selectAll()) m.put(s.getStudentId(), s.getName());
        return m;
    }

    private Map<Integer, CourseClass> classMap() {
        Map<Integer, CourseClass> m = new HashMap<>();
        for (CourseClass c : courseClassMapper.selectByCondition(null, null, null)) m.put(c.getClassId(), c);
        return m;
    }

    /** 是否为正向收入（实际到账：仅“已支付”） */
    private boolean isIncome(Payment p) {
        return p.getAmount() != null && p.getAmount().signum() > 0
                && "已支付".equals(p.getStatus());
    }

    /** 是否为已实际发生的退款支出（“已退款”；“退款中”为在途，不计入） */
    private boolean isRefunded(Payment p) {
        return p.getAmount() != null && "已退款".equals(p.getStatus());
    }

    @Override
    public Map<String, Object> studentAccount(Integer studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException("学员不存在");
        Map<Integer, CourseClass> classes = classMap();

        List<Payment> payments = paymentMapper.selectByStudentId(studentId);
        payments.sort(Comparator.comparing(Payment::getPayDate, Comparator.nullsLast(Comparator.reverseOrder())));
        for (Payment p : payments) {
            CourseClass c = classes.get(p.getClassId());
            if (c != null) p.setClassName(c.getClassName());
        }

        BigDecimal totalPaid = BigDecimal.ZERO, totalRefund = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getAmount() == null) continue;
            if (isIncome(p)) totalPaid = totalPaid.add(p.getAmount());
            else if (isRefunded(p)) totalRefund = totalRefund.add(p.getAmount().abs());
        }

        List<Map<String, Object>> enrollInfo = new ArrayList<>();
        BigDecimal arrears = BigDecimal.ZERO;
        for (Enrollment e : enrollmentMapper.selectByStudentId(studentId)) {
            CourseClass c = classes.get(e.getClassId());
            BigDecimal tuition = e.getTuitionAmount() != null ? e.getTuitionAmount()
                    : (c != null ? c.getTuition() : BigDecimal.ZERO);
            BigDecimal paid = payments.stream()
                    .filter(p -> isIncome(p)
                            && (Objects.equals(p.getEnrollmentId(), e.getEnrollmentId())
                                || (p.getEnrollmentId() == null && Objects.equals(p.getClassId(), e.getClassId()))))
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal owe = tuition.subtract(paid).max(BigDecimal.ZERO);
            arrears = arrears.add(owe);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("className", c != null ? c.getClassName() : "班级" + e.getClassId());
            m.put("status", e.getStatus());
            m.put("attendedCount", e.getAttendedCount());
            m.put("tuitionAmount", tuition);
            m.put("paid", paid);
            m.put("arrears", owe);
            enrollInfo.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("student", student);
        result.put("payments", payments);
        result.put("totalPaid", totalPaid);
        result.put("totalRefund", totalRefund);
        result.put("netPaid", totalPaid.subtract(totalRefund));
        result.put("arrears", arrears);
        result.put("enrollments", enrollInfo);
        return result;
    }

    @Override
    public Object arrearsList() {
        Map<Integer, String> names = studentNames();
        Map<Integer, CourseClass> classes = classMap();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : enrollmentMapper.selectAll()) {
            if (!"在读".equals(e.getStatus())) continue;
            CourseClass c = classes.get(e.getClassId());
            BigDecimal tuition = e.getTuitionAmount() != null ? e.getTuitionAmount()
                    : (c != null && c.getTuition() != null ? c.getTuition() : BigDecimal.ZERO);
            BigDecimal paid = paymentMapper.selectByStudentId(e.getStudentId()).stream()
                    .filter(p -> isIncome(p)
                            && (Objects.equals(p.getEnrollmentId(), e.getEnrollmentId())
                                || (p.getEnrollmentId() == null && Objects.equals(p.getClassId(), e.getClassId()))))
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal owe = tuition.subtract(paid);
            if (owe.compareTo(new BigDecimal("0.01")) > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("studentId", e.getStudentId());
                m.put("studentName", names.get(e.getStudentId()));
                m.put("classId", e.getClassId());
                m.put("className", c != null ? c.getClassName() : "班级" + e.getClassId());
                m.put("tuitionAmount", tuition);
                m.put("paid", paid);
                m.put("arrears", owe);
                result.add(m);
            }
        }
        result.sort((a, b) -> ((BigDecimal) b.get("arrears")).compareTo((BigDecimal) a.get("arrears")));
        return result;
    }

    @Override
    public void renew(Integer studentId, Integer classId, BigDecimal amount, String payMethod, String remark) {
        if (studentMapper.selectById(studentId) == null) throw new BusinessException("学员不存在");
        if (amount == null || amount.signum() <= 0) throw new BusinessException("续费金额必须大于0");
        Enrollment en = classId != null ? enrollmentMapper.selectByStudentAndClass(studentId, classId) : null;
        if (en == null) throw new BusinessException("该学员在此班级没有报名记录，无法续费，请先办理报名");
        Payment p = new Payment();
        p.setStudentId(studentId);
        p.setClassId(classId);
        p.setEnrollmentId(en.getEnrollmentId());
        p.setAmount(amount);
        p.setPayType("续费");
        p.setPayMethod(payMethod != null ? payMethod : "微信");
        p.setRemark(remark);
        p.setPayDate(java.time.LocalDateTime.now());
        p.setStatus("已支付");
        paymentMapper.insert(p);
    }

    @Override
    public Map<String, Object> incomeStat(String dimension) {
        List<Payment> all = allIncome();
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        if ("method".equals(dimension)) {
            for (Payment p : all) grouped.merge(p.getPayMethod() != null ? p.getPayMethod() : "其他",
                    p.getAmount(), BigDecimal::add);
        } else if ("month".equals(dimension)) {
            all.stream().map(p -> p.getPayDate().format(MONTH_FMT)).distinct().sorted()
                    .forEach(m -> grouped.put(m, BigDecimal.ZERO));
            for (Payment p : all) grouped.merge(p.getPayDate().format(MONTH_FMT), p.getAmount(), BigDecimal::add);
        } else { // type 默认
            for (Payment p : all) grouped.merge(p.getPayType() != null ? p.getPayType() : "其他",
                    p.getAmount(), BigDecimal::add);
        }
        List<String> categories = new ArrayList<>(grouped.keySet());
        List<Object> values = grouped.values().stream().map(BigDecimal::doubleValue).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("values", values);
        return result;
    }

    private List<Payment> allIncome() {
        List<Payment> all = new ArrayList<>();
        for (Student s : studentMapper.selectAll()) {
            for (Payment p : paymentMapper.selectByStudentId(s.getStudentId())) if (isIncome(p)) all.add(p);
        }
        return all;
    }

    @Override
    public Map<String, Object> refundStat() {
        BigDecimal income = BigDecimal.ZERO, refund = BigDecimal.ZERO;
        int refundCount = 0;
        for (Student s : studentMapper.selectAll()) {
            for (Payment p : paymentMapper.selectByStudentId(s.getStudentId())) {
                if (p.getAmount() == null) continue;
                if (isIncome(p)) income = income.add(p.getAmount());
                else if (isRefunded(p)) {
                    refund = refund.add(p.getAmount().abs());
                    refundCount++;
                }
            }
        }
        double rate = income.signum() > 0 ? refund.divide(income, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncome", income);
        result.put("totalRefund", refund);
        result.put("totalSalary", totalTeacherSalary());
        result.put("refundCount", refundCount);
        result.put("refundRate", Math.round(rate * 10) / 10.0);
        return result;
    }

    /** 教师工资总支出：底薪 + 课时费 × 已完成课时 */
    private BigDecimal totalTeacherSalary() {
        BigDecimal total = BigDecimal.ZERO;
        for (Teacher t : teacherMapper.selectAll()) {
            if (t.getStatus() == null || t.getStatus() != 1) continue;
            BigDecimal base = t.getBaseSalary() != null ? t.getBaseSalary() : BigDecimal.ZERO;
            BigDecimal rate = t.getHourlyRate() != null ? t.getHourlyRate() : BigDecimal.ZERO;
            BigDecimal hours = teacherMapper.completedHours(t.getTeacherId());
            if (hours == null) hours = BigDecimal.ZERO;
            total = total.add(base).add(rate.multiply(hours));
        }
        return total;
    }

    @Override
    public Map<String, Object> teacherSalary() {
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Teacher t : teacherMapper.selectAll()) {
            if (t.getStatus() == null || t.getStatus() != 1) continue;
            BigDecimal base = t.getBaseSalary() != null ? t.getBaseSalary() : BigDecimal.ZERO;
            BigDecimal rate = t.getHourlyRate() != null ? t.getHourlyRate() : BigDecimal.ZERO;
            BigDecimal hours = teacherMapper.completedHours(t.getTeacherId());
            if (hours == null) hours = BigDecimal.ZERO;
            BigDecimal amount = base.add(rate.multiply(hours));
            total = total.add(amount);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("teacherId", t.getTeacherId());
            m.put("teacherName", t.getName());
            m.put("teacherNo", t.getTeacherNo());
            m.put("employType", t.getEmployType());
            m.put("baseSalary", base);
            m.put("hourlyRate", rate);
            m.put("completedHours", hours);
            m.put("amount", amount);
            rows.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> teacherAccount(Integer teacherId) {
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) throw new BusinessException("教师不存在");
        BigDecimal base = t.getBaseSalary() != null ? t.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal rate = t.getHourlyRate() != null ? t.getHourlyRate() : BigDecimal.ZERO;
        BigDecimal hours = teacherMapper.completedHours(teacherId);
        if (hours == null) hours = BigDecimal.ZERO;
        BigDecimal classFee = rate.multiply(hours);
        BigDecimal amount = base.add(classFee);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("teacherId", t.getTeacherId());
        result.put("teacherName", t.getName());
        result.put("teacherNo", t.getTeacherNo());
        result.put("employType", t.getEmployType());
        result.put("title", t.getTitle());
        result.put("baseSalary", base);
        result.put("hourlyRate", rate);
        result.put("completedHours", hours);
        result.put("classFee", classFee);
        result.put("amount", amount);
        result.put("classHours", teacherMapper.classHours(teacherId));
        return result;
    }

    @Override
    public Object topStudents(Integer limit) {
        Map<Integer, String> names = studentNames();
        Map<Integer, BigDecimal> sum = new HashMap<>();
        for (Student s : studentMapper.selectAll()) {
            BigDecimal total = paymentMapper.selectByStudentId(s.getStudentId()).stream()
                    .filter(this::isIncome).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() > 0) sum.put(s.getStudentId(), total);
        }
        int n = limit != null ? limit : 10;
        return sum.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(n)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentId", e.getKey());
                    m.put("studentName", names.get(e.getKey()));
                    m.put("total", e.getValue());
                    return m;
                }).collect(Collectors.toList());
    }
}
