package com.art.common;

/**
 * 业务状态词表统一常量（与数据库 CHECK 约束保持一致）
 * @author liukang&&heyan
 */
public final class BizConstants {

    private BizConstants() {}

    /** 报名状态 */
    public static final String ENROLL_ACTIVE = "在读";
    public static final String ENROLL_DROPPED = "退学";
    public static final String ENROLL_FINISHED = "结课";

    /** 排课状态 */
    public static final String SCHED_PENDING = "待上课";
    public static final String SCHED_DONE = "已完成";
    public static final String SCHED_CANCELED = "已取消";

    /** 考勤状态 */
    public static final String ATT_PRESENT = "出勤";
    public static final String ATT_LATE = "迟到";
    public static final String ATT_ABSENT = "缺勤";
    public static final String ATT_LEAVE = "请假";

    /** 订单状态 */
    public static final String PAY_PAID = "已支付";
    public static final String PAY_REFUNDING = "退款中";
    public static final String PAY_REFUNDED = "已退款";

    /** 考级报名状态 */
    public static final String EXAM_PENDING = "待审核";
    public static final String EXAM_CONFIRMED = "已确认";
    public static final String EXAM_REJECTED = "已驳回";
    public static final String EXAM_PAID = "已缴费";
    public static final String EXAM_FINISHED = "已完成";
    public static final String EXAM_CANCELED = "已取消";

    /** 调课申请状态 */
    public static final String RR_PENDING = "待审核";
    public static final String RR_MOVE = "调课";
    public static final String RR_SWAP = "交换";
    public static final String RR_PASSED = "已通过";
    public static final String RR_REJECTED = "已拒绝";

    /** 学员申请状态 */
    public static final String REQ_PENDING = "待处理";
    public static final String REQ_APPROVED = "已批准";
    public static final String REQ_REJECTED = "已拒绝";

    /** 作业提交状态 */
    public static final String HS_UNSUBMITTED = "未提交";
    public static final String HS_SUBMITTED = "已提交";
    public static final String HS_GRADED = "已批改";
}
