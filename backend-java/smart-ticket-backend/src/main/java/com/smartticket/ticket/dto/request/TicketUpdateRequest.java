package com.smartticket.ticket.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

// 更新工单请求
@Data
public class TicketUpdateRequest {
    @Size(max = 128, message = "标题不能超过128字符")
    private String title;
    private Integer priority;
}
