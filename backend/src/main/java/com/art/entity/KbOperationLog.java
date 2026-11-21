package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库操作日志
  * @author liukang&&heyan
 */
@Data
public class KbOperationLog implements Serializable {

    private Long logId;
    /** 端别：ADMIN/TEACHER/STUDENT/SYSTEM */
    private String kbScope;
    private Integer kbId;
    /** 操作类型：CREATE/UPDATE/DELETE/AUTO_INGEST/BACKUP/RESTORE/AI_QUERY */
    private String opType;
    private Integer opUserId;
    private String opUsername;
    private String opDetail;
    private LocalDateTime opTime;
}
