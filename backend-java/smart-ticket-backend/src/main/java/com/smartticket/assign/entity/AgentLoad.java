package com.smartticket.assign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_load")
public class AgentLoad {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer currentCount;
    private Integer maxCount;
    private LocalDateTime updatedAt;
}