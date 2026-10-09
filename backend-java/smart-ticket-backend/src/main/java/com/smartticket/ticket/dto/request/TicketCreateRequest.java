package com.smartticket.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "创建工单请求")
@Data
public class TicketCreateRequest {

    @Schema(description = "标题", example = "退款申请")
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过128字符")
    private String title;

    @Schema(description = "内容", example = "订单 12345 未收到货，申请退款")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "优先级 0低/1中/2高/3紧急", example = "1")
    private Integer priority = 1;
}