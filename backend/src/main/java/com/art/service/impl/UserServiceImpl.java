package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.common.MobileValidator;
import com.art.common.PageResult;
import com.art.dto.RegisterDTO;
import com.art.dto.UserDTO;
import com.art.entity.SysUser;
import com.art.entity.Student;
import com.art.mapper.StudentMapper;
import com.art.mapper.UserMapper;
import com.art.service.UserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StudentMapper studentMapper;

    @Override
    public SysUser findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public PageResult<SysUser> pageUsers(Integer pageNum, Integer pageSize, String keyword, String role, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysUser> list = userMapper.selectByCondition(keyword, role, status);
        PageInfo<SysUser> pageInfo = new PageInfo<>(list);
        return PageResult.of(pageInfo.getTotal(), pageInfo.getList(), pageNum, pageSize);
    }

    @Override
    public void createUser(UserDTO userDTO) {
        if (userMapper.findByUsername(userDTO.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        MobileValidator.require(userDTO.getPhone(), "手机号");
        SysUser user = new SysUser();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());
        user.setRealName(userDTO.getRealName());
        user.setPhone(userDTO.getPhone());
        user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : 1);
        userMapper.insert(user);
    }

    @Override
    public void updateUser(Integer userId, UserDTO userDTO) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        MobileValidator.require(userDTO.getPhone(), "手机号");
        user.setUsername(userDTO.getUsername());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        user.setRole(userDTO.getRole());
        user.setRealName(userDTO.getRealName());
        user.setPhone(userDTO.getPhone());
        user.setStatus(userDTO.getStatus());
        userMapper.update(user);
    }

    @Override
    public void deleteUser(Integer userId) {
        userMapper.deleteById(userId);
    }

    @Override
    public void resetPassword(Integer userId, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (newPassword == null || newPassword.length() < 6) throw new BusinessException("新密码至少6位");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    @Override
    public void changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = userMapper.findByUsername(username);
        if (user == null) throw new BusinessException("用户不存在");
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) throw new BusinessException("新密码至少6位");
        if (newPassword.equals(oldPassword)) throw new BusinessException("新密码不能与原密码相同");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        if (userMapper.findByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        MobileValidator.requireNonNull(registerDTO.getPhone(), "手机号");
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole("STUDENT");
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        // 同时创建关联的学员档案，并通过 user_id 关联账号
        Student student = new Student();
        student.setUserId(user.getUserId());
        student.setStudentNo("S" + (1000 + user.getUserId()));
        student.setName(registerDTO.getRealName());
        student.setGender("未知");
        student.setParentPhone(registerDTO.getPhone());
        student.setEnrollDate(java.time.LocalDate.now());
        student.setStatus(1);
        studentMapper.insert(student);
    }
}