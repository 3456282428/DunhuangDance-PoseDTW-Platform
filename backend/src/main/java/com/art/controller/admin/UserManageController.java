package com.art.controller.admin;

import com.art.common.PageResult;
import com.art.common.Result;
import com.art.dto.UserDTO;
import com.art.entity.SysUser;
import com.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 运营管理端 - 用户与角色管理
 * 负责系统用户的增删改查、角色分配
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // 仅超级管理员可操作
public class UserManageController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword, role, status));
    }

    /**
     * 新增用户
     */
    @PostMapping
    public Result<Void> create(@RequestBody UserDTO userDTO) {
        userService.createUser(userDTO);
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/{userId}")
    public Result<Void> update(@PathVariable Integer userId, @RequestBody UserDTO userDTO) {
        userService.updateUser(userId, userDTO);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Integer userId, @RequestParam String newPassword) {
        userService.resetPassword(userId, newPassword);
        return Result.success();
    }
}