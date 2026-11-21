package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开源组件登记（GitHub / Gitee 代码仓库，用于版本追踪与更新状态监控）
  * @author liukang&&heyan
 */
@Data
public class OpenComponent implements Serializable {

    private Integer componentId;
    private String name;
    /** 来源平台：GITHUB / GITEE */
    private String platform;
    private String repoOwner;
    private String repoName;
    private String repoUrl;
    private String category;
    private String language;
    private String license;
    /** 本系统内使用模块 */
    private String usedByModule;
    /** 当前使用版本 */
    private String currentVersion;
    /** 远端最新版本 */
    private String latestVersion;
    private LocalDate latestReleaseDate;
    /** 更新状态：UP_TO_DATE / UPDATE_AVAILABLE / UNKNOWN / SYNC_FAILED */
    private String updateStatus;
    private Integer starCount;
    private Integer forkCount;
    private Integer openIssueCount;
    private String description;
    private String homepage;
    /** 是否参与自动同步：1是 0否 */
    private Integer syncEnabled;
    private LocalDateTime lastSyncTime;
    /** 最近同步结果：OK / FAILED / SKIPPED */
    private String lastSyncStatus;
    private String lastSyncMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
