package com.smartticket.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    private String content;

    private Boolean useAiSuggestion = false;
}