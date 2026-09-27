package com.smartticket.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String callType;
    private String model;
    private Integer costMs;
    private Integer success;
    private Integer fallback;
    private String request;
    private String response;
    private LocalDateTime createdAt;
}