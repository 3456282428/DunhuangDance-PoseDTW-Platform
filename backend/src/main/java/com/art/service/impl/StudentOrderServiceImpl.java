package com.art.service.impl;

import com.art.common.BizConstants;
import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.dto.CreateOrderDTO;
import com.art.entity.CourseClass;
import com.art.entity.Payment;
import com.art.mapper.PaymentMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.CourseClassMapper;
import com.art.service.PaymentService;
import com.art.service.StudentOrderService;
import com.art.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class StudentOrderServiceImpl implements StudentOrderService {

    private final PaymentMapper paymentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseClassMapper courseClassMapper;
    private final PaymentService paymentService;

    @Override
    public PageResult<Payment> pageMyOrders(Integer pageNum, Integer pageSize, String status) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        PageHelper.startPage(pageNum, pageSize);
        List<Payment> list = paymentMapper.selectByStudentId(studentId);
        for (Payment p : list) {
            if (p.getClassId() != null) {
                CourseClass cc = courseClassMapper.selectById(p.getClassId());
                if (cc != null) p.setClassName(cc.getClassName());
            }
        }
        PageInfo<Payment> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public Long createOrder(CreateOrderDTO orderDTO) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        if (orderDTO.getClassId() == null) throw new BusinessException("请选择班级");
        CourseClass cc = courseClassMapper.selectById(orderDTO.getClassId());
        if (cc == null) throw new BusinessException("班级不存在");
        if (orderDTO.getAmount() == null || orderDTO.getAmount().signum() <= 0)
            throw new BusinessException("缴费金额必须大于0");

        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setClassId(orderDTO.getClassId());
        payment.setPayDate(LocalDateTime.now());
        payment.setAmount(orderDTO.getAmount());
        // 未显式指定类型时按报名/续费自动判定
        String payType = orderDTO.getPayType();
        if (payType == null || payType.isBlank()) {
            payType = enrollmentMapper.selectByStudentAndClass(studentId, orderDTO.getClassId()) != null ? "续费" : "报名";
        }
        payment.setPayType(payType);
        payment.setPayMethod(orderDTO.getPayMethod() != null ? orderDTO.getPayMethod() : "微信");
        payment.setStatus(BizConstants.PAY_PAID);
        // 统一走缴费服务：无报名自动建档、有报名累加 total_paid，并回填 enrollment_id
        paymentService.createPayment(payment);
        return payment.getPaymentId().longValue();
    }

    @Override
    public void payOrder(Integer paymentId) {
        Payment payment = mustOwn(paymentId);
        payment.setStatus(BizConstants.PAY_PAID);
        payment.setRemark("已支付");
        paymentMapper.update(payment);
    }

    @Override
    public void applyRefund(Integer paymentId, String reason) {
        Payment payment = mustOwn(paymentId);
        if (!BizConstants.PAY_PAID.equals(payment.getStatus())) throw new BusinessException("仅已支付订单可申请退款");
        payment.setStatus(BizConstants.PAY_REFUNDING);
        payment.setRemark("退款申请：" + reason);
        paymentMapper.update(payment);
    }

    /** 订单必须存在且属于当前学员 */
    private Payment mustOwn(Integer paymentId) {
        Integer studentId = SecurityUtil.getCurrentStudentId();
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) throw new BusinessException("订单不存在");
        if (studentId != null && !studentId.equals(payment.getStudentId()))
            throw new BusinessException("不能操作他人订单");
        return payment;
    }
}
