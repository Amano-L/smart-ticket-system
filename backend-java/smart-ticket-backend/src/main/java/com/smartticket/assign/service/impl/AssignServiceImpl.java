package com.smartticket.assign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smartticket.assign.entity.AgentLoad;
import com.smartticket.assign.mapper.AgentLoadMapper;
import com.smartticket.assign.service.AssignService;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.user.entity.SysRole;
import com.smartticket.user.entity.SysUserRole;
import com.smartticket.user.mapper.SysRoleMapper;
import com.smartticket.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignServiceImpl implements AssignService {

    private static final String ROLE_AGENT = "AGENT";

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AgentLoadMapper agentLoadMapper;
    private final TicketMapper ticketMapper;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoAssign(Long ticketId) {
        // 1. 查 AGENT 角色
        SysRole agentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ROLE_AGENT)
        );
        if (agentRole == null) {
            throw new BizException(ErrorCode.NO_AGENT);
        }

        // 2. 查该角色下的所有 userId
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, agentRole.getId())
        );
        if (userRoles.isEmpty()) {
            throw new BizException(ErrorCode.NO_AGENT);
        }
        List<Long> agentUserIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toList());

        // 3. 查这些客服中未满载的负载记录
        List<AgentLoad> loads = agentLoadMapper.selectList(
                new LambdaQueryWrapper<AgentLoad>()
                        .in(AgentLoad::getUserId, agentUserIds)
                        .apply("current_count < max_count")
        );
        if (loads.isEmpty()) {
            throw new BizException(ErrorCode.NO_AGENT);
        }

        // 4. 选 current_count 最小的
        AgentLoad chosen = loads.stream()
                .min(Comparator.comparingInt(AgentLoad::getCurrentCount))
                .orElseThrow(() -> new BizException(ErrorCode.NO_AGENT));

        // 5. 原子更新负载 +1
        UpdateWrapper<AgentLoad> loadWrapper = new UpdateWrapper<>();
        loadWrapper.eq("user_id", chosen.getUserId())
                .setSql("current_count = current_count + 1");
        agentLoadMapper.update(null, loadWrapper);

        // 6. 更新工单的 assignee_id
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setAssigneeId(chosen.getUserId());
        ticketMapper.updateById(ticket);

        log.info("自动派单：ticketId={}, assigneeId={}", ticketId, chosen.getUserId());

        // 记录日志
        auditService.record("AUTO_ASSIGN", "TICKET", ticketId, "SUCCESS", "自动派单给 userId=" + chosen.getUserId());
        return chosen.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAssign(Long ticketId, Long assigneeId, String remark) {
        // 1. 查工单
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 2. 校验目标客服是 AGENT 角色
        SysRole agentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ROLE_AGENT)
        );
        if (agentRole == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "目标用户不是客服");
        }
        Long roleCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, assigneeId)
                        .eq(SysUserRole::getRoleId, agentRole.getId())
        );
        if (roleCount == null || roleCount == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "目标用户不是客服");
        }

        Long oldAssigneeId = ticket.getAssigneeId();

        // 3. 更新工单
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setAssigneeId(assigneeId);
        ticketMapper.updateById(update);

        // 4. 旧客服负载 -1（如果原来有）
        if (oldAssigneeId != null && !oldAssigneeId.equals(assigneeId)) {
            UpdateWrapper<AgentLoad> oldWrapper = new UpdateWrapper<>();
            oldWrapper.eq("user_id", oldAssigneeId)
                    .gt("current_count", 0)
                    .setSql("current_count = current_count - 1");
            agentLoadMapper.update(null, oldWrapper);
        }

        // 5. 新客服负载 +1
        UpdateWrapper<AgentLoad> newWrapper = new UpdateWrapper<>();
        newWrapper.eq("user_id", assigneeId)
                .setSql("current_count = current_count + 1");
        agentLoadMapper.update(null, newWrapper);

        log.info("手动改派：ticketId={}, oldAssigneeId={}, newAssigneeId={}, remark={}",
                ticketId, oldAssigneeId, assigneeId, remark);

        // 6.记录日志
        String oldDesc = oldAssigneeId == null ? "无" : oldAssigneeId.toString();
        auditService.record("MANUAL_ASSIGN", "TICKET", ticketId, "SUCCESS", "从 userId=" + oldAssigneeId + " 改派给 userId=" + assigneeId);
    }
}