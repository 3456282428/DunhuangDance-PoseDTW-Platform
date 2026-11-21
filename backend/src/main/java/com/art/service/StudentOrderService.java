package com.art.service;

import com.art.common.PageResult;
import com.art.dto.CreateOrderDTO;
import com.art.entity.Payment;

/**
 * @author liukang&&heyan
 */
public interface StudentOrderService {
    PageResult<Payment> pageMyOrders(Integer pageNum, Integer pageSize, String status);
    Long createOrder(CreateOrderDTO orderDTO);
    void payOrder(Integer paymentId);
    void applyRefund(Integer paymentId, String reason);
}