package com.art.config;

import com.art.interceptor.ApiUsageInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 配置：uploads 静态资源映射 + /api/** 接口访问留痕拦截
  * @author liukang&&heyan
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiUsageInterceptor apiUsageInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        // noCache：允许缓存但每次必须回源验证（304/200）。
        // 视频文件可能被同名替换（如大视频转码压缩），若用长效缓存，浏览器会混用新旧分段导致播放暂停/回退。
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadDir)
                .setCacheControl(CacheControl.noCache().mustRevalidate());
    }

    /** 记录 /api/** 接口访问用量（「API调用与用量」可视化数据源） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiUsageInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(ApiUsageInterceptor.EXCLUDE_PATH);
    }
}
