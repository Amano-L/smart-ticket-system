package com.smartticket.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketReplyResponse {
    private Long id;
    private Long ticketId;
    private Long operatorId;
    private String content;
    private Integer useAiSuggestion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}