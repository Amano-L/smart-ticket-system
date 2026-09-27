package com.smartticket.ticket.dto.request;

import lombok.Data;

// 查询工单请求
@Data
public class TicketQueryRequest {
    private long page = 1;
    private long size = 10;
    private Integer status;
    private String type;
    private Long assigneeId;

}
