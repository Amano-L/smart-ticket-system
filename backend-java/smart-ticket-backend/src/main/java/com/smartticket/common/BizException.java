package com.smartticket.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    // 从 ErrorCode 枚举创建异常
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    // 从自定义错误码创建异常
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}