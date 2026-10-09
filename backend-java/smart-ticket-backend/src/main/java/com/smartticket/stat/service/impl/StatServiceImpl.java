package com.smartticket.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartticket.stat.dto.AgentWorkload;
import com.smartticket.stat.dto.StatOverviewResponse;
import com.smartticket.stat.dto.TypeCount;
import com.smartticket.stat.service.StatService;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final TicketMapper ticketMapper;

    @Override
    public StatOverviewResponse overview(LocalDateTime from, LocalDateTime to) {
        // 默认最近 30 天
        if (to == null) {
            to = LocalDateTime.now();
        }
        if (from == null) {
            from = to.minusDays(30);
        }

        // 1. 总工单数
        Long total = ticketMapper.selectCount(
                new LambdaQueryWrapper<Ticket>()
                        .between(Ticket::getCreatedAt, from, to)
        );

        // 2. 已解决数（status=2）
        Long resolved = ticketMapper.selectCount(
                new LambdaQueryWrapper<Ticket>()
                        .between(Ticket::getCreatedAt, from, to)
                        .eq(Ticket::getStatus, 2)
        );

        // 3. 平均处理时长（null 时置 0）
        Double avgHours = ticketMapper.avgHandleHours(from, to);
        if (avgHours == null) {
            avgHours = 0.0;
        }

        // 4. 类型分布转 Map
        List<TypeCount> typeCounts = ticketMapper.countByType(from, to);
        Map<String, Long> typeDistribution = typeCounts.stream()
                .collect(Collectors.toMap(TypeCount::getType, TypeCount::getCnt));

        // 5. 客服工作量
        List<AgentWorkload> workloads = ticketMapper.agentWorkload(from, to);

        // 组装响应
        StatOverviewResponse resp = new StatOverviewResponse();
        resp.setTotalTickets(total);
        resp.setResolvedTickets(resolved);
        resp.setAvgHandleHours(avgHours);
        resp.setTypeDistribution(typeDistribution);
        resp.setAgentWorkload(workloads);
        return resp;
    }
}