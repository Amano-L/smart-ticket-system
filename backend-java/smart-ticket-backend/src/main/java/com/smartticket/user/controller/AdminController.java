package com.smartticket.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.common.Result;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;

    /**
     * 只有 ADMIN 角色能访问
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysUser>> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId)
        );
        // 脱敏：不返回密码
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * ADMIN 或 SUPERVISOR 都能访问
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public Result<String> dashboard() {
        return Result.success("管理看板数据");
    }
}