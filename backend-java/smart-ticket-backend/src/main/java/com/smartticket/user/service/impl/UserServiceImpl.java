package com.smartticket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.security.JwtUtil;
import com.smartticket.user.dto.request.LoginRequest;
import com.smartticket.user.dto.response.LoginResponse;
import com.smartticket.user.entity.SysRole;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.entity.SysUserRole;
import com.smartticket.user.mapper.SysRoleMapper;
import com.smartticket.user.mapper.SysUserMapper;
import com.smartticket.user.mapper.SysUserRoleMapper;
import com.smartticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = findByUsername(request.getUsername());
        if (user == null) {
            throw new BizException(ErrorCode.LOGIN_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_ERROR);
        }

        List<String> roles = findRoleCodesByUserId(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoles(roles);
        return response;
    }

    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
    }

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).toList();
    }
}