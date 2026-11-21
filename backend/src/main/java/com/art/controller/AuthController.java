package com.art.controller;

import com.art.common.Result;
import com.art.config.JwtUtil;
import com.art.dto.ChangePasswordDTO;
import com.art.dto.LoginDTO;
import com.art.dto.RegisterDTO;
import com.art.entity.SysUser;
import com.art.service.UserService;
import com.art.vo.LoginResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器：处理登录、注册、获取当前用户信息等
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@RequestBody LoginDTO loginDTO) {
        // 使用 AuthenticationManager 进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 从认证信息中获取用户详情
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        SysUser user = userService.findByUsername(userDetails.getUsername());

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // 构建返回数据
        LoginResponseVO responseVO = new LoginResponseVO();
        responseVO.setToken(token);
        responseVO.setUsername(user.getUsername());
        responseVO.setRealName(user.getRealName());
        responseVO.setRole(user.getRole());

        return Result.success("登录成功", responseVO);
    }

    /**
     * 用户注册（默认注册为学员）
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    /**
     * 当前登录用户自助修改密码（学员/教师等所有角色通用）
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userService.changePassword(authentication.getName(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<SysUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        SysUser user = userService.findByUsername(username);
        // 出于安全考虑，清除密码
        user.setPassword(null);
        return Result.success(user);
    }
}