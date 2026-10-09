package com.smartticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.stat.dto.AgentWorkload;
import com.smartticket.stat.dto.TypeCount;
import com.smartticket.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketMapper extends BaseMapper<Ticket> {

    // 平均处理时长（只统计已解决 status=2）
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, updated_at)) FROM ticket " +
            "WHERE status = 2 AND created_at BETWEEN #{from} AND #{to}")
    Double avgHandleHours(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 类型分布
    @Select("SELECT type AS type, COUNT(*) AS cnt FROM ticket " +
            "WHERE created_at BETWEEN #{from} AND #{to} " +
            "GROUP BY type")
    List<TypeCount> countByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 客服工作量（left join 拿姓名，过滤未派单）
    @Select("SELECT t.assignee_id AS userId, u.real_name AS name, COUNT(*) AS `count` " +
            "FROM ticket t LEFT JOIN sys_user u ON t.assignee_id = u.id " +
            "WHERE t.assignee_id IS NOT NULL " +
            "AND t.created_at BETWEEN #{from} AND #{to} " +
            "GROUP BY t.assignee_id, u.real_name")
    List<AgentWorkload> agentWorkload(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}