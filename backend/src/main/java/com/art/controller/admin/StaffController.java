package com.art.controller.admin;

import com.art.common.BusinessException;
import com.art.common.MobileValidator;
import com.art.common.Result;
import com.art.entity.SysUser;
import com.art.entity.Teacher;
import com.art.mapper.TeacherMapper;
import com.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运营管理端 - 师资信息管理（教师全生命周期）
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
public class StaffController {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<List<Teacher>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String employType) {
        return Result.success(teacherMapper.selectByCondition(keyword, employType));
    }

    @GetMapping("/{teacherId}")
    public Result<Teacher> detail(@PathVariable Integer teacherId) {
        return Result.success(teacherMapper.selectById(teacherId));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> create(@RequestBody Teacher teacher) {
        if (!StringUtils.hasText(teacher.getName())) throw new BusinessException("教师姓名不能为空");
        MobileValidator.require(teacher.getPhone(), "联系电话");
        MobileValidator.require(teacher.getEmergencyPhone(), "紧急电话");
        if (teacher.getEmployType() == null) teacher.setEmployType("全职");
        if (teacher.getStatus() == null) teacher.setStatus(1);
        // 自动生成工号 T001、T002 ...
        if (!StringUtils.hasText(teacher.getTeacherNo())) {
            int max = teacherMapper.selectAll().stream()
                    .map(t -> t.getTeacherNo() != null && t.getTeacherNo().startsWith("T")
                            ? t.getTeacherNo().substring(1) : "")
                    .filter(s -> s.matches("\\d+"))
                    .mapToInt(Integer::parseInt).max().orElse(0);
            teacher.setTeacherNo(String.format("T%03d", max + 1));
        }
        teacherMapper.insert(teacher);
        // 同步创建教师登录账号，用户名为教师编号
        String username = teacher.getTeacherNo();
        if (userMapper.findByUsername(username) == null) {
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRole("TEACHER");
            u.setRealName(teacher.getName());
            u.setPhone(teacher.getPhone());
            u.setStatus(1);
            userMapper.insert(u);
            teacher.setUserId(u.getUserId());
            teacherMapper.update(teacher);
        }
        syncTeacherCourses(teacher);
        return Result.success(teacher.getTeacherId());
    }

    @PutMapping("/{teacherId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@PathVariable Integer teacherId, @RequestBody Teacher teacher) {
        Teacher exist = teacherMapper.selectById(teacherId);
        if (exist == null) throw new BusinessException("教师不存在");
        MobileValidator.require(teacher.getPhone(), "联系电话");
        MobileValidator.require(teacher.getEmergencyPhone(), "紧急电话");
        teacher.setTeacherId(teacherId);
        teacher.setUserId(exist.getUserId());
        teacherMapper.update(teacher);
        syncTeacherCourses(teacher);
        return Result.success();
    }

    /** 将前端提交的擅长课程逗号串同步到 teacher_course 关联表（全量替换） */
    private void syncTeacherCourses(Teacher teacher) {
        Integer tid = teacher.getTeacherId();
        if (tid == null) return;
        teacherMapper.deleteCoursesByTeacher(tid);
        String ids = teacher.getCourseIds();
        if (!StringUtils.hasText(ids)) return;
        for (String part : ids.split(",")) {
            String p = part.trim();
            if (p.matches("\\d+")) teacherMapper.insertTeacherCourse(tid, Integer.valueOf(p));
        }
    }

    /** 重置教师账号密码为 123456 */
    @PostMapping("/{teacherId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Integer teacherId) {
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null || t.getUserId() == null) throw new BusinessException("该教师尚未关联登录账号");
        SysUser u = userMapper.selectById(t.getUserId());
        if (u == null) throw new BusinessException("账号不存在");
        u.setPassword(passwordEncoder.encode("123456"));
        userMapper.update(u);
        return Result.success();
    }

    /** 离职：状态置为离职并停用账号（班级/排课保留，提示教务重新分配） */
    @PostMapping("/{teacherId}/leave")
    public Result<Void> leave(@PathVariable Integer teacherId) {
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) throw new BusinessException("教师不存在");
        t.setEmployType("离职");
        t.setStatus(0);
        teacherMapper.update(t);
        if (t.getUserId() != null) {
            SysUser u = userMapper.selectById(t.getUserId());
            if (u != null) { u.setStatus(0); userMapper.update(u); }
        }
        return Result.success();
    }

    @DeleteMapping("/{teacherId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Integer teacherId) {
        teacherMapper.deleteCoursesByTeacher(teacherId);
        teacherMapper.deleteById(teacherId);
        return Result.success();
    }
}
