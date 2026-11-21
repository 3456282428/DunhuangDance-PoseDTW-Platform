package com.art.controller.common;

import com.art.common.Result;
import com.art.entity.Message;
import com.art.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内消息（任意登录角色均可访问自己的消息）
 * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    public Result<List<Message>> list() {
        return Result.success(messageService.listMine());
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @PutMapping("/{messageId}/read")
    public Result<Void> read(@PathVariable Integer messageId) {
        messageService.markRead(messageId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.markAllRead();
        return Result.success();
    }
}
