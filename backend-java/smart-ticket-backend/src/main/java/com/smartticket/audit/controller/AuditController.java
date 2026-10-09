package com.smartticket.audit.controller;

import com.smartticket.audit.dto.AuditLogResponse;
import com.smartticket.audit.dto.AuditQueryRequest;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.PageResult;
import com.smartticket.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<AuditLogResponse>> query(AuditQueryRequest request) {
        return Result.success(auditService.query(request));
    }
}