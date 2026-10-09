package com.smartticket.ai.service;

import com.smartticket.ai.dto.response.ReplySuggestResponse;

public interface AiService {

    ReplySuggestResponse suggestForTicket(Long ticketId);

    /**
     * 分类工单。失败时返回 UNKNOWN。
     */
    String classifyForTicket(Long ticketId, String title, String content);
}