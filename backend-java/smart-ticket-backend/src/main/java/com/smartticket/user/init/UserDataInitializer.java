package com.smartticket.user.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.assign.entity.AgentLoad;
import com.smartticket.assign.mapper.AgentLoadMapper;
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
    private final AgentLoadMapper agentLoadMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin", "admin123", "系统管理员", "ADMIN");
        createIfAbsent("agent", "agent123", "张客服", "AGENT");
        createIfAbsent("agent2", "agent123", "李客服", "AGENT");
        createIfAbsent("user", "user123", "李用户", "USER");
        createIfAbsent("supervisor", "super123", "王主管", "SUPERVISOR");
    }

    private void createIfAbsent(String username, String rawPassword, String realName, String roleCode) {
        // 1. 用户已存在则跳过
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (existing != null) {
            log.info("账号 [{}] 已存在，跳过初始化", username);
            return;
        }

        // 2. 查角色
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)
        );
        if (role == null) {
            log.error("未找到角色 [{}]，请检查 sys_role 表", roleCode);
            return;
        }

        // 3. 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 4. 绑定角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);

        // 5. 如果是 AGENT，顺便初始化负载记录
        if ("AGENT".equals(roleCode)) {
            AgentLoad load = new AgentLoad();
            load.setUserId(user.getId());
            load.setCurrentCount(0);
            load.setMaxCount(10);
            agentLoadMapper.insert(load);
            log.info("客服负载初始化：userId={}, maxCount=10", user.getId());
        }

        log.info("账号初始化完成：username={}，password={}，role={}", username, rawPassword, roleCode);
    }
}