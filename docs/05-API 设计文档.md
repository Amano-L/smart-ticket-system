# 智能工单管理系统 - API 设计文档

## 1. 设计规范
- 风格：RESTful
- 前缀：/api
- 认证：JWT，请求头 Authorization: Bearer {token}
- 数据格式：JSON，UTF-8
- 分页参数：page（从 1 开始）、size（默认 10）
- 时间格式：yyyy-MM-dd HH:mm:ss
- 文档工具：Springdoc OpenAPI，访问 /swagger-ui.html

## 2. 统一响应格式
成功：
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "a1b2c3d4e5f6"
}

分页：
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10
  },
  "traceId": "a1b2c3d4e5f6"
}

失败：
{
  "code": 400,
  "message": "参数错误：标题不能为空",
  "data": null,
  "traceId": "a1b2c3d4e5f6"
}

## 3. 错误码
| code | 说明                                 |
| :--- | :----------------------------------- |
| 0    | 成功                                 |
| 400  | 参数错误                             |
| 401  | 未登录或 Token 失效                  |
| 403  | 无权限                               |
| 404  | 资源不存在                           |
| 409  | 状态冲突（如非法状态流转、重复提交） |
| 429  | 请求过于频繁                         |
| 500  | 系统异常                             |
| 1001 | 用户名或密码错误                     |
| 1002 | 账号已禁用                           |
| 2001 | 工单不存在                           |
| 2002 | 工单状态不允许此操作                 |
| 2003 | 重复提交工单                         |
| 3001 | 无可用客服                           |
| 4001 | AI 服务调用失败（已降级）            |

## 4. 接口清单
| 模块 | 方法 | 路径                         | 权限               |
| :--- | :--- | :--------------------------- | :----------------- |
| 认证 | POST | /api/auth/login              | 公开               |
| 认证 | POST | /api/auth/logout             | 登录用户           |
| 认证 | GET  | /api/auth/me                 | 登录用户           |
| 工单 | POST | /api/tickets                 | USER               |
| 工单 | GET  | /api/tickets                 | 所有角色           |
| 工单 | GET  | /api/tickets/{id}            | 所有角色           |
| 工单 | PUT  | /api/tickets/{id}            | AGENT / SUPERVISOR |
| 工单 | POST | /api/tickets/{id}/transition | AGENT / SUPERVISOR |
| 工单 | POST | /api/tickets/{id}/assign     | SUPERVISOR         |
| 工单 | POST | /api/tickets/{id}/reply      | AGENT              |
| 统计 | GET  | /api/stats/overview          | SUPERVISOR / ADMIN |
| 审计 | GET  | /api/audits                  | ADMIN              |
| AI   | GET  | /api/ai/suggest/{ticketId}   | AGENT              |

合计 13 个 Java 对外接口。Python Agent 服务仅暴露 /health、/agent/classify、/agent/reply-suggest 三个内网接口，由 Java 通过 RestClient 调用。

## 5. 认证模块

### 5.1 登录
POST /api/auth/login

请求：
{
  "username": "admin",
  "password": "admin123"
}

响应：
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "roles": ["ADMIN"]
  },
  "traceId": "a1b2c3d4"
}

### 5.2 登出
POST /api/auth/logout
请求头：Authorization: Bearer {token}
响应：code=0

### 5.3 当前用户
GET /api/auth/me

响应：
{
  "code": 0,
  "data": {
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "roles": ["ADMIN"]
  }
}

## 6. 工单模块

### 6.1 创建工单
POST /api/tickets
权限：USER
幂等：请求头 X-Request-Id，相同 ID 重复提交返回原工单

请求：
{
  "title": "退款申请",
  "content": "订单 12345 未收到货，申请退款",
  "priority": 1
}

响应：
{
  "code": 0,
  "data": {
    "id": 1001,
    "ticketNo": "T20250301001",
    "status": 0,
    "type": "REFUND",
    "assigneeId": 5,
    "aiFallback": false
  }
}

说明：创建后自动触发 AI 分类 + 自动派单，AI 失败走规则分类，不影响创建。

### 6.2 工单列表
GET /api/tickets?page=1&size=10&status=0&type=REFUND&assigneeId=5

权限：
- USER：仅看自己创建的
- AGENT：仅看分配给自己的
- SUPERVISOR / ADMIN：看全部

响应：
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1001,
        "ticketNo": "T20250301001",
        "title": "退款申请",
        "type": "REFUND",
        "status": 0,
        "priority": 1,
        "assigneeId": 5,
        "createdAt": "2025-03-01 10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
}

### 6.3 工单详情
GET /api/tickets/{id}

响应：
{
  "code": 0,
  "data": {
    "id": 1001,
    "ticketNo": "T20250301001",
    "title": "退款申请",
    "content": "订单 12345 未收到货，申请退款",
    "type": "REFUND",
    "status": 1,
    "priority": 1,
    "creatorId": 2,
    "assigneeId": 5,
    "createdAt": "2025-03-01 10:00:00",
    "updatedAt": "2025-03-01 10:30:00",
    "flows": [
      {
        "fromStatus": 0,
        "toStatus": 1,
        "operatorId": 5,
        "remark": "开始处理",
        "createdAt": "2025-03-01 10:30:00"
      }
    ]
  }
}

### 6.4 编辑工单
PUT /api/tickets/{id}
权限：AGENT / SUPERVISOR

请求：
{
  "title": "退款申请（已核实）",
  "priority": 2
}

### 6.5 状态流转
POST /api/tickets/{id}/transition
权限：AGENT / SUPERVISOR

请求：
{
  "toStatus": 2,
  "remark": "已退款"
}

状态机规则：
| 当前状态 | 允许流转到 |
| :------- | :--------- |
| 0 待处理 | 1 处理中   |
| 1 处理中 | 2 已解决   |
| 2 已解决 | 3 已关闭   |
| 3 已关闭 | 无         |

非法流转返回 409 / 2002。

### 6.6 手动派单
POST /api/tickets/{id}/assign
权限：SUPERVISOR

请求：
{
  "assigneeId": 5,
  "remark": "改派给张客服"
}

### 6.7 客服回复
POST /api/tickets/{id}/reply
权限：AGENT

请求：
{
  "content": "您好，退款已处理，预计 3 个工作日到账。",
  "useAiSuggestion": true
}

## 7. 统计模块

### 7.1 统计概览
GET /api/stats/overview?from=2025-03-01&to=2025-03-31
权限：SUPERVISOR / ADMIN

响应：
{
  "code": 0,
  "data": {
    "totalTickets": 200,
    "resolvedTickets": 150,
    "avgHandleHours": 4.5,
    "typeDistribution": {
      "REFUND": 80,
      "TECH": 60,
      "COMPLAINT": 30,
      "CONSULT": 30
    },
    "agentWorkload": [
      { "userId": 5, "name": "张客服", "count": 40 },
      { "userId": 6, "name": "李客服", "count": 35 }
    ]
  }
}

## 8. 审计模块

### 8.1 审计日志查询
GET /api/audits?page=1&size=10&userId=1&traceId=abc&from=...&to=...
权限：ADMIN

响应：
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 1,
        "userId": 1,
        "action": "CREATE_TICKET",
        "targetType": "TICKET",
        "targetId": 1001,
        "result": "SUCCESS",
        "detail": "创建工单 T20250301001",
        "traceId": "a1b2c3d4",
        "createdAt": "2025-03-01 10:00:00"
      }
    ],
    "total": 1
  }
}

## 9. AI 模块

### 9.1 获取回复建议（Java 对外）
GET /api/ai/suggest/{ticketId}
权限：AGENT
说明：Java 内部通过 RestClient 调用 Python `/agent/reply-suggest`，超时 2 秒，失败返回空建议并标记降级。

响应成功：
{
  "code": 0,
  "data": {
    "suggestion": "您好，关于您反馈的退款问题，我们已核实...",
    "references": [
      { "ticketNo": "T20250215008", "title": "类似退款工单" }
    ],
    "fallback": false
  }
}

响应降级：
{
  "code": 4001,
  "message": "AI 服务不可用，已降级",
  "data": {
    "suggestion": null,
    "fallback": true
  }
}

### 9.2 Python Agent 服务接口（仅 Java 内网调用）
说明：Python 服务不对外暴露，仅 Java 后端通过内网调用。Java 不对外提供 /api/internal/... 接口。

Python 接口：
| 方法 | 路径                 | 说明     |
| :--- | :------------------- | :------- |
| GET  | /health              | 健康检查 |
| POST | /agent/classify      | 工单分类 |
| POST | /agent/reply-suggest | 回复建议 |

/agent/classify 请求：
{
  "ticketId": 1001,
  "title": "退款申请",
  "content": "订单 12345 未收到货"
}

/agent/classify 响应：
{
  "type": "REFUND",
  "confidence": 0.92,
  "fallback": false
}

/agent/reply-suggest 请求：
{
  "ticketId": 1001,
  "content": "订单 12345 未收到货",
  "history": []
}

/agent/reply-suggest 响应：
{
  "suggestion": "您好，关于您的退款申请...",
  "references": [],
  "fallback": false
}

## 10. 权限矩阵
| 接口         | USER | AGENT | SUPERVISOR | ADMIN |
| :----------- | :--: | :---: | :--------: | :---: |
| 登录         |  ✓   |   ✓   |     ✓      |   ✓   |
| 创建工单     |  ✓   |       |            |       |
| 查看自己工单 |  ✓   |       |            |       |
| 查看分配工单 |      |   ✓   |     ✓      |   ✓   |
| 查看全部工单 |      |       |     ✓      |   ✓   |
| 编辑工单     |      |   ✓   |     ✓      |       |
| 状态流转     |      |   ✓   |     ✓      |       |
| 手动派单     |      |       |     ✓      |       |
| 客服回复     |      |   ✓   |            |       |
| 统计         |      |       |     ✓      |   ✓   |
| 审计日志     |      |       |            |   ✓   |
| AI 建议      |      |   ✓   |            |       |

## 11. 通用请求头
| 请求头        | 必填         | 说明             |
| :------------ | :----------- | :--------------- |
| Authorization | 是           | Bearer {token}   |
| X-Request-Id  | 创建工单必填 | 幂等 ID，UUID    |
| Content-Type  | 是           | application/json |

## 12. 变更记录
| 日期           | 版本 | 变更内容                                                     | 变更人 |
| :------------- | :--- | :----------------------------------------------------------- | :----- |
| 【2026.09.26】 | v1.0 | 初始版本                                                     | 本人   |
| 【2026.09.26】 | v1.1 | 删除 /api/internal 接口，改为 Python 仅暴露三个接口，Java 通过 RestClient 调用；统一错误码 | 本人   |