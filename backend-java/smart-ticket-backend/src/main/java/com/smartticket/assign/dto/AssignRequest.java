package com.smartticket.assign.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignRequest {
    @NotNull(message = "目标客服不能为空")
    private Long assigneeId;

    @Size(max = 255, message = "备注不能超过255字符")
    private String remark;
}