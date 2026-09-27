package com.smartticket.ticket.controller;

import com.smartticket.common.PageResult;
import com.smartticket.common.Result;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketUpdateRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;
import com.smartticket.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<TicketDetailResponse> create(
            @Valid @RequestBody TicketCreateRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Result.success(ticketService.create(request, requestId));
    }

    @GetMapping
    public Result<PageResult<TicketListResponse>> list(TicketQueryRequest query) {
        return Result.success(ticketService.list(query));
    }

    @GetMapping("/{id}")
    public Result<TicketDetailResponse> detail(@PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    public Result<TicketDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TicketUpdateRequest request) {
        return Result.success(ticketService.update(id, request));
    }
}