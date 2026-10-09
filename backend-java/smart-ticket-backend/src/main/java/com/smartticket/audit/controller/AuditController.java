package com.smartticket.audit.controller;

import com.smartticket.audit.dto.AuditLogResponse;
import com.smartticket.audit.dto.AuditQueryRequest;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.PageResult;
import com.smartticket.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审计日志", description = "操作审计查询（仅 ADMIN）")
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "审计日志查询（ADMIN）",
            description = "支持按 userId / traceId / 时间范围过滤")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<AuditLogResponse>> query(AuditQueryRequest request) {
        return Result.success(auditService.query(request));
    }
}