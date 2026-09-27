package com.smartticket.user.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.user.entity.SysRole;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.entity.SysUserRole;
import com.smartticket.user.mapper.SysRoleMapper;
import com.smartticket.user.mapper.SysUserMapper;
import com.smartticket.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin", "admin123", "系统管理员", "ADMIN");
        createIfAbsent("agent", "agent123", "张客服", "AGENT");
        createIfAbsent("user", "user123", "李用户", "USER");
    }

    private void createIfAbsent(String username, String rawPassword, String realName, String roleCode) {
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existing != null) {
            log.info("账号 [{}] 已存在，跳过初始化", username);
            return;
        }

        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)
        );
        if (role == null) {
            log.error("未找到角色 [{}]，请检查 sys_role 表", roleCode);
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setStatus(1);
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);

        log.info("账号初始化完成：username={}，password={}，role={}", username, rawPassword, roleCode);
    }
}