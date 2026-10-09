package com.smartticket.ai.dto.request;

import lombok.Data;

@Data
public class ClassifyRequest {
    private Long ticketId;
    private String title;
    private String content;
}