package com.art.mapper;

import com.art.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface PaymentMapper {
    Payment selectById(@Param("paymentId") Integer paymentId);
    int insert(Payment payment);
    int update(Payment payment);
    int deleteById(@Param("paymentId") Integer paymentId);
    List<Payment> selectByStudentId(@Param("studentId") Integer studentId);
    List<Payment> selectByConditions(@Param("studentId") Integer studentId,
                                     @Param("payType") String payType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("payMethod") String payMethod,
                                     @Param("status") String status);
}