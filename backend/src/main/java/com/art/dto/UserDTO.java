package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class UserDTO {

    private Integer userId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    @NotBlank(message = "角色不能为空")
    private String role;   // ADMIN / TEACHER / STUDENT / FINANCE / ACADEMIC

    private Integer status; // 1-启用，0-禁用
}