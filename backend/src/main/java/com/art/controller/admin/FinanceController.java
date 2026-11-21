package com.art.controller.admin;

import com.art.common.Result;
import com.art.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 运营管理端 - 财务管理与统计报表
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class FinanceController {

    private final FinanceService financeService;

    /** 学员账户：缴费历史、欠费、剩余课时 */
    @GetMapping("/student-account")
    public Result<Map<String, Object>> studentAccount(@RequestParam Integer studentId) {
        return Result.success(financeService.studentAccount(studentId));
    }

    /** 欠费学员列表 */
    @GetMapping("/arrears")
    public Result<Object> arrears() {
        return Result.success(financeService.arrearsList());
    }

    /** 续费办理 */
    @PostMapping("/renew")
    public Result<Void> renew(@RequestBody Map<String, Object> body) {
        financeService.renew(
                toInt(body.get("studentId")),
                toInt(body.get("classId")),
                toDecimal(body.get("amount")),
                (String) body.get("payMethod"),
                (String) body.get("remark"));
        return Result.success();
    }

    /** 收入统计 dimension = type / method / month */
    @GetMapping("/income-stat")
    public Result<Map<String, Object>> incomeStat(@RequestParam(defaultValue = "type") String dimension) {
        return Result.success(financeService.incomeStat(dimension));
    }

    /** 退款统计 */
    @GetMapping("/refund-stat")
    public Result<Map<String, Object>> refundStat() {
        return Result.success(financeService.refundStat());
    }

    /** 学员缴费排行 */
    @GetMapping("/top-students")
    public Result<Object> topStudents(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(financeService.topStudents(limit));
    }

    /** 教师工资支出（与学员缴费收入区分） */
    @GetMapping("/teacher-salary")
    public Result<Map<String, Object>> teacherSalary() {
        return Result.success(financeService.teacherSalary());
    }

    /** 教师工资账户明细 */
    @GetMapping("/teacher-account")
    public Result<Map<String, Object>> teacherAccount(@RequestParam Integer teacherId) {
        return Result.success(financeService.teacherAccount(teacherId));
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        return Integer.valueOf(o.toString());
    }

    private BigDecimal toDecimal(Object o) {
        if (o == null) return null;
        return new BigDecimal(o.toString());
    }
}
