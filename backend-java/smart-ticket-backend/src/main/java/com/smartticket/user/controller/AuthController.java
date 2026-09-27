package com.smartticket.user.controller;

import com.smartticket.common.Result;
import com.smartticket.security.LoginUser;
import com.smartticket.security.SecurityUtil;
import com.smartticket.user.dto.request.LoginRequest;
import com.smartticket.user.dto.response.LoginResponse;
import com.smartticket.user.dto.response.UserInfoResponse;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

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

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，服务端不维护会话，客户端删除 token 即可
        return Result.success();
    }
}