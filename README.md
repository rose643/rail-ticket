# Rail Ticket

一个基于 Spring Boot 3 和 Vue 3 开发的铁路购票系统，参考 12306 的核心业务流程实现，覆盖车次查询、乘车人管理、订单创建、库存扣减、支付与退票模拟、订单超时关闭、站内通知及后台票务配置。

> 本项目用于 Java 后端学习、业务建模和求职作品展示，与中国铁路 12306 官方无关。支付与退款目前为站内模拟流程，未接入微信、支付宝等真实支付渠道。

## 项目亮点

- 支持按城市或车站查询指定日期的可售车次、席别、票价和余票。
- 使用 Redis Cache Aside 缓存查票结果，并通过 Redisson 分布式锁缓解缓存击穿。
- 使用“运行日期 + 运行计划”反向索引进行细粒度缓存失效。
- 按途经区段扣减库存，使用数据库事务、行锁和条件更新防止超卖。
- 使用请求幂等键避免用户重复创建订单。
- 使用 RabbitMQ 延迟消息自动关闭超时未支付订单并释放库存。
- 使用本地消息表（Transactional Outbox）、发布确认、失败重试和消费幂等保证消息可靠性。
- 支持 Access Token + Refresh Token 双令牌认证、验证码、退出登录和权限隔离。
- 购票人与乘车人分别建模，实名用户可查询与本人证件关联的车票。
- 前后端分离，页面同时适配桌面端和移动端。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、Spring MVC、Spring Security |
| 持久层 | MyBatis-Plus 3.5、MySQL 8 |
| 缓存与锁 | Redis、Redisson |
| 消息队列 | RabbitMQ、`rabbitmq_delayed_message_exchange` |
| 认证 | JWT、Access Token、Refresh Token |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios |
| 工程化 | Maven Wrapper、npm、Docker、Nginx |
| 测试 | JUnit 5、Spring Boot Test、Mockito |

## 系统结构

```text
浏览器
  │
  ├── Vue 3 前端
  │      │ /api
  │      ▼
  └── Nginx ──► Spring Boot
                    ├── MySQL：业务数据、库存、订单、消息表
                    ├── Redis：验证码、令牌状态、查票缓存、分布式锁
                    └── RabbitMQ：订单延迟关闭、业务通知
```

后端当前采用模块化单体结构。业务按认证、用户、乘车人、基础票务、查票、订单和通知拆分，便于在保持事务一致性的同时逐步演进。

## 功能模块

### 用户端

- 图片验证码、注册、登录、刷新令牌和退出登录
- 实名认证及个人资料维护
- 常用乘车人新增、查询、修改和删除
- 按城市或具体车站搜索车次
- 查看出发时间、到达时间、历时、席别、票价和余票
- 创建订单、查询订单、取消订单、模拟支付和退票
- 查询本人购买的车票及与本人有效证件关联的车票
- 查看站内通知并标记已读

### 管理端

- 车站信息及启停状态管理
- 列车基础信息及启停状态管理
- 经停站、到发时间偏移和停靠顺序管理
- 每日运行计划管理
- 区间席别票价管理
- 分区段座位库存初始化与调整

## 核心业务设计

### 查票缓存

查询请求先读取 Redis；未命中时由一个请求获得 Redisson 分布式锁并回源 MySQL，其他请求短暂等待缓存生成。缓存键包含出发地类型、出发地、目的地类型、目的地和乘车日期。

票价、库存、运行计划或线路发生变化时，通过日期和运行计划反向索引定位受影响的查票缓存，避免直接清除整个日期下的全部数据。

### 区段库存与防超卖

假设某列车依次经过 A、B、C、D，购买 A 到 D 的车票会同时占用 A→B、B→C、C→D 三个区段的库存。可售数量取所有途经区段余票的最小值。

创建订单时在同一数据库事务内锁定相关库存记录并执行条件扣减；任一区段库存不足，整个事务回滚，从而避免部分扣减和超卖。

### 订单超时关闭

订单和待发送事件在同一个本地事务中写入 MySQL。事务提交后立即尝试投递 RabbitMQ 延迟消息，后台补偿任务负责重试发送失败或因应用宕机未发送的消息。消费者收到超时消息后关闭仍处于待支付状态的订单，并恢复其占用的区段库存。

### 消息可靠性

- 本地消息表解决数据库事务与消息发送之间的一致性问题。
- Publisher Confirm 确认消息是否到达 RabbitMQ。
- 消息发送失败后由补偿任务重试。
- 消费记录表和业务状态检查共同防止重复消费。

## 项目目录

```text
rail-ticket/
├── docs/                         # 开发手册与设计说明
├── frontend/                     # Vue 3 前端工程
├── sql/
│   ├── init_rail_ticket.sql      # 完整库表结构
│   └── sample_business_data.sql  # 车站、列车、线路、票价等示例数据
├── src/main/java/                # Spring Boot 后端源码
├── src/main/resources/           # 配置文件与 MyBatis XML
├── src/test/                     # 服务层与业务测试
├── pom.xml                       # Maven 依赖与构建配置
└── README.md
```

更详细的业务约束和开发约定见 [开发手册](docs/开发手册.md)。

## 本地运行

### 1. 环境要求

- JDK 21
- MySQL 8.x
- Redis 7.x
- RabbitMQ 4.x（启用 `rabbitmq_delayed_message_exchange` 插件）
- Node.js 20+ 与 npm

MySQL、Redis 和 RabbitMQ 可以直接安装，也可以通过 Docker 启动。请勿把生产密码或 JWT 密钥提交到仓库。

### 2. 初始化数据库

先创建表结构，再按需导入不包含用户订单数据的业务样例：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < sql/init_rail_ticket.sql
mysql --default-character-set=utf8mb4 -uroot -p rail_ticket < sql/sample_business_data.sql
```

必须以 `utf8mb4` 导入 SQL，否则中文车站和列车名称可能在数据库中变成乱码。

### 3. 配置环境变量

本地开发可在 IDE 的运行配置中添加以下变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=rail_ticket
DB_USERNAME=root
DB_PASSWORD=your-database-password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=your-rabbitmq-user
RABBITMQ_PASSWORD=your-rabbitmq-password

JWT_SECRET=replace-with-a-random-secret-of-at-least-32-bytes
```

不同环境的配置位于：

- `application.yml`：公共配置
- `application-dev.yml`：本地开发配置
- `application-prod.yml`：服务器生产配置
- `application-test.yml`：自动化测试配置

### 4. 启动后端

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS：

```bash
./mvnw spring-boot:run
```

后端默认监听 `http://localhost:8080`。

### 5. 启动前端

```bash
cd frontend
npm ci
npm run dev
```

开发服务器会把 `/api` 请求代理到 `http://localhost:8080`，终端输出的地址即为前端访问地址。

## 常用接口

| 功能 | 方法与路径 |
| --- | --- |
| 获取验证码 | `POST /api/auth/captcha` |
| 用户注册 | `POST /api/auth/register` |
| 用户登录 | `POST /api/auth/login` |
| 刷新令牌 | `POST /api/auth/refresh` |
| 车站建议 | `GET /api/stations/suggestions` |
| 查询车次 | `GET /api/trains/search` |
| 管理乘车人 | `/api/passengers` |
| 管理订单 | `/api/orders` |
| 查询本人车票 | `GET /api/tickets/me` |
| 管理通知 | `/api/notifications` |
| 管理员车站接口 | `/api/admin/stations` |
| 管理员列车接口 | `/api/admin/trains` |

除注册、登录、验证码、刷新令牌和公开查票等接口外，受保护接口需要发送：

```http
Authorization: Bearer <access-token>
```

## 测试与构建

运行后端测试（集成测试前请确保测试环境所需的 MySQL、Redis 和 RabbitMQ 已就绪）：

```powershell
.\mvnw.cmd test
```

构建后端 JAR：

```powershell
.\mvnw.cmd clean package
```

检查并构建前端：

```bash
cd frontend
npm run type-check
npm run build
```

构建结果分别位于后端 `target/` 和前端 `frontend/dist/`，这些目录不会提交到 Git。

## 生产部署建议

- 使用 Nginx 托管 `frontend/dist`，并将 `/api` 反向代理到 Spring Boot。
- 使用 Docker Compose 管理 MySQL、Redis、RabbitMQ 和应用容器。
- 仅开放 80/443，数据库与中间件端口绑定内网或本机地址。
- 使用云安全组和防火墙限制管理端口访问。
- 通过环境变量或密钥管理服务提供数据库密码和 JWT 密钥。
- 配置 HTTPS、日志轮转、数据库备份和容器健康检查。

## 后续计划

- 接入真实支付平台的沙箱环境
- 增加接口文档与自动化 API 测试
- 增加限流、监控告警和链路追踪
- 完善 CI/CD 与一键部署配置
