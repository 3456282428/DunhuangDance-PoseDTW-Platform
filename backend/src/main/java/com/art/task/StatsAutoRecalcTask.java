package com.art.task;

import com.art.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 报名冗余统计自动轮询校准：
 * 每 5 分钟按考勤明细与缴费流水重算 attended_count / absent_count / total_paid，
 * 仅在发现不一致时落库，替代原管理端手动“重算统计”按钮。
 *
 * @author liukang&&heyan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsAutoRecalcTask {

    private final EnrollmentService enrollmentService;

    /** 启动 10 秒后首次执行，之后每 5 分钟轮询一次 */
    @Scheduled(initialDelay = 10_000, fixedDelay = 300_000)
    public void autoRecalc() {
        try {
            int fixed = enrollmentService.recalcAllStats();
            if (fixed > 0) log.info("[自动校准] 报名统计已修正 {} 条记录", fixed);
        } catch (Exception e) {
            log.warn("[自动校准] 报名统计轮询失败：{}", e.getMessage());
        }
    }
}
