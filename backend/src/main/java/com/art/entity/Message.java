package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * 站内消息通知
 * @author liukang&&heyan
 */
public class Message implements Serializable {

    private Integer messageId;
    private Integer receiverUserId;
    private String msgType;       // 系统/作业/申请/考级/调课/预约
    private String title;
    private String content;
    private String bizType;
    private Integer bizId;
    private Integer isRead;
    private LocalDateTime createTime;
}
