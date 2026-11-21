package com.art.mapper;

import com.art.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * 站内消息
 * @author liukang&&heyan
 */
public interface MessageMapper {
    int insert(Message message);
    /** 全量遍历（知识库全量归集用） */
    List<Message> selectAll();
    List<Message> selectByReceiver(@Param("receiverUserId") Integer receiverUserId);
    int countUnread(@Param("receiverUserId") Integer receiverUserId);
    int markRead(@Param("messageId") Integer messageId, @Param("receiverUserId") Integer receiverUserId);
    int markAllRead(@Param("receiverUserId") Integer receiverUserId);
}
