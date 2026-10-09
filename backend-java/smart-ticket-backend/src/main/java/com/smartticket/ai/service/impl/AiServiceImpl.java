package com.smartticket.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.ai.client.PythonAgentClient;
import com.smartticket.ai.dto.request.ClassifyRequest;
import com.smartticket.ai.dto.request.ReplySuggestRequest;
import com.smartticket.ai.dto.response.ClassifyResponse;
import com.smartticket.ai.dto.response.ReplySuggestResponse;
import com.smartticket.ai.entity.AiCallLog;
import com.smartticket.ai.mapper.AiCallLogMapper;
import com.smartticket.ai.service.AiService;
import com.smartticket.common.BizException;
import com.smartticket.common.ErrorCode;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String CALL_TYPE_REPLY = "REPLY_SUGGEST";
    private static final String MODEL_NAME = "python-agent";

    private final TicketMapper ticketMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final PythonAgentClient pythonAgentClient;
    private final ObjectMapper objectMapper;
    private static final String CALL_TYPE_CLASSIFY = "CLASSIFY";

    @Override
    public String classifyForTicket(Long ticketId, String title, String content) {
        ClassifyRequest request = new ClassifyRequest();
        request.setTicketId(ticketId);
        request.setTitle(title);
        request.setContent(content);

        long start = System.currentTimeMillis();
        try {
            ClassifyResponse response = pythonAgentClient.classify(request);
            long cost = System.currentTimeMillis() - start;
            saveClassifyLog(ticketId, cost, 1, 0, request, response);
            return response.getType() == null ? "UNKNOWN" : response.getType();
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("AI 分类调用失败，降级为 UNKNOWN。ticketId={}, cost={}ms, err={}",
                    ticketId, cost, e.getMessage());
            saveClassifyLog(ticketId, cost, 0, 1, request, null);
            return "UNKNOWN";
        }
    }

    private void saveClassifyLog(Long ticketId, long costMs, int success, int fallback,
                                 Object request, Object response) {
        try {
            AiCallLog log = new AiCallLog();
            log.setTicketId(ticketId);
            log.setCallType(CALL_TYPE_CLASSIFY);
            log.setModel(MODEL_NAME);
            log.setCostMs((int) costMs);
            log.setSuccess(success);
            log.setFallback(fallback);
            log.setRequest(objectMapper.writeValueAsString(request));
            log.setResponse(response == null ? null : objectMapper.writeValueAsString(response));
            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写 ai_call_log 失败", e);
        }
    }

    @Override
    public ReplySuggestResponse suggestForTicket(Long ticketId) {
        // 1. 查工单
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BizException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 2. 构造请求
        ReplySuggestRequest request = new ReplySuggestRequest();
        request.setTicketId(ticketId);
        request.setContent(ticket.getContent());

        // 3. 调 Python + 降级
        long start = System.currentTimeMillis();
        try {
            ReplySuggestResponse response = pythonAgentClient.replySuggest(request);
            long cost = System.currentTimeMillis() - start;
            saveCallLog(ticketId, cost, 1, 0, request, response);
            return response;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("AI 回复建议调用失败，已降级。ticketId={}, cost={}ms, err={}",
                    ticketId, cost, e.getMessage());
            saveCallLog(ticketId, cost, 0, 1, request, null);

            ReplySuggestResponse fallback = new ReplySuggestResponse();
            fallback.setFallback(true);
            return fallback;
        }
    }

    private void saveCallLog(Long ticketId, long costMs, int success, int fallback,
                             Object request, Object response) {
        try {
            AiCallLog log = new AiCallLog();
            log.setTicketId(ticketId);
            log.setCallType(CALL_TYPE_REPLY);
            log.setModel(MODEL_NAME);
            log.setCostMs((int) costMs);
            log.setSuccess(success);
            log.setFallback(fallback);
            log.setRequest(objectMapper.writeValueAsString(request));
            log.setResponse(response == null ? null : objectMapper.writeValueAsString(response));
            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            // 写日志失败不影响主流程
            log.error("写 ai_call_log 失败", e);
        }
    }
}