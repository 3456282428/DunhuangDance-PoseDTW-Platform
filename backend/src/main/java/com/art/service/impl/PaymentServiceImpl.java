package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.entity.CourseClass;
import com.art.entity.Enrollment;
import com.art.entity.Payment;
import com.art.entity.Student;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.PaymentMapper;
import com.art.mapper.StudentMapper;
import com.art.service.PaymentService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final StudentMapper studentMapper;
    private final CourseClassMapper courseClassMapper;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    public PageResult<Payment> pagePayments(Integer pageNum, Integer pageSize, Integer studentId, String payType,
                                            String payMethod, String status, LocalDateTime startTime, LocalDateTime endTime) {
        PageHelper.startPage(pageNum, pageSize);
        List<Payment> list = paymentMapper.selectByConditions(studentId, payType, startTime, endTime, payMethod, status);
        PageInfo<Payment> pageInfo = new PageInfo<>(list);
        fillNames(pageInfo.getList());
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    private void fillNames(List<Payment> list) {
        Map<Integer, String> names = new HashMap<>();
        for (Student s : studentMapper.selectAll()) names.put(s.getStudentId(), s.getName());
        Map<Integer, String> classes = new HashMap<>();
        for (CourseClass c : courseClassMapper.selectByCondition(null, null, null)) classes.put(c.getClassId(), c.getClassName());
        for (Payment p : list) {
            p.setStudentName(names.get(p.getStudentId()));
            p.setClassName(classes.get(p.getClassId()));
        }
    }

    @Override
    public void createPayment(Payment payment) {
        payment.setPayDate(LocalDateTime.now());
        if (payment.getStatus() == null) payment.setStatus(BizConstants.PAY_PAID);

        // 组合一致性：指定了报名单时，其学员/班级必须与单据一致
        if (payment.getEnrollmentId() != null) {
            Enrollment linked = enrollmentMapper.selectById(payment.getEnrollmentId());
            if (linked != null) {
                if (payment.getStudentId() == null) payment.setStudentId(linked.getStudentId());
                if (payment.getClassId() == null) payment.setClassId(linked.getClassId());
                if (!linked.getStudentId().equals(payment.getStudentId())
                        || (linked.getClassId() != null && !linked.getClassId().equals(payment.getClassId())))
                    throw new BusinessException("缴费单与报名记录的学员/班级不一致");
            }
        }

        // 与报名模块关联同步：指定了学员+班级时，无报名则建档，有则累加缴费
        if (payment.getStudentId() != null && payment.getClassId() != null && BizConstants.PAY_PAID.equals(payment.getStatus())) {
            Enrollment en = enrollmentMapper.selectByStudentAndClass(payment.getStudentId(), payment.getClassId());
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            if (en == null) {
                en = new Enrollment();
                en.setStudentId(payment.getStudentId());
                en.setClassId(payment.getClassId());
                en.setEnrollDate(LocalDate.now());
                en.setStatus(BizConstants.ENROLL_ACTIVE);
                CourseClass cc = courseClassMapper.selectById(payment.getClassId());
                en.setTuitionAmount(cc != null && cc.getTuition() != null ? cc.getTuition() : amount);
                en.setTotalPaid(amount);
                en.setAttendedCount(0);
                en.setAbsentCount(0);
                enrollmentMapper.insert(en);
            } else {
                BigDecimal paid = en.getTotalPaid() != null ? en.getTotalPaid() : BigDecimal.ZERO;
                en.setTotalPaid(paid.add(amount));
                if (!BizConstants.ENROLL_ACTIVE.equals(en.getStatus())) en.setStatus(BizConstants.ENROLL_ACTIVE);
                enrollmentMapper.update(en);
            }
            payment.setEnrollmentId(en.getEnrollmentId());
        }
        paymentMapper.insert(payment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void refund(Integer paymentId, String remark) {
        Payment original = paymentMapper.selectById(paymentId);
        if (original == null) throw new BusinessException("缴费记录不存在");
        // 学员申请退款后状态为“退款中”，管理端确认；直接退款也允许“已支付”
        if (!BizConstants.PAY_PAID.equals(original.getStatus()) && !BizConstants.PAY_REFUNDING.equals(original.getStatus()))
            throw new BusinessException("仅已支付/退款中订单可退款");
        original.setStatus(BizConstants.PAY_REFUNDED);
        original.setRefundTime(LocalDateTime.now());
        original.setRefundOperatorId(com.art.util.SecurityUtil.getCurrentUserId());
        paymentMapper.update(original);

        Payment refund = new Payment();
        refund.setStudentId(original.getStudentId());
        refund.setClassId(original.getClassId());
        refund.setEnrollmentId(original.getEnrollmentId());
        refund.setRegId(original.getRegId());
        refund.setPayDate(LocalDateTime.now());
        refund.setAmount(original.getAmount().negate());
        refund.setPayType("退款");
        refund.setPayMethod(original.getPayMethod());
        refund.setStatus(BizConstants.PAY_REFUNDED);
        refund.setRemark(remark);
        refund.setRefundTime(original.getRefundTime());
        refund.setRefundOperatorId(original.getRefundOperatorId());
        paymentMapper.insert(refund);

        // 回减报名累计缴费，保证 total_paid 始终等于缴费流水净额
        if (original.getEnrollmentId() != null) {
            Enrollment en = enrollmentMapper.selectById(original.getEnrollmentId());
            if (en != null) {
                BigDecimal paid = en.getTotalPaid() != null ? en.getTotalPaid() : BigDecimal.ZERO;
                BigDecimal back = original.getAmount() != null ? original.getAmount() : BigDecimal.ZERO;
                en.setTotalPaid(paid.subtract(back).max(BigDecimal.ZERO));
                enrollmentMapper.update(en);
            }
        }
    }
}
