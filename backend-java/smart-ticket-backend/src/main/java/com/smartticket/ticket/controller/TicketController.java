package com.smartticket.ticket.controller;

import com.smartticket.assign.dto.AssignRequest;
import com.smartticket.common.PageResult;
import com.smartticket.common.Result;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketReplyRequest;
import com.smartticket.ticket.dto.request.TicketTransitionRequest;
import com.smartticket.ticket.dto.request.TicketUpdateRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;
import com.smartticket.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工单管理", description = "工单创建、列表、详情、编辑、状态流转、派单、回复")
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "创建工单（USER）",
            description = "支持 X-Request-Id 幂等，创建后自动 AI 分类和派单")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<TicketDetailResponse> create(
            @Valid @RequestBody TicketCreateRequest request,
            @Parameter(description = "幂等 ID，UUID（5分钟内相同 ID 返回同一工单）")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Result.success(ticketService.create(request, requestId));
    }

    @Operation(summary = "工单列表",
            description = "按角色过滤：USER 看自己创建的、AGENT 看分配给自己的、SUPERVISOR/ADMIN 看全部")
    @GetMapping
    public Result<PageResult<TicketListResponse>> list(TicketQueryRequest query) {
        return Result.success(ticketService.list(query));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public Result<TicketDetailResponse> detail(
            @Parameter(description = "工单 ID") @PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @Operation(summary = "编辑工单（AGENT / SUPERVISOR / ADMIN）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    public Result<TicketDetailResponse> update(
            @Parameter(description = "工单 ID") @PathVariable Long id,
            @Valid @RequestBody TicketUpdateRequest request) {
        return Result.success(ticketService.update(id, request));
    }

    @Operation(summary = "状态流转",
            description = "0待处理 → 1处理中 → 2已解决 → 3已关闭，非法流转返回 2002")
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    public Result<TicketDetailResponse> transition(
            @Parameter(description = "工单 ID") @PathVariable Long id,
            @Valid @RequestBody TicketTransitionRequest request) {
        return Result.success(ticketService.transition(id, request));
    }

    @Operation(summary = "手动派单（SUPERVISOR）")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Result<TicketDetailResponse> assign(
            @Parameter(description = "工单 ID") @PathVariable Long id,
            @Valid @RequestBody AssignRequest request) {
        ticketService.assign(id, request);
        return Result.success(ticketService.detail(id));
    }

    @Operation(summary = "客服回复（AGENT）")
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('AGENT')")
    public Result<Void> reply(
            @Parameter(description = "工单 ID") @PathVariable Long id,
            @Valid @RequestBody TicketReplyRequest request) {
        ticketService.reply(id, request);
        return Result.success();
    }
}