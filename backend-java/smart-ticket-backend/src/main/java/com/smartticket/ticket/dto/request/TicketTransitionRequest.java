package com.smartticket.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "状态流转请求")
@Data
public class TicketTransitionRequest {

    @Schema(description = "目标状态 0待处理/1处理中/2已解决/3已关闭", example = "1")
    @NotNull(message = "目标状态不能为空")
    private Integer toStatus;

    @Schema(description = "备注", example = "开始处理")
    @Size(max = 255, message = "备注不能超过255字符")
    private String remark;
}