package com.smartticket.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 失效"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "状态冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    SYSTEM_ERROR(500, "系统异常"),

    LOGIN_ERROR(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已禁用"),

    TICKET_NOT_FOUND(2001, "工单不存在"),
    TICKET_STATUS_ERROR(2002, "工单状态不允许此操作"),
    TICKET_DUPLICATE(2003, "重复提交工单"),

    NO_AGENT(3001, "无可用客服"),

    AI_FALLBACK(4001, "AI 服务调用失败（已降级）");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}