package com.art.task;

import com.art.service.AiModelMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 仪表盘定时任务：AI模型性能指标周期采样（每分钟一次，保留48小时）
  * @author liukang&&heyan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardMetricTask {

    private final AiModelMetricService metricService;

    /** 每60秒采集一次模型性能指标 */
    @Scheduled(fixedRate = 60_000L, initialDelay = 20_000L)
    public void sampleModelMetrics() {
        try {
            int n = metricService.collectAll().size();
            log.debug("AI模型性能指标采集完成，模型数：{}", n);
        } catch (Exception e) {
            log.warn("AI模型性能指标采集失败：{}", e.getMessage());
        }
    }
}
