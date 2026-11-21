package com.art.service;

import com.art.entity.AiModel;
import com.art.entity.AiModelMetric;
import com.art.mapper.AiModelMapper;
import com.art.mapper.AiModelMetricMapper;
import com.art.mapper.DashboardStatsMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI模型性能指标采集服务：
 * 指标全部取自真实业务数据（AI统一入口调用日志、AI评分落库记录），
 * 采集窗口内无新样本时沿用最近一次观测值并置调用数为0，不做数据伪造。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelMetricService {

    private final AiModelMetricMapper metricMapper;
    private final AiModelMapper aiModelMapper;
    private final DashboardStatsMapper statsMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 指标采样窗口（分钟）：每次采集统计近5分钟的真实业务活动 */
    private static final int WINDOW_MINUTES = 5;
    /** 指标保留时长（小时） */
    private static final int RETENTION_HOURS = 48;
    /** 单次采集最多解析的AI报告样本数（解析processingTime求真实推理耗时） */
    private static final int REPORT_SAMPLE_LIMIT = 50;

    /**
     * 采集一次指标（对全部已登记模型执行），返回本次采集结果
     */
    public List<AiModelMetric> collectAll() {
        List<AiModel> models = aiModelMapper.selectAll();
        List<AiModelMetric> collected = new ArrayList<>();
        for (AiModel m : models) {
            AiModelMetric metric = collectForModel(m.getModelId());
            if (metric != null) collected.add(metric);
        }
        return collected;
    }

    /**
     * 采集指定模型的一次性能指标
     */
    public AiModelMetric collectForModel(Integer modelId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        AiModelMetric prev = lastMetric(modelId);

        AiModelMetric metric = new AiModelMetric();
        metric.setModelId(modelId);
        metric.setMetricTime(LocalDateTime.now());
        metric.setMetricSource("SAMPLE");

        // 1) AI统一入口调用量 + 知识库命中（真实调用日志）
        Map<String, Object> invoke = statsMapper.selectInvokeAgg(since);
        long callCount = toLong(invoke == null ? null : invoke.get("callCount"));
        metric.setCallCount((int) callCount);
        metric.setThroughputQps(scale(BigDecimal.valueOf(callCount)
                        .divide(BigDecimal.valueOf(WINDOW_MINUTES), 2, RoundingMode.HALF_UP)));
        metric.setKbHitAvg(invoke == null || callCount == 0 ? carry(prev, AiModelMetric::getKbHitAvg)
                : scale(toDecimal(invoke.get("kbHitAvg"))));

        // 2) 成功率：AI评分完成占比（真实评分状态）
        Map<String, Object> perf = statsMapper.selectScorePerf(since);
        long scored = toLong(perf == null ? null : perf.get("scoredCount"));
        Long done = perf == null ? null : toLongObj(perf.get("doneCount"));
        Long fail = perf == null ? null : toLongObj(perf.get("failCount"));
        if (scored > 0 && done != null) {
            long total = done + (fail == null ? 0L : fail);
            metric.setSuccessRate(total == 0 ? BigDecimal.ONE
                    : scale(BigDecimal.valueOf(done).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)));
        } else {
            metric.setSuccessRate(carry(prev, AiModelMetric::getSuccessRate));
        }

        // 3) 准确率：AI评分与教师评分一致度（1 - 平均绝对差/100），真实双向打分样本
        if (perf != null && perf.get("avgAbsDiff") != null) {
            BigDecimal diff = toDecimal(perf.get("avgAbsDiff"));
            BigDecimal acc = BigDecimal.ONE.subtract(diff.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            if (acc.compareTo(BigDecimal.ZERO) < 0) acc = BigDecimal.ZERO;
            metric.setAccuracy(scale(acc));
        } else {
            metric.setAccuracy(carry(prev, AiModelMetric::getAccuracy));
        }

        // 4) 延迟：AI评分报告中的真实处理耗时 processingTime
        BigDecimal latency = avgProcessingTime(since);
        metric.setLatencyMs(latency != null ? scale(latency) : carry(prev, AiModelMetric::getLatencyMs));

        metricMapper.insert(metric);
        metricMapper.deleteOlderThan(LocalDateTime.now().minusHours(RETENTION_HOURS));
        return metric;
    }

    /** 最近 N 条指标序列（时间升序，直接供图表使用） */
    public List<AiModelMetric> recent(Integer modelId, int limit) {
        return metricMapper.selectRecent(modelId, Math.min(Math.max(limit, 1), 300));
    }

    /** 汇总当前模型指标概览（最新值 + 窗口均值） */
    public Map<String, Object> summary(Integer modelId) {
        List<AiModelMetric> list = recent(modelId, 60);
        Map<String, Object> out = new LinkedHashMap<>();
        if (list.isEmpty()) {
            out.put("latest", null);
            out.put("count", 0);
            return out;
        }
        AiModelMetric latest = list.get(list.size() - 1);
        out.put("latest", latest);
        out.put("count", list.size());
        out.put("avgLatencyMs", avg(list, AiModelMetric::getLatencyMs));
        out.put("avgAccuracy", avg(list, AiModelMetric::getAccuracy));
        out.put("avgSuccessRate", avg(list, AiModelMetric::getSuccessRate));
        out.put("avgKbHit", avg(list, AiModelMetric::getKbHitAvg));
        out.put("totalCalls", list.stream().mapToInt(m -> m.getCallCount() == null ? 0 : m.getCallCount()).sum());
        return out;
    }

    // ==================== 内部工具 ====================

    private AiModelMetric lastMetric(Integer modelId) {
        List<AiModelMetric> list = metricMapper.selectRecent(modelId, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private BigDecimal avgProcessingTime(LocalDateTime since) {
        List<String> reports = statsMapper.selectRecentAiReports(since, REPORT_SAMPLE_LIMIT);
        if (reports == null || reports.isEmpty()) return null;
        double sum = 0;
        int n = 0;
        for (String r : reports) {
            try {
                JsonNode node = JSON.readTree(r);
                JsonNode pt = node.get("processingTime");
                if (pt != null && pt.isNumber()) {
                    sum += pt.asDouble();
                    n++;
                }
            } catch (Exception ignore) {
                // 单条报告解析失败不影响整体统计
            }
        }
        return n == 0 ? null : BigDecimal.valueOf(sum / n);
    }

    private static BigDecimal carry(AiModelMetric prev,
                                    java.util.function.Function<AiModelMetric, BigDecimal> getter) {
        return prev == null ? null : getter.apply(prev);
    }

    private static BigDecimal avg(List<AiModelMetric> list,
                                 java.util.function.Function<AiModelMetric, BigDecimal> getter) {
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (AiModelMetric m : list) {
            BigDecimal v = getter.apply(m);
            if (v != null) {
                sum = sum.add(v);
                n++;
            }
        }
        return n == 0 ? null : sum.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static long toLong(Object o) {
        Long v = toLongObj(o);
        return v == null ? 0L : v;
    }

    private static Long toLongObj(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
