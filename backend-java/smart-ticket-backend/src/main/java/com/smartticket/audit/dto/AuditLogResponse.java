package com.smartticket.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String result;
    private String detail;
    private String traceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}