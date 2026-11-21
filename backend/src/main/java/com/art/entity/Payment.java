package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class Payment implements Serializable {

    private Integer paymentId;
    private Integer studentId;
    private Integer classId;
    private Integer enrollmentId;
    private Integer regId;            // 关联考级/比赛报名ID
    private LocalDateTime payDate;
    private BigDecimal amount;
    private String payType;       // 报名 / 续费 / 考级 / 比赛 / 退款 / 其他
    private String payMethod;     // 微信 / 支付宝 / 现金 / 刷卡
    private Integer operatorId;
    private String remark;
    private String status;        // 已支付 / 退款中 / 已退款
    private LocalDateTime refundTime;
    private Integer refundOperatorId;

    // 关联展示字段（非数据库列）
    private String studentName;
    private String className;
}