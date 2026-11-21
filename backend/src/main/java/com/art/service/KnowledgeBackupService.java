package com.art.service;

import com.art.entity.KbBackup;

import java.util.List;
import java.util.Map;

/**
 * 知识库备份服务：JSON快照落盘 uploads/kb_backup/，支持手动/定时备份与恢复
  * @author liukang&&heyan
 */
public interface KnowledgeBackupService {

    String SCOPE_ALL = "ALL";

    /**
     * 创建备份
     * @param scope ALL/ADMIN/TEACHER/STUDENT
     * @param backupType MANUAL/AUTO/RESTORE
     * @param operator 操作人（定时任务传SYSTEM）
     */
    KbBackup createBackup(String scope, String backupType, String operator);

    List<KbBackup> listBackups();

    /** 从备份恢复（恢复前自动做一次RESTORE类型安全备份） */
    Map<String, Object> restoreBackup(Integer backupId, String operator);
}
