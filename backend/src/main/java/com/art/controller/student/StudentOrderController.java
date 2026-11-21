package com.art.controller.student;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.CreateOrderDTO;
import com.art.entity.Payment;
import com.art.service.StudentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端 - 我的订单与报名缴费
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/student/order")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentOrderController {

    private final StudentOrderService studentOrderService;

    /**
     * 分页查询我的订单
     */
    @GetMapping("/list")
    public Result<PageResult<Payment>> listOrders(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) String status) {
        return Result.success(studentOrderService.pageMyOrders(pageNum, pageSize, status));
    }

    /**
     * 在线报名/缴费，创建订单
     */
    @PostMapping("/create")
    public Result<Long> createOrder(@RequestBody CreateOrderDTO orderDTO) {
        return Result.success(studentOrderService.createOrder(orderDTO));
    }

    /**
     * 模拟支付（实际可对接微信/支付宝）
     */
    @PostMapping("/{paymentId}/pay")
    public Result<Void> payOrder(@PathVariable Integer paymentId) {
        studentOrderService.payOrder(paymentId);
        return Result.success();
    }

    /**
     * 申请退款
     */
    @PostMapping("/{paymentId}/refund")
    public Result<Void> applyRefund(@PathVariable Integer paymentId, @RequestParam String reason) {
        studentOrderService.applyRefund(paymentId, reason);
        return Result.success();
    }
}