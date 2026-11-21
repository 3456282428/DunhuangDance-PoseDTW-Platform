package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI模型性能指标时序（实时曲线数据源）
  * @author liukang&&heyan
 */
@Data
public class AiModelMetric implements Serializable {

    private Long metricId;
    private Integer modelId;
    private LocalDateTime metricTime;
    /** 平均响应延迟(ms) */
    private BigDecimal latencyMs;
    /** 吞吐量(次/分钟) */
    private BigDecimal throughputQps;
    /** 准确率(0-1) */
    private BigDecimal accuracy;
    /** 调用成功率(0-1) */
    private BigDecimal successRate;
    private Integer callCount;
    /** 知识库平均命中条数 */
    private BigDecimal kbHitAvg;
    /** 来源：SAMPLE / SEED / MANUAL */
    private String metricSource;
    private LocalDateTime createTime;
}
