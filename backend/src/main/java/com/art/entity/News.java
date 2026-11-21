package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class News implements Serializable {

    private Integer newsId;
    private String title;
    private String content;
    private String publisher;
    private Integer publisherId; // 发布人用户ID（权威外键 -> sys_user）
    private LocalDateTime publishTime;
    private Integer viewCount;
}
