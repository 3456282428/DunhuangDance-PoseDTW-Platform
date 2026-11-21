package com.art.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis 配置类
 * 可配置分页插件等
  * @author liukang&&heyan
 */
@Configuration
public class MyBatisConfig {

    /**
     * 配置 PageHelper 分页插件（需要引入依赖）
     * 如果暂不使用分页插件，可省略此方法
     */
    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        properties.setProperty("reasonable", "true");
        interceptor.setProperties(properties);
        return interceptor;
    }
}