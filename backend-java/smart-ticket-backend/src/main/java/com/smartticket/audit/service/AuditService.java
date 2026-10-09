package com.smartticket.audit.service;

import com.smartticket.audit.dto.AuditLogResponse;
import com.smartticket.audit.dto.AuditQueryRequest;
import com.smartticket.common.PageResult;

public interface AuditService {

    // 记录审计日志
    void record(String action, String targetType, Long targetId, String result, String detail);

    // 查询审计日志
    PageResult<AuditLogResponse> query(AuditQueryRequest request);

}
