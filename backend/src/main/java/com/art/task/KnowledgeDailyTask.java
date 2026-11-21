package com.art.task;

import com.art.service.KnowledgeAutoIngestService;
import com.art.service.KnowledgeBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识库常态化自动任务：
 * - 应用启动就绪后首次全量归集（异常隔离，不阻塞启动）
 * - 每 3 小时全量归集一次
 * - 每日 02:00 运营日报自动归集至管理员端知识库
 * - 每日 02:30 三端知识库全量自动备份
  * @author liukang&&heyan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeDailyTask {

    private final KnowledgeAutoIngestService autoIngestService;
    private final KnowledgeBackupService backupService;

    /** 应用启动就绪后执行一次全量归集（不抛异常、不阻塞启动） */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            autoIngestService.ingestAllData();
            log.info("[知识库] 启动首次全量归集完成");
        } catch (Exception e) {
            log.warn("[知识库] 启动首次全量归集失败：{}", e.getMessage());
        }
    }

    /** 每 3 小时全量归集一次（幂等 upsert，内容随业务数据刷新） */
    @Scheduled(cron = "0 0 */3 * * ?")
    public void allDataIngest() {
        try {
            autoIngestService.ingestAllData();
            log.info("[知识库] 每3小时全量归集完成");
        } catch (Exception e) {
            log.warn("[知识库] 每3小时全量归集失败：{}", e.getMessage());
        }
    }

    /** 每日 02:00 运营日报归集（知识库自然增长机制） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReportIngest() {
        try {
            autoIngestService.ingestAdminDailyReport();
            log.info("[知识库] 每日运营日报归集完成");
        } catch (Exception e) {
            log.warn("[知识库] 每日运营日报归集失败：{}", e.getMessage());
        }
    }

    /** 每日 02:30 知识库全量自动备份 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void dailyBackup() {
        try {
            backupService.createBackup(KnowledgeBackupService.SCOPE_ALL, "AUTO", "SYSTEM");
            log.info("[知识库] 每日自动备份完成");
        } catch (Exception e) {
            log.warn("[知识库] 每日自动备份失败：{}", e.getMessage());
        }
    }
}
