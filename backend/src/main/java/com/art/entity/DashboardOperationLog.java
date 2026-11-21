package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仪表盘管理操作日志（同步/升级/导出留痕）
  * @author liukang&&heyan
 */
@Data
public class DashboardOperationLog implements Serializable {

    private Long dashLogId;
    /** SYNC_COMPONENT / UPGRADE_MODEL / EXPORT_REPORT / VIEW_DETAIL */
    private String opType;
    private String opTarget;
    private String opDetail;
    /** SUCCESS / FAILED / PENDING */
    private String opResult;
    private Integer opUserId;
    private String opUsername;
    private String opRole;
    private LocalDateTime opTime;
}
