package com.art.service.impl;

import com.art.entity.Message;
import com.art.mapper.MessageMapper;
import com.art.service.MessageService;
import com.art.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public void send(Integer receiverUserId, String msgType, String title, String content, String bizType, Integer bizId) {
        if (receiverUserId == null) return;
        Message m = new Message();
        m.setReceiverUserId(receiverUserId);
        m.setMsgType(msgType != null ? msgType : "系统");
        m.setTitle(title);
        m.setContent(content);
        m.setBizType(bizType);
        m.setBizId(bizId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    @Override
    public List<Message> listMine() {
        Integer uid = SecurityUtil.getCurrentUserId();
        return messageMapper.selectByReceiver(uid);
    }

    @Override
    public long unreadCount() {
        Integer uid = SecurityUtil.getCurrentUserId();
        return uid == null ? 0 : messageMapper.countUnread(uid);
    }

    @Override
    public void markRead(Integer messageId) {
        messageMapper.markRead(messageId, SecurityUtil.getCurrentUserId());
    }

    @Override
    public void markAllRead() {
        Integer uid = SecurityUtil.getCurrentUserId();
        if (uid != null) messageMapper.markAllRead(uid);
    }
}
