package com.smartticket.stat.controller;

import com.smartticket.common.Result;
import com.smartticket.stat.dto.StatOverviewResponse;
import com.smartticket.stat.service.StatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "数据统计", description = "工单统计概览（SUPERVISOR / ADMIN）")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @Operation(summary = "统计概览",
            description = "总数、已解决数、平均处理时长、类型分布、客服工作量；默认最近 30 天")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public Result<StatOverviewResponse> overview(
            @Parameter(description = "开始时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @Parameter(description = "结束时间 yyyy-MM-dd HH:mm:ss")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return Result.success(statService.overview(from, to));
    }
}