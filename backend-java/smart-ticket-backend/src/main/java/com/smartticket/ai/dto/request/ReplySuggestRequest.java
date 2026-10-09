package com.smartticket.ai.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ReplySuggestRequest {
    private Long ticketId;
    private String content;
    private List<Map<String, Object>> history = new ArrayList<>();
}