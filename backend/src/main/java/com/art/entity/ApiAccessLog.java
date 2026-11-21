package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接口访问留痕（由 ApiUsageInterceptor 自动写入，作为「API调用与用量」可视化数据源）
  * @author liukang&&heyan
 */
@Data
public class ApiAccessLog implements Serializable {

    private Long logId;
    /** 接口端点模板（数字段归一为 {id}） */
    private String endpoint;
    private String method;
    /** 业务分组 */
    private String apiGroup;
    private String username;
    /** 调用角色：ADMIN/TEACHER/STUDENT/ANON */
    private String roleScope;
    private Integer status;
    private Integer durationMs;
    private LocalDateTime accessTime;
}
