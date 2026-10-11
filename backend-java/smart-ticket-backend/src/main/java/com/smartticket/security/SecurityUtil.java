package com.smartticket.security;

import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/*
 * 获取当前登录用户的工具类
 */
public class SecurityUtil {

    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return (LoginUser) auth.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}