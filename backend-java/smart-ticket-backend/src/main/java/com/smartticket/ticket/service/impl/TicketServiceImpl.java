package com.smartticket.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.ai.service.AiService;
import com.smartticket.assign.dto.AssignRequest;
import com.smartticket.assign.service.AssignService;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.common.PageResult;
import com.smartticket.security.LoginUser;
import com.smartticket.security.SecurityUtil;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketReplyRequest;
import com.smartticket.ticket.dto.request.TicketTransitionRequest;
import com.smartticket.ticket.dto.request.TicketUpdateRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketFlowResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketFlow;
import com.smartticket.ticket.entity.TicketReply;
import com.smartticket.ticket.enums.TicketStatus;
import com.smartticket.ticket.enums.TicketType;
import com.smartticket.ticket.mapper.TicketFlowMapper;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.ticket.mapper.TicketReplyMapper;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.util.TicketNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String IDEMPOTENT_KEY_PREFIX = "ticket:idempotent:";

    private final TicketMapper ticketMapper;
    private final TicketFlowMapper ticketFlowMapper;
    private final TicketReplyMapper ticketReplyMapper;
    private final TicketNoGenerator ticketNoGenerator;
    private final StringRedisTemplate redisTemplate;
    private final AssignService assignService;
    private final AiService aiService;
    private final AuditService auditService;

    // ==================== 创建 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse create(TicketCreateRequest request, String requestId) {
        // 1. 校验 requestId
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "缺少 X-Request-Id 请求头");
        }

        // 2. 幂等检查
        String key = IDEMPOTENT_KEY_PREFIX + requestId;
        String cachedTicketId = redisTemplate.opsForValue().get(key);
        if (cachedTicketId != null) {
            return detail(Long.valueOf(cachedTicketId));
        }

        // 3. 取当前用户
        LoginUser currentUser = SecurityUtil.getCurrentUser();

        // 4. 构造工单
        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNoGenerator.generate());
        ticket.setTitle(request.getTitle());
        ticket.setContent(request.getContent());
        ticket.setType(TicketType.UNKNOWN.name());
        ticket.setStatus(TicketStatus.PENDING.getCode());
        ticket.setPriority(request.getPriority() == null ? 1 : request.getPriority());
        ticket.setCreatorId(currentUser.getUserId());
        ticketMapper.insert(ticket);

        // 5. AI 分类（失败降级 UNKNOWN，不阻塞创建）
        String type = aiService.classifyForTicket(ticket.getId(), ticket.getTitle(), ticket.getContent());
        if (!"UNKNOWN".equals(type)) {
            ticket.setType(type);
            ticketMapper.updateById(ticket);
        }

        // 6. 自动派单
        assignService.autoAssign(ticket.getId());

        // 7. 写幂等标记
        redisTemplate.opsForValue().set(key, ticket.getId().toString(), Duration.ofMinutes(5));

        // 8. 审计
        auditService.record("CREATE_TICKET", "TICKET", ticket.getId(), "SUCCESS",
                "创建工单 " + ticket.getTicketNo());

        return detail(ticket.getId());
    }

    // ==================== 列表 ====================

    @Override
    public PageResult<TicketListResponse> list(TicketQueryRequest query) {
        LoginUser currentUser = SecurityUtil.getCurrentUser();
        List<String> roles = currentUser.getRoles();

        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        // 角色数据过滤
        if (roles.contains("ADMIN") || roles.contains("SUPERVISOR")) {
            // 全部可见
        } else if (roles.contains("AGENT")) {
            wrapper.eq(Ticket::getAssigneeId, currentUser.getUserId());
        } else {
            wrapper.eq(Ticket::getCreatorId, currentUser.getUserId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Ticket::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Ticket::getType, query.getType());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(Ticket::getAssigneeId, query.getAssigneeId());
        }
        wrapper.orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = new Page<>(query.getPage(), query.getSize());
        Page<Ticket> result = ticketMapper.selectPage(page, wrapper);

        List<TicketListResponse> list = result.getRecords().stream().map(t -> {
            TicketListResponse r = new TicketListResponse();
            BeanUtils.copyProperties(t, r);
            return r;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 详情 ====================

    @Override
    public TicketDetailResponse detail(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }
        checkReadPermission(ticket);

        TicketDetailResponse resp = new TicketDetailResponse();
        BeanUtils.copyProperties(ticket, resp);

        // 查流转记录
        List<TicketFlow> flowList = ticketFlowMapper.selectList(
                new LambdaQueryWrapper<TicketFlow>()
                        .eq(TicketFlow::getTicketId, id)
                        .orderByAsc(TicketFlow::getCreatedAt)
        );
        List<TicketFlowResponse> flowResponses = flowList.stream()
                .map(this::toFlowResponse)
                .collect(Collectors.toList());
        resp.setFlows(flowResponses);

        return resp;
    }

    // ==================== 编辑 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse update(Long id, TicketUpdateRequest request) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }
        checkWritePermission(ticket);

        if (StringUtils.hasText(request.getTitle())) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        ticketMapper.updateById(ticket);

        auditService.record("UPDATE_TICKET", "TICKET", id, "SUCCESS",
                "编辑工单 " + ticket.getTicketNo());

        return detail(id);
    }

    // ==================== 状态流转 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse transition(Long id, TicketTransitionRequest request) {
        // 1. 查工单
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 2. 权限校验
        checkWritePermission(ticket);

        // 3. 转换目标状态
        TicketStatus toStatus;
        try {
            toStatus = TicketStatus.fromCode(request.getToStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "非法状态: " + request.getToStatus());
        }

        // 4. 转换当前状态
        TicketStatus fromStatus = TicketStatus.fromCode(ticket.getStatus());

        // 5. 校验流转合法性
        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new BizException(ErrorCode.TICKET_STATUS_ERROR);
        }

        // 6. 记录旧状态
        Integer oldStatus = ticket.getStatus();

        // 7. 更新工单
        ticket.setStatus(toStatus.getCode());
        ticketMapper.updateById(ticket);

        // 8. 写流转记录
        TicketFlow flow = new TicketFlow();
        flow.setTicketId(id);
        flow.setFromStatus(oldStatus);
        flow.setToStatus(toStatus.getCode());
        flow.setOperatorId(SecurityUtil.getCurrentUserId());
        flow.setRemark(request.getRemark());
        ticketFlowMapper.insert(flow);

        // 9. 审计
        auditService.record("TRANSITION", "TICKET", id, "SUCCESS",
                "状态从 " + oldStatus + " 变为 " + toStatus.getCode());

        return detail(id);
    }

    // ==================== 手动改派 ====================

    @Override
    public void assign(Long id, AssignRequest request) {
        assignService.manualAssign(id, request.getAssigneeId(), request.getRemark());
    }

    // ==================== 客服回复 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, TicketReplyRequest request) {
        // 1. 查工单
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 2. 权限校验
        checkWritePermission(ticket);

        // 3. 保存回复
        TicketReply reply = new TicketReply();
        reply.setTicketId(id);
        reply.setOperatorId(SecurityUtil.getCurrentUserId());
        reply.setContent(request.getContent());
        reply.setUseAiSuggestion(Boolean.TRUE.equals(request.getUseAiSuggestion()) ? 1 : 0);
        ticketReplyMapper.insert(reply);

        // 4. 审计
        auditService.record("REPLY", "TICKET", id, "SUCCESS",
                "客服回复工单 " + ticket.getTicketNo());
    }

    // ==================== 私有方法 ====================

    private TicketFlowResponse toFlowResponse(TicketFlow flow) {
        TicketFlowResponse fr = new TicketFlowResponse();
        BeanUtils.copyProperties(flow, fr);
        return fr;
    }

    private void checkReadPermission(Ticket ticket) {
        LoginUser currentUser = SecurityUtil.getCurrentUser();
        List<String> roles = currentUser.getRoles();
        if (roles.contains("ADMIN") || roles.contains("SUPERVISOR")) {
            return;
        }
        if (roles.contains("AGENT")) {
            if (!currentUser.getUserId().equals(ticket.getAssigneeId())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        if (!currentUser.getUserId().equals(ticket.getCreatorId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private void checkWritePermission(Ticket ticket) {
        LoginUser currentUser = SecurityUtil.getCurrentUser();
        List<String> roles = currentUser.getRoles();
        if (roles.contains("ADMIN") || roles.contains("SUPERVISOR")) {
            return;
        }
        if (roles.contains("AGENT")) {
            if (!currentUser.getUserId().equals(ticket.getAssigneeId())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN);
    }
}