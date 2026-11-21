package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class LoginResponseVO {

    private String token;
    private String username;
    private String realName;
    private String role;
}
