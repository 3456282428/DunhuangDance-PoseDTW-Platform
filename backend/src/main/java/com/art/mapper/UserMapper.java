package com.art.mapper;

import com.art.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface UserMapper {
    SysUser findByUsername(@Param("username") String username);
    List<SysUser> selectAll();
    SysUser selectById(@Param("userId") Integer userId);
    int insert(SysUser user);
    int update(SysUser user);
    int deleteById(@Param("userId") Integer userId);
    List<SysUser> selectByCondition(@Param("keyword") String keyword,
                                    @Param("role") String role,
                                    @Param("status") Integer status);
}