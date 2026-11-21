package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class SystemConfig implements Serializable {

    private Integer configId;
    private String configKey;
    private String configValue;
    private LocalDateTime updateTime;
}
