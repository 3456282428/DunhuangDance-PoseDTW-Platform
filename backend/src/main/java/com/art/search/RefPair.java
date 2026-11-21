package com.art.search;

import lombok.Data;

import java.io.Serializable;

/**
 * 业务实体引用对（关联映射推导的中间产物）：如 schedule:12 → class:3（BELONGS_TO）
 * @author liukang&&heyan
 */
@Data
public class RefPair implements Serializable {

    private String srcRef;
    private String dstRef;
    private String relType;
}
