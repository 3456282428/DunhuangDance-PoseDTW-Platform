package com.art.service;

import java.util.Map;

/**
 * 运营管理端 - 财务管理与统计报表
  * @author liukang&&heyan
 */
public interface FinanceService {

    /** 学员账户：缴费历史、欠费、剩余课时 */
    Map<String, Object> studentAccount(Integer studentId);

    /** 欠费学员列表 */
    Object arrearsList();

    /** 续费办理 */
    void renew(Integer studentId, Integer classId, java.math.BigDecimal amount, String payMethod, String remark);

    /** 收入统计：dimension = type / method / month */
    Map<String, Object> incomeStat(String dimension);

    /** 退款统计 */
    Map<String, Object> refundStat();

    /** 学员缴费排行 */
    Object topStudents(Integer limit);

    /** 教师工资支出（与学员缴费收入区分） */
    Map<String, Object> teacherSalary();

    /** 单个教师工资账户明细 */
    Map<String, Object> teacherAccount(Integer teacherId);
}
