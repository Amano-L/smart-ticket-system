package com.smartticket.ticket.enums;

import lombok.Getter;

@Getter
public enum TicketStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已解决"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String desc;

    TicketStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 判断是否可以转换到目标状态
    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING;
            case PROCESSING -> target == RESOLVED;
            case RESOLVED -> target == CLOSED;
            case CLOSED -> false;
        };
    }

    // 从code获取状态
    public static TicketStatus fromCode(Integer code) {
        for (TicketStatus s : values()) {
            if (s.getCode() == code) return s;
        }
        throw new IllegalArgumentException("Invalid status: " + code);
    }
}
