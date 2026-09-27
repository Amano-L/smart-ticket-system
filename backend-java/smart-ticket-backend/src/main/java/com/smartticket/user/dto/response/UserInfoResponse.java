package com.smartticket.user.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoResponse {
    private Long userId;
    private String username;
    private String realName;
    private List<String> roles;
}