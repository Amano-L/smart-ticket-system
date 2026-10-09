-- 数据库
CREATE DATABASE IF NOT EXISTS smart_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_ticket;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                        username VARCHAR(64) NOT NULL COMMENT '登录名',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt加密密码',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
                                        id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                                        role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
                                             id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             user_id BIGINT NOT NULL COMMENT '用户ID',
                                             role_id BIGINT NOT NULL COMMENT '角色ID',
                                             PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 工单表
CREATE TABLE IF NOT EXISTS ticket (
                                      id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
                                      ticket_no VARCHAR(32) NOT NULL COMMENT '工单编号',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT '类型',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已解决 3已关闭',
    priority TINYINT NOT NULL DEFAULT 1 COMMENT '0低 1中 2高 3紧急',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    assignee_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_status_created (status, created_at),
    KEY idx_assignee_status (assignee_id, status),
    KEY idx_creator (creator_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 工单流转记录表
CREATE TABLE IF NOT EXISTS ticket_flow (
                                           id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           ticket_id BIGINT NOT NULL COMMENT '工单ID',
                                           from_status TINYINT NOT NULL COMMENT '原状态',
                                           to_status TINYINT NOT NULL COMMENT '新状态',
                                           operator_id BIGINT NOT NULL COMMENT '操作人ID',
                                           remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转记录表';

-- 客服负载表
CREATE TABLE IF NOT EXISTS agent_load (
                                          id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          user_id BIGINT NOT NULL COMMENT '客服用户ID',
                                          current_count INT NOT NULL DEFAULT 0 COMMENT '当前处理中工单数',
                                          max_count INT NOT NULL DEFAULT 10 COMMENT '最大并发数',
                                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服负载表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
                                         id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         user_id BIGINT DEFAULT NULL COMMENT '操作人ID',
                                         action VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_id BIGINT DEFAULT NULL COMMENT '目标ID',
    result VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAIL',
    detail VARCHAR(512) DEFAULT NULL COMMENT '详情',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_trace_id (trace_id),
    KEY idx_target (target_type, target_id),
    KEY idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- AI调用记录表
CREATE TABLE IF NOT EXISTS ai_call_log (
                                           id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           ticket_id BIGINT DEFAULT NULL COMMENT '关联工单ID',
                                           call_type VARCHAR(32) NOT NULL COMMENT 'CLASSIFY/REPLY_SUGGEST',
    model VARCHAR(64) DEFAULT NULL COMMENT '模型名称',
    cost_ms INT DEFAULT NULL COMMENT '耗时毫秒',
    success TINYINT NOT NULL DEFAULT 0 COMMENT '1成功 0失败',
    fallback TINYINT NOT NULL DEFAULT 0 COMMENT '1已降级 0未降级',
    request TEXT DEFAULT NULL COMMENT '请求摘要',
    response TEXT DEFAULT NULL COMMENT '响应摘要',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用记录表';

-- 初始化角色
INSERT IGNORE INTO sys_role (role_code, role_name) VALUES
('USER', '普通用户'),
('AGENT', '客服'),
('SUPERVISOR', '客服主管'),
('ADMIN', '管理员');