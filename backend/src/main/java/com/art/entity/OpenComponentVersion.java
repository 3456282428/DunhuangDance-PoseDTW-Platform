package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开源组件版本迭代记录
  * @author liukang&&heyan
 */
@Data
public class OpenComponentVersion implements Serializable {

    private Integer versionId;
    private Integer componentId;
    private String version;
    private LocalDate releaseDate;
    private String changelog;
    /** 是否当前使用版本：1是 0否 */
    private Integer isCurrent;
    /** 数据来源：API / MANUAL / SEED */
    private String detectSource;
    private LocalDateTime createTime;
}
