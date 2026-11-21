package com.art;

import com.art.service.UserService;
import com.art.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 艺术培训管理系统启动测试类
 * 使用 test profile 避免依赖真实 MySQL，需在测试资源中提供 application-test.yml 配置 H2 内存数据库
 */
@SpringBootTest
@ActiveProfiles("test")
class ArtTrainingApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private TeacherService teacherService;

    /**
     * 验证 Spring 上下文能够正常加载
     */
    @Test
    void contextLoads() {
        // 该方法为空，仅用于触发上下文加载
    }

    /**
     * 验证用户服务 Bean 注入成功
     */
    @Test
    void testUserServiceBean() {
        assertNotNull(userService, "UserService should be autowired");
    }

    /**
     * 验证教师服务 Bean 注入成功
     */
    @Test
    void testTeacherServiceBean() {
        assertNotNull(teacherService, "TeacherService should be autowired");
    }
}