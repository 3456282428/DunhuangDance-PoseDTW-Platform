package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库备份记录
  * @author liukang&&heyan
 */
@Data
public class KbBackup implements Serializable {

    private Integer backupId;
    private String backupName;
    /** 类型：MANUAL手动 / AUTO每日定时 / RESTORE恢复前安全备份 */
    private String backupType;
    /** 备份范围：ALL/ADMIN/TEACHER/STUDENT */
    private String tableScope;
    private Integer itemCount;
    /** 备份文件相对路径（相对 uploads 目录） */
    private String backupFile;
    private String createdBy;
    private LocalDateTime createTime;
}
