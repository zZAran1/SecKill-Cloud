# 秒杀专场 SecKill

一个基于 **Spring Cloud 微服务 + Vue 3** 的高并发秒杀系统。围绕「限流削峰、异步建单、缓存前置、安全加固」四个核心目标设计，覆盖用户、商品、订单、秒杀四大业务域，并提供一键 Docker Compose 部署。

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [模块结构](#模块结构)
- [核心功能](#核心功能)
- [秒杀核心流程](#秒杀核心流程)
- [安全设计](#安全设计)
- [快速开始](#快速开始)
  - [方式一：Docker Compose 一键部署（推荐）](#方式一docker-compose-一键部署推荐)
  - [方式二：本地开发运行](#方式二本地开发运行)
- [环境变量](#环境变量)
- [接口概览](#接口概览)
- [数据库与迁移](#数据库与迁移)
- [默认账号](#默认账号)

---

## 项目简介

秒杀场景的典型挑战是**瞬时流量洪峰**与**库存超卖**。本项目通过多层防护解决这些问题：

- **网关层限流削峰**：Spring Cloud Gateway + Redis 令牌桶，按用户 ID / IP 限流。
- **缓存前置拦截**：活动信息、库存预热到 Redis，绝大部分请求不落库。
- **Lua 原子扣减**：防重、查库存、扣库存、记录用户在 Redis 中原子完成，杜绝超卖。
- **异步建单削峰**：抢购成功只发 MQ 消息，订单异步落库，解决 DB 瓶颈。
- **延迟队列关单**：TTL 延迟队列 + 死信转发，实现支付超时自动关单、库存回补。
- **统一安全收敛**：JWT（RSA 非对称）、网关验签、身份头注入、服务间签名，全部在网关统一处理。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0 |
| 微服务 | Spring Cloud Gateway、Nacos（注册中心）、OpenFeign |
| 数据访问 | MyBatis-Plus 3.5.5、MySQL 8.0、Flyway（版本化迁移） |
| 缓存 | Redis 7（Lua 脚本、令牌桶限流、缓存预热） |
| 消息队列 | RabbitMQ 3.13（异步建单、TTL 延迟队列、死信队列、手动 ACK） |
| 安全 | JWT（RSA 非对称签名）、Spring Security、HmacSHA256 网关签名 |
| 工具 | Lombok、MapStruct、Swagger/OpenDoc、JUnit |
| 前端 | Vue 3.5、Vite 8、TypeScript、Vue Router 4、Axios |
| 部署 | Docker / Docker Compose、Nginx（静态托管 + 反向代理）、多阶段构建 |
| 运行环境 | Java 21、Node.js 22+ |

## 系统架构

```
                         ┌─────────────────┐
      用户浏览器 ───────▶│  前端 (Nginx)    │ :80
                         │  托管 Vue 静态站  │
                         │  /api ──反代──┐  │
                         └──────────────┼──┘
                                        │
                         ┌──────────────▼──────────────────────────┐
                         │        网关 Gateway :8080               │
                         │  · JWT 验签（RSA 公钥）                  │
                         │  · 限流（Redis 令牌桶，用户/IP）          │
                         │  · 身份头注入 X-User-Id / X-Role ...     │
                         │  · 网关签名 X-Gateway-Signature          │
                         └───┬──────────┬──────────┬───────────────┘
                             │          │          │
              ┌──────────────▼──┐  ┌────▼─────┐  ┌─▼──────────────┐
              │ user  :8081     │  │ product  │  │ order  :8083   │
              │ 用户/鉴权/管理   │  │ :8082    │  │ 订单/秒杀       │
              │ 签发 JWT(私钥)   │  │ 商品/活动 │  │ 异步建单        │
              └──────┬──────────┘  └────┬─────┘  └─┬──────────────┘
                     │                  │          │
              ┌──────▼──────────────────▼──────────▼──────┐
              │   Nacos :8848   │  MySQL  │ Redis │ RabbitMQ │
              │   (注册中心)     │  各服务  │ 缓存   │  消息队列 │
              │                 │  独立库  │ 限流   │  异步建单 │
              └────────────────────────────────────────────┘
```

**关键设计**：

- **单一入口**：前端与外部流量只能经网关（8080）进入，业务服务端口（8081-8083）不对外暴露，消除直连绕过网关的攻击面。
- **鉴权上移**：各业务服务不再自行解析 JWT，仅信任网关注入的 `X-User-Id`、`X-Username`、`X-Role`、`X-Status` 请求头。
- **数据库隔离**：每个服务连接独立数据库（`seckill_user` / `seckill_product` / `seckill_order`），避免 Flyway 版本冲突。

## 模块结构

```
SecKill/
├── SecKill-backend/                # 后端 Maven 多模块工程
│   ├── SecKill-common/             # 公共模块：统一结果、异常、网关签名过滤器、角色切面、JWT 工具
│   ├── SecKill-gateway/            # 网关：路由、JWT 验签、限流、身份头注入（WebFlux）
│   ├── SecKill-user/               # 用户服务：注册/登录/鉴权、个人信息、用户管理（:8081）
│   ├── SecKill-product/            # 商品服务：商品管理、秒杀活动管理、库存预热（:8082）
│   ├── SecKill-order/              # 订单服务：订单管理、秒杀下单、MQ 消费、延迟关单（:8083）
│   └── mvnw / mvnw.cmd             # Maven Wrapper
├── seckill-frontend/               # 前端 Vue 3 + Vite 工程
│   └── src/
│       ├── api/                    # Axios 封装与各业务接口
│       ├── views/                  # 页面视图（登录、商品、秒杀、订单、管理后台…）
│       ├── components/             # 通用组件（头部、商品卡片、分页、Toast）
│       ├── router/                 # 路由与登录/管理员守卫
│       ├── stores/                 # 状态管理（鉴权状态）
│       └── utils/                  # 格式化、安全、提示工具
├── docker/
│   └── mysql/init.sql              # MySQL 初始化：建库、建业务账号、授权
├── docker-compose.yml              # 一键编排：中间件 + 微服务 + 前端
└── .env                            # 密钥与密码模板（须按需修改）
```

## 核心功能

### 用户域（user）
- 注册、登录（JWT 签发）、登出（token 黑名单）、获取/修改个人信息、修改密码
- 登录失败次数限制（防暴力破解）
- 管理员：分页查询用户、启用/禁用账号

### 商品域（product）
- 商品增删改查、分页、上下架状态管理
- 秒杀活动管理：创建/编辑活动、上线/下线、库存预热（活动信息 + 库存写入 Redis）
- 内部接口：扣减库存 / 回补库存（供订单服务 Feign 调用）

### 订单域（order）
- 普通下单、我的订单、订单分页、订单详情、支付、取消
- 秒杀下单：`POST /seckill/{activityId}` 返回「抢购排队中」，`GET /seckill/{activityId}/result` 轮询结果
- MQ 异步建单消费者（手动 ACK、异常入死信队列兜底）
- 支付超时自动关单（TTL 延迟队列）+ 库存回补

### 前端页面
- 商品列表 / 详情 / 发布 / 编辑
- 秒杀专场（倒计时、抢购、结果轮询）
- 订单中心（我的订单）
- 个人中心（资料、密码）
- 管理后台（活动管理、用户管理、订单管理，仅管理员可见）

## 秒杀核心流程

秒杀是本系统的核心链路，整体分为**同步抢资格**与**异步建单**两段：

```
用户抢购 ─▶ 网关限流 ─▶ 校验活动时间窗
                          │
                          ▼
              Redis Lua 原子操作（一步完成）
              ┌─────────────────────────────────┐
              │ 1. 防重检查（已抢过则拒绝）        │
              │ 2. 库存检查（不足则返回已售罄）    │
              │ 3. 扣减库存                       │
              │ 4. 记录用户到「已参与」集合         │
              └─────────────────────────────────┘
                          │
                          ▼
              发送 MQ 消息 ─▶ 返回「抢购排队中」
                          │
                          ▼ （异步）
              消费者建单落库 ─▶ 用户轮询拿到订单号
                          │
                          ▼ （延迟 TTL）
              超时未支付 ─▶ 自动关单 + 库存/资格回补
```

**要点说明**：

- **Lua 原子脚本**：防重、库存检查、扣减、记录用户在 Redis 中原子执行，避免并发下的超卖与重复抢购。
- **异步建单**：抢购接口只发消息立即返回，DB 写入削峰到消费者串行处理。
- **失败回补**：MQ 发送失败时立即回补库存与用户资格，避免「扣了库存却没建单」的泄漏。
- **死信兜底**：消费系统异常时消息转入死信队列，由 `SeckillOrderDlqConsumer` 兜底处理。
- **延迟关单**：下单后消息进入 TTL 延迟队列，到期转入关单队列，查库确认仍待支付则关单并回补库存。

## 安全设计

| 机制 | 说明 |
|------|------|
| JWT 非对称签名 | user 服务持**私钥**签发，网关与各服务持**公钥**验签，避免对称密钥泄露风险 |
| 网关验签 + 身份注入 | 网关校验 JWT 后注入 `X-User-Id`、`X-Username`、`X-Role`、`X-Status`，下游只信任这些头 |
| 防身份伪造 | 网关在白名单判断前**剥离**客户端伪造的 `X-*` 身份头 |
| 服务间签名 | 网关加 `X-Gateway-Signature`（HmacSHA256），业务服务 `GatewaySignatureFilter` 校验，拦截直连伪造 |
| 网关限流 | `RequestRateLimiter` 令牌桶，按用户 ID 优先、IP 降级；秒杀接口更严格（5/秒，突发 10） |
| 角色鉴权 | `@RequiresRole` 注解 + AOP 切面，从 `X-Role` 取值比对，管理接口仅管理员可用 |
| 密钥外部化 | 所有密钥、密码、连接串通过环境变量注入，禁止硬编码 |
| 登录防护 | 登录失败次数限制，防暴力破解 |
| Token 黑名单 | 登出 token 记入黑名单，由网关统一拦截 |

## 快速开始

### 方式一：Docker Compose 一键部署（推荐）

**前置要求**：已安装 Docker 与 Docker Compose。

1. **准备密钥文件**：复制 `.env` 模板并按需修改密码/密钥。

   ```bash
   # .env 中包含 MySQL/Redis/RabbitMQ 密码、JWT RSA 密钥对、网关内部密钥
   # 生产环境务必替换所有默认值，JWT 密钥对须重新生成
   ```

2. **构建并启动全部服务**（中间件 + 4 个微服务 + 前端）：

   ```bash
   docker-compose up -d --build
   ```

3. **访问**：
   - 前端页面：<http://localhost>
   - 网关入口：<http://localhost:8080>
   - Nacos 控制台：<http://localhost:8848>（默认账号 nacos/nacos）
   - RabbitMQ 管理台：<http://localhost:15672>

> 首次启动时，MySQL 数据卷为空会自动执行 `docker/mysql/init.sql` 建库建账号；各服务通过 Flyway 自动建表并写入初始管理员。若 MySQL/Nacos 未就绪，服务会自动重启重试。

### 方式二：本地开发运行

**前置要求**：Java 21、Maven（或用 `mvnw`）、Node.js 22+、本地 MySQL / Redis / RabbitMQ / Nacos。

1. **启动中间件**：确保 MySQL、Redis、RabbitMQ、Nacos 在本地运行，配置见各服务 `application-dev.yml`。

2. **编译后端**：

   ```bash
   cd SecKill-backend
   ./mvnw clean package -DskipTests
   ```

3. **启动各服务**（建议顺序：user → product → order → gateway），可用 IDEA 的 `.run` 运行配置：

   ```bash
   # 也可逐个启动
   ./mvnw -pl SecKill-user spring-boot:run
   ./mvnw -pl SecKill-product spring-boot:run
   ./mvnw -pl SecKill-order spring-boot:run
   ./mvnw -pl SecKill-gateway spring-boot:run
   ```

4. **启动前端**：

   ```bash
   cd seckill-frontend
   npm install
   npm run dev        # 默认 http://localhost:5173
   ```

   前端开发服务器将 `/api` 代理到网关（默认 `http://localhost:8080`），可通过 `VITE_PROXY_TARGET` 覆盖。

## 环境变量

后端通过环境变量注入密钥与连接信息（`.env` 供 Docker Compose 使用）：

| 变量 | 说明 | 示例/默认 |
|------|------|----------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `root` |
| `DB_PASSWORD` | 业务库密码（与 init.sql 建账号一致） | `123456` |
| `DB_USERNAME` / `DB_HOST` / `DB_PORT` / `DB_NAME` | 各服务数据源 | `seckill` / `mysql` / `3306` / 按服务分库 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `redis` / `6379` / 自定义 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 连接 | `rabbitmq` / `5672` / `seckill` |
| `NACOS_ADDR` | Nacos 地址 | `nacos:8848` |
| `NACOS_DISCOVERY_IP` | 注册 IP（置空让 Nacos 自动探测容器 IP） | 空 / `127.0.0.1` |
| `JWT_PRIVATE_KEY` | JWT RSA 私钥（仅 user 服务签发用） | 见 `.env` |
| `JWT_PUBLIC_KEY` | JWT RSA 公钥（网关/各服务验签） | 见 `.env` |
| `JWT_EXPIRATION` | token 有效期（毫秒） | `86400000`（24h） |
| `GATEWAY_INTERNAL_SECRET` | 网关与服务间 HmacSHA256 签名密钥 | 见 `.env` |
| `SPRING_PROFILES_ACTIVE` | 环境 profile | `dev` / `prod` |

前端环境变量：

| 变量 | 说明 | 默认 |
|------|------|------|
| `VITE_PROXY_TARGET` | 开发代理目标（网关地址） | `http://localhost:8080` |
| `VITE_API_BASE_URL` | 生产 API 基础地址 | 走 Nginx `/api` 反代 |

> **安全提醒**：生产环境的 `JWT_PRIVATE_KEY`、`JWT_PUBLIC_KEY`、`GATEWAY_INTERNAL_SECRET` 与各数据库密码**必须重新生成并妥善保管**，切勿使用仓库中的示例值。

## 接口概览

所有接口统一经网关访问，响应统一为 `{ code, message, data }` 格式。生产环境 Swagger 关闭，开发环境可访问各服务的 `/swagger-ui.html`。

| 域 | 方法与路径 | 说明 | 权限 |
|----|-----------|------|------|
| 用户 | `POST /users` | 注册 | 公开 |
| 用户 | `POST /users/login` | 登录 | 公开 |
| 用户 | `POST /users/logout` | 登出 | 登录 |
| 用户 | `GET /users/me` | 获取个人信息 | 登录 |
| 用户 | `PUT /users/me` | 修改个人信息 | 登录 |
| 用户 | `PUT /users/password` | 修改密码 | 登录 |
| 用户 | `GET /users/page` | 用户分页 | 管理员 |
| 用户 | `PUT /users/{id}/status` | 启用/禁用 | 管理员 |
| 商品 | `GET /products`、`GET /products/{id}`、`GET /products/page` | 商品浏览 | 公开 |
| 商品 | `POST /products`、`PUT /products/{id}`、`PUT /products/{id}/status` | 商品管理 | 管理员 |
| 活动 | `GET /seckill-activities`、`GET /seckill-activities/{id}`、`/page` | 活动浏览 | 公开 |
| 活动 | `POST /seckill-activities`、`PUT /{id}`、`/{id}/preload`、`/{id}/online`、`/{id}/offline` | 活动管理 | 管理员 |
| 秒杀 | `POST /seckill/{activityId}` | 抢购（返回排队中） | 登录 |
| 秒杀 | `GET /seckill/{activityId}/result` | 查询抢购结果 | 登录 |
| 订单 | `POST /orders` | 普通下单 | 登录 |
| 订单 | `GET /orders/my`、`GET /orders/{orderNo}` | 我的订单/详情 | 登录 |
| 订单 | `POST /orders/{orderNo}/pay`、`/cancel` | 支付/取消 | 登录 |
| 订单 | `GET /orders/page` | 订单分页 | 管理员 |
| 内部 | `POST /internal/products/{id}/deduct-stock`、`/restore-stock` | 库存扣减/回补（Feign） | 服务间 |

## 数据库与迁移

- **分库隔离**：`seckill_user`（用户）、`seckill_product`（商品/活动）、`seckill_order`（订单），各自独立，避免迁移版本冲突。
- **Flyway 管理**：表结构**只能**由 `classpath:db/migration` 下的迁移脚本创建和修改，命名规则 `V{版本}__{描述}.sql`（如 `V1__create_users_table.sql`）。
- **不可变原则**：已执行的迁移脚本**永不修改**，表结构变更需新增版本号递增的脚本。
- **自动执行**：Flyway 配置在主 `application.yml`，各环境启动时自动同步结构。

迁移脚本一览：

| 服务 | 脚本 |
|------|------|
| user | `V1__create_users_table`、`V2__seed_admin_user` |
| product | `V1__create_products_table`、`V2__create_seckill_activity_table`、`V3__fix_seckill_activity_constraints`、`V4__add_stock_to_products` |
| order | `V1__create_orders_table`、`V2__add_unique_index_user_activity`、`V3__add_expire_time_to_orders` |

## 默认账号

系统首次启动时由 `V2__seed_admin_user.sql` 写入超级管理员：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | 见下方说明（BCrypt 加密存储） | 超级管理员（role=0） |

> **密码说明**：迁移脚本中仅存储 BCrypt 哈希（`$2a$10$qn.pn...`），仓库内未记录对应明文。部署后请用已知明文重新生成管理员密码，或通过前端注册一个新账号后在数据库中将其 `role` 置为 `0`。**首次登录后请立即修改密码**。普通用户可通过前端注册页面自助注册。
