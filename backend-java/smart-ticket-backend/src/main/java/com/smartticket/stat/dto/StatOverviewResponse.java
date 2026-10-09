package com.smartticket.stat.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StatOverviewResponse {
    private Long totalTickets;              // 总工单数
    private Long resolvedTickets;           // 已解决数（status=2）
    private Double avgHandleHours;          // 平均处理时长（小时）
    private Map<String, Long> typeDistribution;   // 类型分布
    private List<AgentWorkload> agentWorkload;    // 客服工作量
}