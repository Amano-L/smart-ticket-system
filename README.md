# 智能工单管理系统

一个基于 **Java + Python Agent** 的智能工单管理平台。Java 后端保证核心业务稳定可控，Python Agent 提供 AI 辅助能力（工单分类、回复建议），AI 故障自动降级，不影响主流程。

## ✨ 功能特性

- **认证授权**：JWT + RBAC，支持 USER / AGENT / SUPERVISOR / ADMIN 四种角色
- **工单管理**：创建、列表分页、详情、编辑、优先级、接口幂等
- **状态机**：待处理 → 处理中 → 已解决 → 已关闭，非法流转 100% 拦截
- **自动派单**：按客服负载自动分配，负载原子更新，避免并发丢失更新
- **审计日志**：关键操作记录 traceId，全链路可追溯
- **数据统计**：工单数量、类型分布、平均处理时长、客服工作量
- **AI 辅助**：工单自动分类 + 回复建议，超时 2 秒自动降级
- **一键部署**：Docker Compose 编排 Java + Python + MySQL + Redis

## 🛠 技术栈

**后端**
- Java 21、Spring Boot 3.5.0
- Spring Security + JWT
- MyBatis-Plus 3.5.17、MySQL 8、Redis
- Springdoc OpenAPI 2.8.6
- JUnit 5 + Mockito

**AI Agent**
- Python 3.11、FastAPI
- 规则实现（可扩展为 LangChain / LangGraph）

**部署**
- Docker Compose

## 🏗 系统架构

```
客户端 (浏览器 / Apifox)
        ↓ HTTP
Java Spring Boot (8080)
  ├── MySQL   (持久化)
  ├── Redis   (缓存 + 幂等 + 工单号)
  └── Python FastAPI (8000, AI 辅助)
        ↓ HTTP (超时 2 秒, 失败降级)
     大模型 API (预留)
```

Java 侧负责认证、工单、派单、审计、统计；Python 侧负责分类和回复建议。两者通过 HTTP 解耦，AI 服务可替换、可降级。

## 🚀 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
git clone https://github.com/Amano-L/smart-ticket-system.git
cd smart-ticket-system/deploy
docker compose up -d
```

启动后访问：

- Swagger UI: http://localhost:8080/swagger-ui.html

### 方式二：本地开发

**1. 准备环境**

- JDK 21、Maven
- MySQL 8、Redis
- Python 3.11

**2. 初始化数据库**

```bash
mysql -u root -p < deploy/init.sql
```

**3. 配置本地参数**

```bash
cp backend-java/smart-ticket-backend/src/main/resources/application-local.yml.example \
   backend-java/smart-ticket-backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入你的 MySQL 密码和 JWT secret
```

**4. 启动 Java 后端**

```bash
cd backend-java/smart-ticket-backend
mvn spring-boot:run
```

**5. 启动 Python Agent**

```bash
cd agent-python
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## 👤 测试账号

| 账号 | 密码 | 角色 |
|:---|:---|:---|
| admin | admin123 | 管理员 |
| supervisor | super123 | 客服主管 |
| agent | agent123 | 客服 |
| agent2 | agent123 | 客服 |
| user | user123 | 普通用户 |

账号在应用启动时自动创建（`UserDataInitializer`）。

## 📖 接口文档

启动后访问：**http://localhost:8080/swagger-ui.html**

共 15 个对外接口，覆盖认证、工单、派单、审计、统计、AI 六类。

## 🧪 运行测试

```bash
cd backend-java/smart-ticket-backend
mvn test
```

单元测试使用 `@ExtendWith(MockitoExtension.class)`，**不依赖 MySQL 和 Redis**，24 个用例 5 秒内跑完。

## 📁 项目结构

```
smart-ticket-system/
├── docs/                              # 6 份项目文档
│   ├── 00-项目章程.md
│   ├── 01-MVP范围.md
│   ├── 02-需求规格说明书.md
│   ├── 03-架构设计.md
│   ├── 04-数据库设计.md
│   └── 05-API设计文档.md
├── backend-java/                      # Java 后端
│   └── smart-ticket-backend/
├── agent-python/                      # Python Agent 服务
├── deploy/                            # Docker Compose
│   ├── docker-compose.yml
│   └── init.sql
└── README.md
```

## 📐 核心设计

**为什么 Java + Python 双服务？**
Java 保证核心业务的稳定性、事务、权限和可审计性；Python 的 AI 生态（LangChain 等）更成熟。两者通过 HTTP 解耦，AI 服务挂了也不影响主流程。

**AI 降级策略**
- 分类失败 → 降级为 UNKNOWN
- 回复建议失败 → 返回 `fallback=true`，客服手动回复
- 超时 2 秒（RestClient connectTimeout 1s / readTimeout 2s）

**幂等设计**
创建工单时校验 `X-Request-Id`，相同 ID 在 5 分钟内重复提交返回同一工单，避免用户重复点击产生多条记录。

**并发安全的负载更新**
自动派单更新客服负载使用 SQL 原子操作 `current_count = current_count + 1`，避免"读-改-写"导致的丢失更新。

## 📄 License

MIT