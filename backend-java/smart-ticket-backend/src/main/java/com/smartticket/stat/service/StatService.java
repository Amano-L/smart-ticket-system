package com.smartticket.stat.service;

import com.smartticket.stat.dto.StatOverviewResponse;

import java.time.LocalDateTime;

public interface StatService {

    StatOverviewResponse overview(LocalDateTime from, LocalDateTime to);
}