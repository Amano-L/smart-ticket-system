package com.smartticket.user.controller;

import com.smartticket.common.Result;
import com.smartticket.security.LoginUser;
import com.smartticket.security.SecurityUtil;
import com.smartticket.user.dto.request.LoginRequest;
import com.smartticket.user.dto.response.LoginResponse;
import com.smartticket.user.dto.response.UserInfoResponse;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录、登出、当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @Operation(summary = "用户登出", description = "JWT 无状态，客户端删除 Token 即可")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        LoginUser loginUser = SecurityUtil.getCurrentUser();
        SysUser user = userService.findByUsername(loginUser.getUsername());

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoles(loginUser.getRoles());
        return Result.success(response);
    }
}