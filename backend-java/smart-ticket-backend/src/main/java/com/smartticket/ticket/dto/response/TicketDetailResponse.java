package com.smartticket.ticket.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketDetailResponse {
    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String type;
    private Integer status;
    private Integer priority;
    private Long creatorId;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}