package com.smartticket.ai.controller;

import com.smartticket.ai.dto.response.ReplySuggestResponse;
import com.smartticket.ai.service.AiService;
import com.smartticket.common.ErrorCode;
import com.smartticket.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 辅助", description = "AI 回复建议（AGENT）")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "获取 AI 回复建议（AGENT）",
            description = "调用 Python Agent 生成话术，失败返回 code=4001 且 fallback=true")
    @GetMapping("/suggest/{ticketId}")
    @PreAuthorize("hasRole('AGENT')")
    public Result<ReplySuggestResponse> suggest(
            @Parameter(description = "工单 ID") @PathVariable Long ticketId) {
        ReplySuggestResponse response = aiService.suggestForTicket(ticketId);
        if (Boolean.TRUE.equals(response.getFallback())) {
            return Result.fail(ErrorCode.AI_FALLBACK.getCode(),
                    ErrorCode.AI_FALLBACK.getMessage(),
                    response);
        }
        return Result.success(response);
    }
}