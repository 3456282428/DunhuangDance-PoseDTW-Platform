package com.art.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}