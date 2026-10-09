package com.smartticket.ticket.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.ai.service.AiService;
import com.smartticket.assign.service.AssignService;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.BizException;
import com.smartticket.common.PageResult;
import com.smartticket.security.LoginUser;
import com.smartticket.security.SecurityUtil;
import com.smartticket.ticket.dto.request.TicketCreateRequest;
import com.smartticket.ticket.dto.request.TicketQueryRequest;
import com.smartticket.ticket.dto.request.TicketTransitionRequest;
import com.smartticket.ticket.dto.response.TicketDetailResponse;
import com.smartticket.ticket.dto.response.TicketListResponse;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketFlow;
import com.smartticket.ticket.mapper.TicketFlowMapper;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.ticket.mapper.TicketReplyMapper;
import com.smartticket.ticket.service.impl.TicketServiceImpl;
import com.smartticket.util.TicketNoGenerator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketFlowMapper ticketFlowMapper;
    @Mock private TicketReplyMapper ticketReplyMapper;
    @Mock private TicketNoGenerator ticketNoGenerator;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private AssignService assignService;
    @Mock private AiService aiService;
    @Mock private AuditService auditService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private LoginUser userLogin;
    private LoginUser agentLogin;
    private LoginUser adminLogin;

    /**
     * 初始化 MyBatis-Plus 的 Lambda 缓存。
     * 单元测试不启动 Spring，LambdaQueryWrapper 会因缺少缓存而报错。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Ticket.class);
        TableInfoHelper.initTableInfo(assistant, TicketFlow.class);
    }

    @BeforeEach
    void setUp() {
        userLogin = new LoginUser(3L, "user", List.of("USER"));
        agentLogin = new LoginUser(2L, "agent", List.of("AGENT"));
        adminLogin = new LoginUser(1L, "admin", List.of("ADMIN"));
    }

    // ==================== create ====================

    @Test
    void create_shouldThrow_whenRequestIdMissing() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("测试");
        req.setContent("内容");

        assertThatThrownBy(() -> ticketService.create(req, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("X-Request-Id");
    }

    @Test
    void create_shouldThrow_whenRequestIdBlank() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitle("测试");
        req.setContent("内容");

        assertThatThrownBy(() -> ticketService.create(req, "  "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("X-Request-Id");
    }

    @Test
    void create_shouldReturnCachedTicket_whenIdempotentHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("ticket:idempotent:req-001")).thenReturn("100");

        Ticket cached = new Ticket();
        cached.setId(100L);
        cached.setTicketNo("T202609270001");
        cached.setCreatorId(3L);
        cached.setStatus(0);
        when(ticketMapper.selectById(100L)).thenReturn(cached);
        when(ticketFlowMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(userLogin);

            TicketCreateRequest req = new TicketCreateRequest();
            req.setTitle("测试");
            req.setContent("内容");
            TicketDetailResponse result = ticketService.create(req, "req-001");

            assertThat(result.getId()).isEqualTo(100L);
            verify(ticketMapper, never()).insert(any(Ticket.class));
            verify(assignService, never()).autoAssign(any());
        }
    }

    @Test
    void create_shouldCreateTicket_whenIdempotentMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("ticket:idempotent:req-002")).thenReturn(null);
        when(ticketNoGenerator.generate()).thenReturn("T202609270002");
        when(aiService.classifyForTicket(any(), any(), any())).thenReturn("REFUND");

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(userLogin);

            TicketCreateRequest req = new TicketCreateRequest();
            req.setTitle("退款申请");
            req.setContent("订单未收到");
            req.setPriority(1);

            doAnswer(inv -> {
                Ticket t = inv.getArgument(0);
                t.setId(200L);
                return 1;
            }).when(ticketMapper).insert(any(Ticket.class));

            Ticket detailTicket = new Ticket();
            detailTicket.setId(200L);
            detailTicket.setTicketNo("T202609270002");
            detailTicket.setCreatorId(3L);
            detailTicket.setType("REFUND");
            detailTicket.setStatus(0);
            when(ticketMapper.selectById(200L)).thenReturn(detailTicket);
            when(ticketFlowMapper.selectList(any())).thenReturn(List.of());

            TicketDetailResponse result = ticketService.create(req, "req-002");

            assertThat(result.getId()).isEqualTo(200L);
            assertThat(result.getType()).isEqualTo("REFUND");

            verify(ticketMapper).insert(any(Ticket.class));
            verify(aiService).classifyForTicket(eq(200L), eq("退款申请"), eq("订单未收到"));
            verify(assignService).autoAssign(200L);
            verify(auditService).record(eq("CREATE_TICKET"), eq("TICKET"), eq(200L), eq("SUCCESS"), anyString());
        }
    }

    // ==================== list ====================

    @Test
    void list_shouldFilterByCreator_whenUser() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(userLogin);

            Page<Ticket> emptyPage = new Page<>(1, 10);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            when(ticketMapper.selectPage(any(), any())).thenReturn(emptyPage);

            TicketQueryRequest query = new TicketQueryRequest();
            query.setPage(1);
            query.setSize(10);

            PageResult<TicketListResponse> result = ticketService.list(query);

            assertThat(result.getTotal()).isZero();
            assertThat(result.getList()).isEmpty();
        }
    }

    // ==================== detail ====================

    @Test
    void detail_shouldThrow_whenTicketNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> ticketService.detail(999L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(2001));
    }

    @Test
    void detail_shouldThrow403_whenUserNotOwner() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setCreatorId(999L);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(userLogin);

            assertThatThrownBy(() -> ticketService.detail(1L))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(403));
        }
    }

    @Test
    void detail_shouldReturnTicket_whenAdmin() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNo("T202609270001");
        ticket.setCreatorId(999L);
        ticket.setStatus(0);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketFlowMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(adminLogin);

            TicketDetailResponse result = ticketService.detail(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    // ==================== transition ====================

    @Test
    void transition_shouldThrow_whenIllegalTransition() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(0);
        ticket.setAssigneeId(2L);
        ticket.setCreatorId(3L);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(agentLogin);
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(2L);

            TicketTransitionRequest req = new TicketTransitionRequest();
            req.setToStatus(3);

            assertThatThrownBy(() -> ticketService.transition(1L, req))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(2002));
        }
    }

    @Test
    void transition_shouldThrow_whenIllegalStatusCode() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(0);
        ticket.setAssigneeId(2L);
        ticket.setCreatorId(3L);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(agentLogin);

            TicketTransitionRequest req = new TicketTransitionRequest();
            req.setToStatus(99);

            assertThatThrownBy(() -> ticketService.transition(1L, req))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(400));
        }
    }

    @Test
    void transition_shouldUpdateStatusAndWriteFlow_whenLegal() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(0);
        ticket.setAssigneeId(2L);
        ticket.setCreatorId(3L);
        ticket.setTicketNo("T202609270001");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketFlowMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUser).thenReturn(agentLogin);
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(2L);

            TicketTransitionRequest req = new TicketTransitionRequest();
            req.setToStatus(1);
            req.setRemark("开始处理");

            TicketDetailResponse result = ticketService.transition(1L, req);

            assertThat(result.getStatus()).isEqualTo(1);
            verify(ticketMapper).updateById(any(Ticket.class));
            verify(ticketFlowMapper).insert(any(TicketFlow.class));
            verify(auditService).record(eq("TRANSITION"), eq("TICKET"), eq(1L), eq("SUCCESS"), anyString());
        }
    }
}