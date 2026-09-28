package com.smartticket.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.assign.dto.AssignRequest;
import com.smartticket.assign.service.AssignService;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.common.PageResult;
import com.smartticket.security.LoginUser;
import com.smartticket.security.SecurityUtil;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketTransitionRequest;
import com.smartticket.ticket.dto.request.TicketUpdateRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketFlowResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketFlow;
import com.smartticket.ticket.enums.TicketStatus;
import com.smartticket.ticket.enums.TicketType;
import com.smartticket.ticket.mapper.TicketFlowMapper;
import com.smartticket.ticket.mapper.TicketMapper;
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

import static com.smartticket.common.ErrorCode.PARAM_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String IDEMPOTENT_KEY_PREFIX = "ticket:idempotent:";

    private final TicketMapper ticketMapper;
    private final TicketFlowMapper ticketFlowMapper;
    private final TicketNoGenerator ticketNoGenerator;
    private final StringRedisTemplate redisTemplate;
    private final AssignService assignService;

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

        // 5. 保存
        ticketMapper.insert(ticket);

        // 6. 自动派单
        assignService.autoAssign(ticket.getId());

        // 7. 写幂等标记
        redisTemplate.opsForValue().set(key, ticket.getId().toString(), Duration.ofMinutes(5));

        // 8. 返回详情
        return detail(ticket.getId());
    }

    @Override
    public PageResult<TicketListResponse> list(TicketQueryRequest query) {
        LoginUser currentUser = SecurityUtil.getCurrentUser();
        List<String> roles = currentUser.getRoles();

        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        // 角色数据过滤
        if (roles.contains("ADMIN") || roles.contains("SUPERVISOR")) {
            // 看全部
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

    @Override
    public TicketDetailResponse detail(Long id) {
        // 1. 查工单
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 2. 权限校验
        checkReadPermission(ticket);

        // 3. 转 DTO
        TicketDetailResponse resp = new TicketDetailResponse();
        BeanUtils.copyProperties(ticket, resp);

        // 4. 查流转记录（按时间升序）
        List<TicketFlow> flowList = ticketFlowMapper.selectList(
                new LambdaQueryWrapper<TicketFlow>()
                        .eq(TicketFlow::getTicketId, id)
                        .orderByAsc(TicketFlow::getCreatedAt)
        );

        // 5. 转成 DTO 塞进详情
        List<TicketFlowResponse> flowResponses = flowList.stream()
                .map(f -> {
                    TicketFlowResponse fr = new TicketFlowResponse();
                    BeanUtils.copyProperties(f, fr);
                    return fr;
                })
                .collect(Collectors.toList());
        resp.setFlows(flowResponses);

        return resp;
    }

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

        return detail(id);
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
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "非法状态: " + request.getToStatus());
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

        // 9. 返回详情
        return detail(id);
    }

    @Override
    public void assign(Long id, AssignRequest request) {
        assignService.manualAssign(id, request.getAssigneeId(), request.getRemark());
    }
}