package com.art.service;

import com.art.common.PageResult;
import com.art.entity.Payment;
import java.time.LocalDateTime;

/**
 * @author liukang&&heyan
 */
public interface PaymentService {
    PageResult<Payment> pagePayments(Integer pageNum, Integer pageSize, Integer studentId, String payType,
                                     String payMethod, String status, LocalDateTime startTime, LocalDateTime endTime);
    void createPayment(Payment payment);
    void refund(Integer paymentId, String remark);
}
