package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.entity.Payment;
import com.art.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 运营管理端 - 订单与财务管理
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 分页查询缴费记录
     */
    @GetMapping("/list")
    public Result<PageResult<Payment>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) Integer studentId,
                                            @RequestParam(required = false) String payType,
                                            @RequestParam(required = false) String payMethod,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(paymentService.pagePayments(pageNum, pageSize, studentId, payType, payMethod, status, startTime, endTime));
    }

    /**
     * 新增缴费（手动录入）
     */
    @PostMapping
    public Result<Void> create(@RequestBody Payment payment) {
        paymentService.createPayment(payment);
        return Result.success();
    }

    /**
     * 退款操作
     */
    @PostMapping("/{paymentId}/refund")
    public Result<Void> refund(@PathVariable Integer paymentId, @RequestParam String remark) {
        paymentService.refund(paymentId, remark);
        return Result.success();
    }
}