package com.smartticket.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TicketNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final StringRedisTemplate redisTemplate;

    public String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        String key = "ticket:no:" + date;

        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        if (seq == null) {
            throw new IllegalStateException("生成工单编号失败");
        }
        return "T" + date + String.format("%04d", seq);
    }
}