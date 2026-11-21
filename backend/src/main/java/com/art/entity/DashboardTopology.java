package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仪表盘拓扑关系边（组件依赖 / AI模型↔知识库关联，统一供拓扑图渲染）
  * @author liukang&&heyan
 */
@Data
public class DashboardTopology implements Serializable {

    private Integer edgeId;
    /** 源类型：COMPONENT / AI_MODEL / KB_SCOPE */
    private String sourceType;
    private String sourceKey;
    /** 目标类型：COMPONENT / AI_MODEL / KB_SCOPE */
    private String targetType;
    private String targetKey;
    /** 关系：depends_on / retrieves / invokes */
    private String relation;
    /** 依赖类型：runtime / build / peer */
    private String depType;
    private String versionConstraint;
    private String remark;
    private LocalDateTime createTime;
}
