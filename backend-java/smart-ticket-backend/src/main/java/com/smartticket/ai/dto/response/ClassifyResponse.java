package com.smartticket.ai.dto.response;

import lombok.Data;

@Data
public class ClassifyResponse {
    private String type;        // REFUND/TECH/COMPLAINT/CONSULT/UNKNOWN
    private Double confidence;  // 0.0 ~ 1.0
    private Boolean fallback;   // 是否是降级结果
}