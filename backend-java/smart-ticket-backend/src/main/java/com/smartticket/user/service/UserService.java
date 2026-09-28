package com.smartticket.user.service;

import com.smartticket.user.dto.request.LoginRequest;
import com.smartticket.user.dto.response.LoginResponse;
import com.smartticket.user.entity.SysUser;

import java.util.List;

public interface UserService {

    LoginResponse login(LoginRequest request);

    SysUser findByUsername(String username);

    List<String> findRoleCodesByUserId(Long userId);


}