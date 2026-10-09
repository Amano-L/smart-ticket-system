package com.smartticket.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.audit.dto.AuditLogResponse;
import com.smartticket.audit.dto.AuditQueryRequest;
import com.smartticket.audit.entity.AuditLog;
import com.smartticket.audit.mapper.AuditLogMapper;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.PageResult;
import com.smartticket.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void record(String action, String targetType, Long targetId, String result, String detail) {
        Long userId;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            userId = null;
        }

        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = "";
        }

        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setResult(result);
        log.setDetail(detail);
        log.setTraceId(traceId);

        auditLogMapper.insert(log);
    }

    @Override
    public PageResult<AuditLogResponse> query(AuditQueryRequest request) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (request.getUserId() != null) {
            wrapper.eq(AuditLog::getUserId, request.getUserId());
        }
        if (StringUtils.hasText(request.getTraceId())) {
            wrapper.eq(AuditLog::getTraceId, request.getTraceId());
        }
        if (request.getFrom() != null) {
            wrapper.ge(AuditLog::getCreatedAt, request.getFrom());
        }
        if (request.getTo() != null) {
            wrapper.le(AuditLog::getCreatedAt, request.getTo());
        }

        wrapper.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> page = new Page<>(request.getPage(), request.getSize());
        Page<AuditLog> result = auditLogMapper.selectPage(page, wrapper);

        List<AuditLogResponse> list = result.getRecords().stream().map(log -> {
            AuditLogResponse resp = new AuditLogResponse();
            BeanUtils.copyProperties(log, resp);
            return resp;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }
}