package com.smartticket.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketTransitionRequest {

    @NotNull(message = "目标状态不能为空")
    private Integer ToStatus;

    @Size(max = 255, message = "备注不能超过255字符")
    private String remark;


}
