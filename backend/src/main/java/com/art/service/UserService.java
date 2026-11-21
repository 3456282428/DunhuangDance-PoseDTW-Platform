package com.art.service;

import com.art.common.PageResult;
import com.art.dto.RegisterDTO;
import com.art.dto.UserDTO;
import com.art.entity.SysUser;

/**
 * @author liukang&&heyan
 */
public interface UserService {
    SysUser findByUsername(String username);
    PageResult<SysUser> pageUsers(Integer pageNum, Integer pageSize, String keyword, String role, Integer status);
    void createUser(UserDTO userDTO);
    void updateUser(Integer userId, UserDTO userDTO);
    void deleteUser(Integer userId);
    void resetPassword(Integer userId, String newPassword);
    void changePassword(String username, String oldPassword, String newPassword);
    void register(RegisterDTO registerDTO);
}