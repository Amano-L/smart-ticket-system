package com.smartticket.ai.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ReplySuggestResponse {
    private String suggestion;
    private List<Map<String, Object>> references = new ArrayList<>();
    private Boolean fallback;
}