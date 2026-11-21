package com.art.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * @author liukang&&heyan
 */
public class SysUser implements Serializable {

    private Integer userId;
    private String username;
    private String password;
    private String role;          // ADMIN / TEACHER / STUDENT / FINANCE / ACADEMIC
    private String realName;
    private String phone;
    private Integer status;       // 1-启用 0-禁用
    private LocalDateTime createTime;
}