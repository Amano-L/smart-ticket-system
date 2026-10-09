package com.smartticket.ai.controller;

import com.smartticket.ai.dto.response.ReplySuggestResponse;
import com.smartticket.ai.service.AiService;
import com.smartticket.common.ErrorCode;
import com.smartticket.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/suggest/{ticketId}")
    @PreAuthorize("hasRole('AGENT')")
    public Result<ReplySuggestResponse> suggest(@PathVariable Long ticketId) {
        ReplySuggestResponse response = aiService.suggestForTicket(ticketId);
        if (Boolean.TRUE.equals(response.getFallback())) {
            return Result.fail(ErrorCode.AI_FALLBACK.getCode(),
                    ErrorCode.AI_FALLBACK.getMessage(),
                    response);
        }
        return Result.success(response);
    }
}