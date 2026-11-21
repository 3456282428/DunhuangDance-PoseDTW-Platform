package com.art.config;

import com.art.entity.SysUser;
import com.art.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

/**
 * Spring Security 配置类
 * 
  * @author liukang&&heyan
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 启用方法级权限注解（如 @PreAuthorize）
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 用户详情服务：从数据库按用户名加载用户
     */
    @Bean
    public UserDetailsService userDetailsService(UserMapper userMapper) {
        return username -> {
            SysUser sysUser = userMapper.findByUsername(username);
            if (sysUser == null) {
                throw new UsernameNotFoundException("用户不存在: " + username);
            }
            return new User(
                    sysUser.getUsername(),
                    sysUser.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole()))
            );
        };
    }

    /**
     * 认证管理器：供登录认证使用
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())   // 关闭 CSRF（前后端分离使用 JWT）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()   // 放行登录、注册接口
                .requestMatchers("/api/portal/**").permitAll()   // 放行游客门户公开接口（课程浏览/试听预约）
                .requestMatchers("/uploads/**").permitAll()   // 放行上传的静态资源（作业图片/视频/PDF）
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll() // 放行 Swagger（如果有）
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 添加 JWT 过滤器

        return http.build();
    }
}