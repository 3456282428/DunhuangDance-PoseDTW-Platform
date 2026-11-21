package com.art.util;

import com.art.entity.SysUser;
import com.art.entity.Student;
import com.art.entity.Teacher;
import com.art.mapper.StudentMapper;
import com.art.mapper.TeacherMapper;
import com.art.mapper.UserMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类：获取当前登录用户信息
 * 通过 sys_user 与 student/teacher 的 user_id 关联，解析当前学员/教师档案ID
  * @author liukang&&heyan
 */
@Component
public class SecurityUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Integer getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) return null;
        UserMapper userMapper = applicationContext.getBean(UserMapper.class);
        SysUser user = userMapper.findByUsername(username);
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前登录学员ID（通过 sys_user.user_id 关联 student.user_id）
     */
    public static Integer getCurrentStudentId() {
        Integer userId = getCurrentUserId();
        if (userId == null) return null;
        StudentMapper studentMapper = applicationContext.getBean(StudentMapper.class);
        Student s = studentMapper.selectByUserId(userId);
        return s != null ? s.getStudentId() : null;
    }

    /**
     * 获取当前登录教师ID（通过 sys_user.user_id 关联 teacher.user_id）
     */
    public static Integer getCurrentTeacherId() {
        Integer userId = getCurrentUserId();
        if (userId == null) return null;
        TeacherMapper teacherMapper = applicationContext.getBean(TeacherMapper.class);
        Teacher t = teacherMapper.selectByUserId(userId);
        return t != null ? t.getTeacherId() : null;
    }
}
