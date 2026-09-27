package com.smartticket.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_flow")
public class TicketFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}