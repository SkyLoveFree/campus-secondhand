# 校园二手交易系统 — 使用手册

> 版本：v1.0 ｜ 最后更新：2026-10-24 ｜ 适用对象：验收老师、演示同学、新加入成员
> 项目仓库：https://github.com/SkyLoveFree/campus-secondhand
> 设计文档入口：[AGENTS.md](../AGENTS.md) ｜ [docs/](.)

## 1. 系统简介

校园二手交易信息平台：用户注册登录后可发布二手商品（名称、价格、联系方式），所有访问者可浏览、按名称模糊搜索、按价格排序；普通用户只能修改/删除自己发布的商品，管理员可修改/删除所有商品。

| 角色 | 可做什么 |
|---|---|
| 游客 | 浏览商品、搜索、价格排序、查看详情、注册、登录 |
| 普通用户 USER | 游客能力 + 发布商品 + 修改/删除自己的商品 + 查看当前用户 + 登出 |
| 管理员 ADMIN | 普通用户能力 + 修改/删除所有商品 |

技术栈：JDK 21、Spring Boot 3.3.5、MyBatis、MySQL 8.0.16+、Vue 3 + Vite + Pinia + Element Plus。

## 2. 运行环境

| 工具 | 版本要求 | 说明 |
|---|---|---|
| JDK | 21 | 后端编译/运行 |
| Maven | 3.9+ | 仓库已带 Maven Wrapper，可免安装 |
| Node.js | 20+ | 前端构建/开发服务器 |
| MySQL | 8.0.16+ | 本机开发使用 8.4.11；建表脚本依赖 CHECK 约束 |
| 浏览器 | Chrome / Edge 新版本 | 前端为 Vue 3 应用 |

## 3. 获取代码与目录

```powershell
git clone https://github.com/SkyLoveFree/campus-secondhand.git
cd campus-secondhand
```

```text
campus-secondhand/
├─ AGENTS.md              # 项目规则入口
├─ README.md              # 快速开始
├─ docs/                  # 需求、架构、API、数据库、分工、流程、评审、手册
├─ sql/                   # schema.sql（建表）、data.sql（测试数据）
├─ backend/               # Spring Boot 后端（含 mvnw / mvnw.cmd）
├─ frontend/              # Vue 3 前端
└─ screenshots/           # 14 张演示截图
```

## 4. 数据库初始化

```powershell
# PowerShell 不支持 < 重定向，用 cmd /c 包一层
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\schema.sql"
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\data.sql"
```

- 会创建数据库 `campus_secondhand` 并重建 `user`、`item` 两张表。
- `data.sql` 可重复执行（先删后插），执行后固定 2 个账号 + 5 条商品。

**测试账号（仅本地/演示环境）**

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 ADMIN |
| `student` | `123456` | 普通用户 USER |

> 这两个账号是课程测试数据，禁止用于真实环境；数据库密码不要把真实值写进仓库。

## 5. 启动方式

### 5.1 启动后端（开发模式）

```powershell
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<本机 MySQL 密码>"
.\mvnw.cmd spring-boot:run      # 首次会自动下载 Maven（需联网）；已装 Maven 可用 mvn spring-boot:run
```

后端地址：`http://localhost:8080`，所有接口以 `/api` 开头。
验证：浏览器访问 `http://localhost:8080/api/items` 应返回 5 条商品 JSON。

### 5.2 启动前端（开发模式）

```powershell
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`；开发期 `/api` 由 Vite 代理到 8080，Cookie 同源，无需额外跨域配置。

### 5.3 单 Jar 部署（演示/交付推荐）

```powershell
cd frontend
npm install
npm run build

# 把前端产物复制到后端静态资源目录（先清空旧产物）
$static = "..\backend\src\main\resources\static"
New-Item -ItemType Directory -Force $static | Out-Null
Remove-Item "$static\*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item ".\dist\*" -Destination $static -Recurse -Force

cd ..\backend
$env:DB_USERNAME = "root"; $env:DB_PASSWORD = "<本机密码>"
.\mvnw.cmd -q clean package
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1
java -jar $jar.FullName
```

浏览器访问 `http://localhost:8080`：前端页面与 `/api` 同域，刷新不会 404（前端使用 hash 路由）。

### 5.4 一键启动（Windows，推荐）

双击项目根目录的 `一键启动.bat`，或在 PowerShell 执行：

```powershell
.\start.ps1              # 已有 Jar 时直接启动
.\start.ps1 -Rebuild     # 修改代码后重新构建前端 + 后端
.\start.ps1 -Port 8080   # 指定端口
```

脚本会自动检查 Java、端口、Jar、数据库连接，设置环境变量后启动，并在 8 秒后打开浏览器。首次运行会询问 MySQL 用户名/密码；也可以提前设置 `DB_USERNAME`/`DB_PASSWORD`，或在根目录创建 `.env.local`（已被 `.gitignore` 忽略）：

```text
DB_USERNAME=root
DB_PASSWORD=你的密码
```

停止：按 `Ctrl+C`，或双击 `停止服务.bat`（等价于 `.\stop.ps1`）。

## 6. 功能使用说明

### 6.1 游客

1. 打开首页自动进入商品列表（5 条测试商品）。
2. 搜索：输入商品名称关键词（如「耳机」「英语」），点击搜索；空关键词显示全部。
3. 排序：下拉选择「价格从低到高 / 从高到低」；默认按发布时间倒序。
4. 分页：底部分页可切换页码与每页条数（10/20/50）。
5. 详情：点击商品卡片进入详情，查看价格、联系方式、发布者 ID、发布时间。
6. 搜索/排序/分页状态会同步到 URL，刷新或分享链接后状态保留。

### 6.2 注册与登录

1. 右上角「注册」：用户名 3–20 位字母/数字/下划线，密码 6–20 位且不含空白；注册成功不自动登录。
2. 「登录」：输入用户名/密码；失败统一提示「用户名或密码错误」。
3. 登录成功后右上角显示用户名与角色；刷新页面会通过 `/api/auth/me` 恢复登录态。

### 6.3 普通用户

1. 发布：右上角「发布商品」→ 填写名称（1–100 字）、价格（>0 且 ≤999999.99，两位小数）、联系方式（1–100 字）→ 提交后进入详情页。
2. 修改：自己的商品详情页显示「编辑商品」→ 表单自动回填 → 修改后保存。
3. 删除：详情页「删除商品」→ 二次确认 → 成功后回到列表。
4. 普通用户看不到他人商品的编辑/删除按钮；即使伪造请求，后端也会返回 403。

### 6.4 管理员

1. 使用 `admin / 123456` 登录。
2. 任意商品详情页都会显示「编辑商品」「删除商品」；管理员可修改/删除所有用户发布的商品。
3. 管理员账号只能由 `sql/data.sql` 种子或人工 SQL 设置，注册接口永远创建普通用户。

### 6.5 登出

右上角「退出登录」：后端 Session 失效，前端清理本地状态并回登录页。

### 6.6 常见错误提示对照

| 场景 | HTTP | 业务码 | 提示 |
|---|---|---|---|
| 参数不合法 | 400 | 10001 | 参数校验失败（`data.fieldErrors` 指明字段） |
| 未登录/会话过期 | 401 | 10002 | 未登录或登录已过期 |
| 用户名或密码错误 | 401 | 10003 | 用户名或密码错误 |
| 越权修改/删除他人商品 | 403 | 10004 | 无权限执行该操作 |
| 商品不存在/已删除 | 404 | 10005 | 商品不存在或已删除 |
| 用户名重复 | 409 | 10006 | 用户名已存在 |
| 请求格式错误 | 400 | 10007 | 请求格式错误 |

## 7. 接口速查

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 否 | 注册普通用户 |
| POST | `/api/auth/login` | 否 | 登录并建立 Session |
| POST | `/api/auth/logout` | 是 | 登出 |
| GET | `/api/auth/me` | 是 | 当前登录用户 |
| GET | `/api/items` | 否 | 列表/搜索/排序/分页 |
| GET | `/api/items/{id}` | 否 | 商品详情 |
| POST | `/api/items` | 是 | 发布商品 |
| PUT | `/api/items/{id}` | 是 | 修改（本人或 ADMIN） |
| DELETE | `/api/items/{id}` | 是 | 删除（本人或 ADMIN） |

完整字段、示例与错误码见 [api-spec.md](api-spec.md)。

## 8. 重置演示数据

```powershell
# 仅开发/演示环境，会清空当前库
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\schema.sql"
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\data.sql"
```

重置后恢复：`admin`、`student` 两个账号 + 5 条商品（全部归属 student）。

## 9. 常见问题

| 现象 | 处理 |
|---|---|
| 后端启动报数据库连接失败 | 确认 MySQL 服务已启动、`campus_secondhand` 已初始化、`DB_USERNAME/DB_PASSWORD` 正确 |
| 前端请求 401、登录后又 401 | 用 5173 开发服务器 + Vite 代理；Axios `withCredentials: true`；后端重启会丢失 Session 属预期 |
| 端口 8080/5173 被占用 | 关闭旧进程（`Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess`）或改端口并同步文档 |
| 中文乱码 | 数据库/表使用 `utf8mb4`；执行 SQL 时加 `--default-character-set=utf8mb4` |
| `npm install` 慢 | 切换国内镜像或设置代理；CI/生产环境不提交 `node_modules` |
| 前端构建提示 chunk > 500 kB | Element Plus 全量引入的提示，不是错误，不影响运行 |
| 刷新后 404 | 单 Jar 部署时确认前端使用 hash 路由；开发期访问 5173 而不是直接打开 dist/index.html |
| Session 重启后丢失 | 当前 Session 存 Tomcat 内存，演示前避免重启后端；生产可改持久化 Session/Redis |

## 10. 演示前检查清单

- [ ] MySQL 已启动，`campus_secondhand` 已初始化为 2 账号 + 5 商品。
- [ ] 后端已启动，`http://localhost:8080/api/items` 返回 JSON。
- [ ] 前端已启动（5173）或单 Jar 已启动（8080）。
- [ ] 浏览器已清除旧登录态（或确认当前角色符合演示顺序）。
- [ ] 测试账号 `admin/123456`、`student/123456` 可登录。
- [ ] 演示用浏览器缩放 100%，关闭无关标签页/弹窗。
- [ ] 准备好备份：预构建 Jar、14 张截图（`screenshots/`）。
- [ ] 演示中如需修改数据，使用临时商品并在结束后删除，保持种子 5 条不变。

## 11. 相关文档

- [需求规格](requirements.md) ｜ [系统架构](architecture.md) ｜ [API 规范](api-spec.md)
- [数据库设计](database-design.md) ｜ [分工计划](team-assignments.md) ｜ [开发流程](development-workflow.md)
- [设计评审与验收](design-review.md) ｜ [答辩讲解与问答](presentation-and-qa.md)
