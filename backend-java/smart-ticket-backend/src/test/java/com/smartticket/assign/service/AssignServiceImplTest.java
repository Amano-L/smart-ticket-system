package com.smartticket.assign.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.smartticket.assign.entity.AgentLoad;
import com.smartticket.assign.mapper.AgentLoadMapper;
import com.smartticket.assign.service.impl.AssignServiceImpl;
import com.smartticket.audit.service.AuditService;
import com.smartticket.common.BizException;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.user.entity.SysRole;
import com.smartticket.user.entity.SysUserRole;
import com.smartticket.user.mapper.SysRoleMapper;
import com.smartticket.user.mapper.SysUserRoleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignServiceImplTest {

    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserRoleMapper sysUserRoleMapper;
    @Mock private AgentLoadMapper agentLoadMapper;
    @Mock private TicketMapper ticketMapper;
    @Mock private AuditService auditService;

    @InjectMocks
    private AssignServiceImpl assignService;

    /**
     * 初始化 MyBatis-Plus 的 Lambda 缓存。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, AgentLoad.class);
        TableInfoHelper.initTableInfo(assistant, Ticket.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
    }

    @Test
    void autoAssign_shouldThrow_whenNoAgentRole() {
        when(sysRoleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> assignService.autoAssign(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(3001));
    }

    @Test
    void autoAssign_shouldThrow_whenNoAgentUser() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> assignService.autoAssign(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(3001));
    }

    @Test
    void autoAssign_shouldThrow_whenAllAgentsFull() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);

        SysUserRole ur = new SysUserRole();
        ur.setUserId(10L);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(ur));
        when(agentLoadMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> assignService.autoAssign(1L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(3001));
    }

    @Test
    void autoAssign_shouldPickLowestLoad() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);

        SysUserRole ur1 = new SysUserRole(); ur1.setUserId(10L);
        SysUserRole ur2 = new SysUserRole(); ur2.setUserId(20L);
        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(ur1, ur2));

        AgentLoad load1 = new AgentLoad();
        load1.setUserId(10L); load1.setCurrentCount(3); load1.setMaxCount(10);

        AgentLoad load2 = new AgentLoad();
        load2.setUserId(20L); load2.setCurrentCount(1); load2.setMaxCount(10);

        when(agentLoadMapper.selectList(any())).thenReturn(List.of(load1, load2));

        Long assigned = assignService.autoAssign(100L);

        assertThat(assigned).isEqualTo(20L);

        verify(agentLoadMapper).update(isNull(), any());
        verify(ticketMapper).updateById(any(Ticket.class));
        verify(auditService).record(eq("AUTO_ASSIGN"), eq("TICKET"), eq(100L), eq("SUCCESS"), any());
    }

    @Test
    void manualAssign_shouldThrow_whenTicketNotFound() {
        when(ticketMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> assignService.manualAssign(999L, 10L, "改派"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(2001));
    }

    @Test
    void manualAssign_shouldThrow_whenTargetNotAgent() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setAssigneeId(2L);
        ticket.setTicketNo("T202609270001");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> assignService.manualAssign(1L, 999L, "改派"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(400));
    }

    @Test
    void manualAssign_shouldUpdateLoads_whenSuccess() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setAssigneeId(10L);
        ticket.setTicketNo("T202609270001");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);

        assignService.manualAssign(1L, 20L, "改派给李客服");

        verify(ticketMapper).updateById(any(Ticket.class));
        verify(agentLoadMapper, times(2)).update(isNull(), any());
        verify(auditService).record(eq("MANUAL_ASSIGN"), eq("TICKET"), eq(1L), eq("SUCCESS"), any());
    }

    @Test
    void manualAssign_shouldNotDecrement_whenSameAssignee() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setAssigneeId(20L);
        ticket.setTicketNo("T202609270001");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        SysRole role = new SysRole();
        role.setId(2L);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);

        assignService.manualAssign(1L, 20L, "重复派单");

        verify(agentLoadMapper, times(1)).update(isNull(), any());
    }
}