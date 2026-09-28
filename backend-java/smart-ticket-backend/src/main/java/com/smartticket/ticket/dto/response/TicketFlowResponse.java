package com.smartticket.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketFlowResponse {
    private Long id;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}