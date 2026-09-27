package com.smartticket.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 创建工单请求
@Data
public class TicketCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过128字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Integer priority = 1;
}
