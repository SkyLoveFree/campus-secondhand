# 校园二手交易系统 — AGENTS.md

> 项目：JavaWeb 课程期末大作业（4 人小组，前端 Vue + 后端 Spring Boot + MySQL）。本文件是规则入口，细节见 `docs/`。
> 阶段：M3（商品查询）——按 `docs/team-assignments.md` 的任务范围开发；未分配模块不得改动。
> 标记：✅ 已确认 ｜ 🟡 建议方案（按此执行，有异议先找 A）｜ ⏳ 待确认。最后更新：2026-10-23（v1.1 精简版）。

## 1. 项目简介与目标

校园二手交易信息平台：注册/登录后可发布商品（名称、价格、联系方式），所有人可浏览、按名称模糊搜索、按价格排序；普通用户只能修改/删除自己的商品，管理员可修改/删除所有商品。

- 本期功能：注册、登录、登出、当前用户、商品列表（分页）、搜索、价格排序、商品详情、发布、修改、删除。
- 本期不做：聊天、支付、订单、收藏、评论、推荐、图片上传、管理员用户管理、ECharts、云部署。
- 成功标准：9 个接口可用、权限在后端生效、README 可按步骤启动、`main` 可构建。
- 详细需求：[docs/requirements.md](docs/requirements.md)。

## 2. 技术栈（已确认）

| 项 | 选择 | 状态 |
|---|---|---|
| JDK | 21 | ✅ |
| 后端 | Spring Boot 3.3.x | 🟡 版本 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia | 🟡 版本 |
| 数据库 | MySQL 8.0.16+（本机 8.4.11） | ✅ |
| 数据访问 | MyBatis（XML Mapper） | 🟡 |
| 构建 | Maven（建议提交 Wrapper）+ npm | 🟡 |
| 认证 | HttpSession + Cookie | 🟡 |
| 密码 | BCrypt（spring-security-crypto） | 🟡 |
| 代码托管 | GitHub 公开仓库 | ✅ |

- 不引入 Redis、消息队列、Docker、微服务、Spring Security 完整框架、Swagger；需要时先经 A 确认。
- 安全按课程基础要求：BCrypt 密码、参数化 SQL、后端权限校验、不提交密码/密钥。

## 3. 目录结构与职责

```text
campus-secondhand/
├─ AGENTS.md / README.md / .gitignore     # A
├─ docs/                                  # 设计文档（见 §13）
├─ sql/                                   # schema.sql、data.sql（C）
├─ backend/                               # Spring Boot 后端（D）
└─ frontend/                              # Vue 3 前端（B）
```

- 文件归属：`docs/`（database-design.md 除外）、`AGENTS.md`、`README.md`、`.gitignore` → **A**；`docs/database-design.md`、`sql/` → **C**；`backend/` → **D**；`frontend/` → **B**。
- 根目录 `背景约束.md`、`暂时框架.md` 是 A 的原始构思，保持原样；冲突以 `docs/` 为准。
- 不得在他人目录内创建或修改文件；新增目录先确认归属。

## 4. 架构分层与模块边界

- 后端：`AuthInterceptor`（认证）→ `Controller`（路由/参数校验）→ `Service`（业务/权限/事务）→ `Mapper`（SQL）→ MySQL。
- Controller 不写业务、不直接调 Mapper；Service 不依赖 `HttpServletRequest`；Mapper 不写业务判断。
- 模块：auth（注册/登录/登出/me/拦截器）、item（商品增删改查/搜索）、common（Result/ErrorCode/异常）。
- 前端：`views` → `api/` → `utils/request.js`；页面不直接拼后端 URL。
- 详见 [docs/architecture.md](docs/architecture.md)。

## 5. 代码风格与命名

- Java：包 `com.campus.secondhand`；类 UpperCamelCase（`XxxController`/`XxxService`/`XxxMapper`/`XxxVO`）；方法/变量 lowerCamelCase；常量 UPPER_SNAKE；4 空格；单文件 ≤500 行、单方法 ≤50 行。
- 实体只映射数据库；对外用 DTO/VO，**不返回 `password_hash`**；日志用 SLF4J，禁止 `System.out.println`，禁止打印密码/哈希。
- 前端：组件 PascalCase（`ItemList.vue`）；请求统一走 `src/api/`；表单前端先校验，后端仍必须校验。
- 前端状态用 Pinia 管理登录用户；路由使用 **hash 模式**；列表页搜索/排序/分页同步到 URL query；列表卡片只展示名称/价格/时间，联系方式在详情页展示。
- API JSON camelCase（`itemPrice`/`contactInfo`/`userId`/`createdAt`）；数据库 snake_case（`item_price`/`contact_info`/`user_id`）。
- 不提交调试代码、注释掉的大段代码、`target/`、`node_modules/`、`dist/`、密钥。

## 6. API 调用与响应规范

- **唯一契约**：[docs/api-spec.md](docs/api-spec.md)；没有写进契约的接口/字段不得私自增加。
- 基路径 `/api`；统一响应 `{code, message, data}`；成功 HTTP 200 + `code=0`；失败用 400/401/403/404/405/409/500 + 业务码（见 api-spec 错误码表）。
- 参数校验失败：HTTP 400 + `10001`，`data.fieldErrors` 给出字段错误。
- 分页：`page` 默认 1、`size` 默认 10（≤50）；搜索 `keyword`；排序 `sort=price_asc|price_desc`。
- 认证：Session + Cookie（🟡），Axios `withCredentials: true`；开发期 Vite 代理 `/api → http://localhost:8080`。
- 修改契约：先改 api-spec 并经 A 确认，再改代码。

## 7. 数据库设计与访问规范

- 权威来源：[docs/database-design.md](docs/database-design.md)、[sql/schema.sql](sql/schema.sql)、[sql/data.sql](sql/data.sql)。
- 两张表：`user`、`item`；`item.user_id → user.id`（外键 RESTRICT）；商品物理删除；用户本期不提供删除。
- 字段 snake_case；金额 `DECIMAL(10,2)`；时间 `DATETIME`；角色 `VARCHAR(16)`（`USER`/`ADMIN`）；字符集 `utf8mb4`。
- MyBatis 必须 `#{}` 参数化，禁止 `${}` 拼接用户输入；SQL 由 C 设计与评审、D 实现 Mapper。
- 密码只存 BCrypt 哈希；测试数据（admin/student，口令 `123456`）仅本地使用。
- 改表结构：C 改 `sql/` 与 database-design，D 改实体/Mapper，须 A 确认；禁止先改代码后补文档。

## 8. 认证、权限与数据归属

- 登录成功后在 Session 保存 `currentUserId`、`currentUsername`、`currentRole`；Session 闲置超时 30 分钟（🟡）。
- 白名单：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/items`、`GET /api/items/{id}`；其他 `/api/**` 需登录。
- 角色：游客、`USER`、`ADMIN`；注册接口只创建 `USER`；管理员由 `sql/data.sql` 种子创建。
- 权限：所有人可浏览；`USER` 只能改删自己的商品；`ADMIN` 可改删所有商品。
- **商品归属只依据 Session 当前用户 ID 与数据库 `item.user_id` 比对；请求中的 `userId`/`user_id` 一律忽略。**
- 后端必须校验；前端隐藏按钮只是体验。未登录 401、无权限 403、商品不存在 404。
- 详见 [docs/api-spec.md](docs/api-spec.md)、[docs/architecture.md](docs/architecture.md)。

## 9. 错误处理与参数校验

- 统一用 `Result` 返回；业务异常用 `BusinessException` + 全局异常处理。
- 错误码：`10001` 参数校验、`10002` 未登录、`10003` 登录失败、`10004` 无权限、`10005` 不存在、`10006` 用户名重复、`10007` 请求格式、`10008` 方法不支持、`10500` 服务器错误。
- 所有接口入参后端校验（`@Valid` + 业务校验），字符串 trim 后校验；错误信息为中文可展示。
- 响应不返回堆栈/SQL/密码；禁止空 catch、禁止吞异常；未预期异常记日志并返回 500。

## 10. 测试、构建与运行

| 操作 | 命令 |
|---|---|
| 初始化数据库 | `cmd /c "mysql -u<user> -p < sql\schema.sql"`，再 `cmd /c "mysql -u<user> -p campus_secondhand < sql\data.sql"` |
| 后端运行 | `cd backend && mvn spring-boot:run`（或 `mvnw.cmd spring-boot:run`） |
| 后端测试/打包 | `cd backend && mvn test`；`mvn clean package` → `java -jar target/*.jar` |
| 前端运行/构建 | `cd frontend && npm install && npm run dev`；`npm run build` |

- 环境：JDK 21、Maven 3.9+（或 Maven Wrapper）、Node.js 20+、MySQL 8.0.16+。
- 数据库密码用环境变量或本地 `application-local.yml`（不提交）。
- 完成前必须：后端编译/相关测试通过、前端 `npm run build` 通过、手工自测相关接口；**没跑的测试不许说通过**。

## 11. Git 协作与代码提交

- `main` 分支可运行、禁止直接 push；功能分支 `feature/<成员>-<模块>`（如 `feature/d-item-api`），修复 `fix/<简述>`。
- 文档同步（A 的例外）：A 确认决定或修改 `docs/`、`AGENTS.md`、`README.md` 后，可直接在 `main` 提交并 push，队友及时 `git pull`；代码变更仍走功能分支 + PR。
- Commit：Conventional Commits + 中文说明，如 `feat(item): 商品列表搜索与排序`；小步提交，每人保留自己的提交记录。
- PR：Reviewer 至少 A + 1 名相关成员；合并用 **Merge Commit，禁止 Squash**（课程要个人提交记录）。
- 不提交敏感信息、构建产物、依赖目录；冲突先 rebase，不得丢弃他人代码。
- 公共文件（`pom.xml`、`request.js`、`router`、`sql/schema.sql`、`docs/*`、`AGENTS.md`）改动前先同步。
- 未明确要求时，Agent 不自行 `git commit` / `git push`。

## 12. AI 编程代理工作约束

1. 修改前先读本文件、相关 docs 和现有代码，确认模块边界。
2. 不得擅自改已确认的 API 契约、数据库字段、错误码、模块职责；🟡 项按当前方案执行，改动先问 A。
3. 需要改设计时先说明原因、影响范围、替代方案，等 A 确认，不先改后报。
4. 不要无要求重写整个项目或批量覆盖文件；只做最小必要改动。
5. 不把未运行的测试描述为已经通过；不能验证时标注「未验证」。
6. 不提交数据库密码、Token 等敏感信息，不写入响应/日志。
7. 只做分配模块，不越权修改他人目录；发现冲突/漏洞先报告。
8. 复杂实现（权限判定、搜索排序）说明思路，保证同学能讲清楚。
9. 当前是 M2 编码阶段：按 `docs/team-assignments.md` 中分配的任务范围开发；未分配模块或未列入本期需求的功能不主动实现。
10. 完成后汇报：改了哪些文件、怎么验证、有什么待确认。

## 13. 文档地图与当前阶段

| 文档 | 内容 |
|---|---|
| [requirements.md](docs/requirements.md) | 需求、范围、权限、验收 |
| [architecture.md](docs/architecture.md) | 架构、分层、认证、目录 |
| [api-spec.md](docs/api-spec.md) | 9 个接口、错误码、示例 |
| [database-design.md](docs/database-design.md) | 表结构、索引、种子数据 |
| [team-assignments.md](docs/team-assignments.md) | 分工、任务、里程碑 |
| [development-workflow.md](docs/development-workflow.md) | Git、PR、联调、部署 |
| [design-review.md](docs/design-review.md) | 交叉核对与确认结论 |
| `sql/schema.sql`、`sql/data.sql` | 建表与测试数据脚本 |
| `README.md` | 环境、启动、部署（M2/M5 阶段补） |

- 当前：M3 已完成初版（2026-10-23）。M1/M2 已完成；D3 商品查询接口、B4/B5 列表/详情页已完成并验证；下一步 M4（发布/修改/删除 + 权限联调）。
- 文档冲突时：需求看 requirements、接口看 api-spec、表结构看 database-design/schema.sql、流程看 development-workflow；仍冲突先停下问 A。
