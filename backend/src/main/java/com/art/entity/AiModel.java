package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI模型注册（预留：本期仅登记与配置管理，不实现具体调用逻辑）
  * @author liukang&&heyan
 */
@Data
public class AiModel implements Serializable {

    private Integer modelId;
    private String modelName;
    /** 模型类型：LLM/EMBEDDING/RERANK */
    private String modelType;
    private String provider;
    private String endpoint;
    private String apiKey;
    private String description;
    /** 状态：UNCONFIGURED未配置/ENABLED启用/DISABLED停用 */
    private String status;
    /** 当前运行版本 */
    private String currentVersion;
    /** 可升级到的版本 */
    private String latestVersion;
    /** 更新状态：UP_TO_DATE / UPDATE_AVAILABLE / UNKNOWN */
    private String updateStatus;
    private LocalDateTime lastUpgradeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
