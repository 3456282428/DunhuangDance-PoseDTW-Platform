package com.art.dto;

import lombok.Data;

/**
 * 当前登录用户自助修改密码入参
 *
 * @author liukang&&heyan
 */
@Data
public class ChangePasswordDTO {
    private String oldPassword;
    private String newPassword;
}
