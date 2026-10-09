package com.smartticket.stat.dto;

import lombok.Data;

@Data
public class TypeCount {
    private String type;    // 工单类型：REFUND/TECH/...
    private Long cnt;       // 该类型数量（用 cnt 避免关键字冲突）
}