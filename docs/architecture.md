# 校园二手交易系统 — 系统架构设计

> 文档编号：ARC-001 ｜ 版本：v1.1（精简版）｜ 状态：评审中 ｜ 最后更新：2026-10-23
> 关联：[需求](requirements.md) · [API](api-spec.md) · [数据库](database-design.md) · [分工](team-assignments.md) · [流程](development-workflow.md) · [评审](design-review.md)

## 1. 总体架构

```text
Vue 3 前端（开发 :5173 / 构建后为静态资源）
        │  HTTP/JSON，统一 /api 前缀
        ▼
Spring Boot 后端（:8080）
  AuthInterceptor → Controller → Service → Mapper
  统一 Result 返回 / 全局异常处理
        │ JDBC（参数化）
        ▼
MySQL 8（:3306，库 campus_secondhand）
```

- 前后端分离：前端只做展示与交互，后端负责业务、认证、权限、校验，MySQL 负责持久化。
- 原则：契约先行、简单够用、分层清晰、权限由后端强制、不引入课程范围外的中间件。

## 2. 技术选型

| 项 | 选择 | 状态 | 说明 |
|---|---|---|---|
| JDK | 21 | ✅ | 本机 Temurin 21 |
| 后端 | Spring Boot 3.3.x | 🟡 | 具体版本在 `pom.xml` 锁定 |
| 前端 | Vue 3 + Vite + Element Plus | 🟡 | Node.js 20+ |
| 数据库 | MySQL 8.0.16+ | ✅ | 本机 8.4.11；CHECK 约束需 8.0.16+ |
| 数据访问 | MyBatis（XML Mapper） | 🟡 | 不用 JPA |
| 构建 | Maven（建议 Maven Wrapper）+ npm | 🟡 | Windows 开发 |
| 认证 | HttpSession + Cookie | 🟡 | 不用 JWT（课程够用、实现简单） |
| 密码 | BCrypt（spring-security-crypto） | 🟡 | 不引入 Spring Security 完整框架 |
| 代码托管 | GitHub | ✅ | 公开仓库 |

- 不引入 Redis、消息队列、Docker、微服务、Swagger；确需引入须 A 确认。
- 安全按课程基础要求，不做 CSRF Token、HTTPS 等额外加固。

## 3. 分层与模块边界

| 层 | 职责 | 禁止 |
|---|---|---|
| Controller | 路由、参数绑定/`@Valid`、调用 Service、返回 `Result` | 写业务、直接调 Mapper、拼 SQL |
| Service | 业务规则、权限归属判定、事务 | 依赖 `HttpServletRequest`/Session、返回 `Result` |
| Mapper | MyBatis SQL、结果映射 | 业务判断、事务 |
| Entity/DTO | 数据载体 | 业务逻辑 |

- 模块：auth（注册/登录/登出/me/拦截器）、item（商品 CRUD/搜索/排序/分页）、common（Result/ErrorCode/异常/配置）。
- 依赖方向：item 依赖 auth 提供的当前用户，反向不依赖；模块间以 Service 接口协作。
- 前端：`views` → `components` → `api/` → `utils/request.js`；页面不直接拼后端 URL。

## 4. 请求处理链路

```text
请求 → AuthInterceptor（白名单放行；其余校验 Session，未登录 401）
     → Controller（@Valid 校验参数）
     → Service（业务规则 + 权限归属 + 事务）
     → Mapper（#{} 参数化 SQL）
     → MySQL
响应 ← Controller 包装 Result ← Service 返回业务对象
异常：Controller/Service 抛出 → GlobalExceptionHandler 转为统一错误响应
```

- 成功统一 HTTP 200 + `code=0`；失败按 400/401/403/404/405/409/500 + 业务码返回。
- 错误响应不返回堆栈、SQL、密码；未预期异常记日志后返回 500。

## 5. 认证与授权

- 登录：按用户名查用户 → BCrypt 校验 → 创建 Session（`currentUserId`、`currentUsername`、`currentRole`）→ 返回 `id/username/role`。
- Cookie：容器默认 `JSESSIONID`，`HttpOnly`；闲置超时 30 分钟；后端重启丢失登录态（课程演示可接受）。
- 白名单：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/items`、`GET /api/items/{id}`；其余 `/api/**` 需登录。
- 权限判定：查商品（不存在 404）→ `ADMIN` 放行 → 本人放行 → 其他 403。
- 商品归属只依据 Session 当前用户 ID 与数据库 `item.user_id`；请求中的 `userId` 忽略。
- 管理员账号由 `sql/data.sql` 创建，注册接口只创建 `USER`。

## 6. 统一约定

- 响应：`{ "code": 0, "message": "ok", "data": ... }`；错误码见 [api-spec.md](api-spec.md)。
- JSON camelCase（`itemPrice`/`contactInfo`/`userId`/`createdAt`）；数据库 snake_case，映射见 [database-design.md](database-design.md)。
- 分页 `page`/`size`（默认 1/10，上限 50）；搜索 `keyword`；排序 `price_asc`/`price_desc`，默认创建时间倒序。
- 时间 `yyyy-MM-dd HH:mm:ss`（Asia/Shanghai）；金额 JSON number，数据库 `DECIMAL(10,2)`。
- 开发期 Vite 代理 `/api → http://localhost:8080`，Cookie 同源；演示时单 Jar 同域。

## 7. 目录结构

```text
campus-secondhand/
├─ AGENTS.md、README.md、.gitignore        # A
├─ docs/                                    # 设计文档（database-design.md 由 C）
├─ sql/                                     # schema.sql、data.sql（C）
├─ backend/                                 # Spring Boot（D）
│  ├─ pom.xml
│  └─ src/main/java/com/campus/secondhand/
│     ├─ controller/ service/ mapper/ entity/ dto/ common/ config/ interceptor/
│     └─ resources/ application.yml、mapper/*.xml
└─ frontend/                                # Vue 3（B）
   ├─ package.json、vite.config.js
   └─ src/ api/ views/ components/ router/ utils/
```

## 8. 开发与部署

- 开发环境：后端 `8080`、前端 `5173`（Vite）、MySQL `3306`；后端用 `mvn spring-boot:run`，前端 `npm run dev`。
- 演示部署：`frontend npm run build` → 复制 `dist/` 到 `backend/src/main/resources/static/` → `mvn clean package` → `java -jar`，浏览器访问 `http://localhost:8080`。
- 数据库账号/密码通过环境变量或本地 `application-local.yml` 注入，不提交仓库。
- 数据库初始化：`sql/schema.sql` → `sql/data.sql`（命令见 [database-design.md](database-design.md)）。

## 9. 风险与待处理

- **Session/Cookie 联调**是最大风险：M2 必须先打通「登录 → 刷新保持 → 401 处理」。
- SQL 脚本尚未实机执行（本机 MySQL root 非免密）：M1 出口前由 A 在真实 MySQL 验证并记录。
- 本机未安装 Maven；Git 未初始化、README 未创建：M1 出口前补齐。
- 其余事项（成员信息、排期）已确认暂不处理，见 [design-review.md](design-review.md)。

## 10. 变更记录

| 日期 | 版本 | 修改人 | 内容 |
|---|---|---|---|
| 2026-10-23 | v1.1 | A | 精简版：保留架构/分层/认证/目录/部署要点，删除安全扩展与长决策表 |
