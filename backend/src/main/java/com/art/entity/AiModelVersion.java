package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI模型版本迭代记录
  * @author liukang&&heyan
 */
@Data
public class AiModelVersion implements Serializable {

    private Integer versionId;
    private Integer modelId;
    private String version;
    private LocalDate releaseDate;
    private String changelog;
    /** 是否当前在跑版本：1是 0否 */
    private Integer isCurrent;
    /** 升级状态：APPLIED已应用 / PENDING待接入执行 / FAILED失败 */
    private String upgradeStatus;
    private LocalDateTime createTime;
}
