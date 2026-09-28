package com.smartticket.ticket.service;

import com.smartticket.common.PageResult;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketTransitionRequest;
import com.smartticket.ticket.dto.request.TicketUpdateRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;

public interface TicketService {

    TicketDetailResponse create(TicketCreateRequest request, String requestId);

    PageResult<TicketListResponse> list(TicketQueryRequest query);

    TicketDetailResponse detail(Long id);

    TicketDetailResponse update(Long id, TicketUpdateRequest request);

    TicketDetailResponse transition(Long id, TicketTransitionRequest request);
}