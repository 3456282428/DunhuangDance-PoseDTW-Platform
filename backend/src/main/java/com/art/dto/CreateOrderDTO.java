package com.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
/**
 * @author liukang&&heyan
 */
public class CreateOrderDTO {

    @NotNull(message = "班级不能为空")
    private Integer classId;

    @NotNull(message = "缴费类型不能为空")
    private String payType;   // 报名 / 续费 / 考级 / 比赛 / 其他

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private String payMethod; // 微信 / 支付宝 / 现金 / 刷卡
}