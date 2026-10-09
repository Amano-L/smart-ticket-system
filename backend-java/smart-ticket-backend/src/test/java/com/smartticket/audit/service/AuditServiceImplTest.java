package com.smartticket.audit.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.audit.dto.AuditLogResponse;
import com.smartticket.audit.dto.AuditQueryRequest;
import com.smartticket.audit.entity.AuditLog;
import com.smartticket.audit.mapper.AuditLogMapper;
import com.smartticket.audit.service.impl.AuditServiceImpl;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.common.PageResult;
import com.smartticket.security.SecurityUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    /**
     * 初始化 MyBatis-Plus 的 Lambda 缓存。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, AuditLog.class);
    }

    // ==================== record ====================

    @Test
    void record_shouldInsertLog_whenUserLoggedIn() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
            MDC.put("traceId", "trace-001");

            try {
                auditService.record("CREATE_TICKET", "TICKET", 100L, "SUCCESS", "创建工单");
            } finally {
                MDC.clear();
            }

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogMapper).insert(captor.capture());

            AuditLog log = captor.getValue();
            assertThat(log.getUserId()).isEqualTo(1L);
            assertThat(log.getAction()).isEqualTo("CREATE_TICKET");
            assertThat(log.getTargetType()).isEqualTo("TICKET");
            assertThat(log.getTargetId()).isEqualTo(100L);
            assertThat(log.getResult()).isEqualTo("SUCCESS");
            assertThat(log.getDetail()).isEqualTo("创建工单");
            assertThat(log.getTraceId()).isEqualTo("trace-001");
        }
    }

    @Test
    void record_shouldUseNull_whenNoLoginUser() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId)
                    .thenThrow(new BizException(ErrorCode.UNAUTHORIZED));

            auditService.record("SYSTEM_TASK", "TICKET", 1L, "SUCCESS", "定时任务");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogMapper).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isNull();
        }
    }

    @Test
    void record_shouldUseEmptyString_whenTraceIdMissing() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
            MDC.clear();

            auditService.record("TEST", "TICKET", 1L, "SUCCESS", "无 traceId");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogMapper).insert(captor.capture());
            assertThat(captor.getValue().getTraceId()).isEmpty();
        }
    }

    // ==================== query ====================

    @Test
    void query_shouldReturnEmptyPage_whenNoData() {
        Page<AuditLog> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(auditLogMapper.selectPage(any(), any())).thenReturn(emptyPage);

        AuditQueryRequest req = new AuditQueryRequest();
        req.setPage(1);
        req.setSize(10);

        PageResult<AuditLogResponse> result = auditService.query(req);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    void query_shouldReturnListWithConvertedFields() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setUserId(2L);
        log.setAction("CREATE_TICKET");
        log.setTargetType("TICKET");
        log.setTargetId(100L);
        log.setResult("SUCCESS");
        log.setDetail("创建");
        log.setTraceId("trace-001");
        log.setCreatedAt(LocalDateTime.now());

        Page<AuditLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log));
        page.setTotal(1);
        when(auditLogMapper.selectPage(any(), any())).thenReturn(page);

        AuditQueryRequest req = new AuditQueryRequest();
        req.setPage(1);
        req.setSize(10);

        PageResult<AuditLogResponse> result = auditService.query(req);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getAction()).isEqualTo("CREATE_TICKET");
        assertThat(result.getList().get(0).getTraceId()).isEqualTo("trace-001");
    }
}