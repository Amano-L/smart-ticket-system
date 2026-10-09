package com.smartticket.stat.dto;

import lombok.Data;

@Data
public class AgentWorkload {
    private Long userId;    // 客服用户ID
    private String name;    // 客服姓名
    private Long count;     // 处理工单数（对齐文档字段名）
}