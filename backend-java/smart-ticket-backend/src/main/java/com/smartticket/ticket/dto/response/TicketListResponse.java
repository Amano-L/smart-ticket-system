package com.smartticket.ticket.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketListResponse {
    private Long id;
    private String ticketNo;
    private String title;
    private String type;
    private Integer status;
    private Integer priority;
    private Long assigneeId;
    private LocalDateTime createdAt;
}