package com.smartticket.ticket.service;

import com.smartticket.assign.dto.AssignRequest;
import com.smartticket.common.PageResult;
import com.smartticket.ticket.dto.request.*;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;

public interface TicketService {

    TicketDetailResponse create(TicketCreateRequest request, String requestId);

    PageResult<TicketListResponse> list(TicketQueryRequest query);

    TicketDetailResponse detail(Long id);

    TicketDetailResponse update(Long id, TicketUpdateRequest request);

    TicketDetailResponse transition(Long id, TicketTransitionRequest request);

    void assign(Long id, AssignRequest request);

    void reply(Long id, TicketReplyRequest request);
}