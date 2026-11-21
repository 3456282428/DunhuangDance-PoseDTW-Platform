package com.art.service;

import com.art.entity.Message;

import java.util.List;

/**
 * 站内消息
 * @author liukang&&heyan
 */
public interface MessageService {

    /** 发送一条站内消息，receiverUserId 为 sys_user.user_id；为空则静默忽略 */
    void send(Integer receiverUserId, String msgType, String title, String content, String bizType, Integer bizId);

    List<Message> listMine();

    long unreadCount();

    void markRead(Integer messageId);

    void markAllRead();
}
