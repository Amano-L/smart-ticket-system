package com.smartticket.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.common.Result;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统管理", description = "用户管理、管理看板（仅 ADMIN）")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;

    @Operation(summary = "查询用户列表（仅 ADMIN）")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysUser>> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId)
        );
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @Operation(summary = "管理看板（ADMIN / SUPERVISOR）")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public Result<String> dashboard() {
        return Result.success("管理看板数据");
    }
}