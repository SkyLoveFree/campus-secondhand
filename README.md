# 校园二手交易系统（campus-secondhand）

面向校园的二手物品信息平台（JavaWeb 课程期末大作业，4 人小组）。用户注册登录后可发布二手商品（名称、价格、联系方式），所有人可浏览、按名称模糊搜索、按价格升序/降序排序；普通用户只能修改/删除自己发布的商品，管理员可修改/删除所有商品。

- 仓库：https://github.com/SkyLoveFree/campus-secondhand
- 设计文档入口：[AGENTS.md](AGENTS.md) ；需求/架构/API/数据库/分工/流程见 [`docs/`](docs/)

## 功能

- 用户：注册、登录、登出、获取当前用户
- 商品：列表（分页）、按名称模糊搜索、按价格排序、详情、发布、修改、删除
- 权限：游客/普通用户/管理员；后端强制校验，普通用户仅能改删自己的商品
- 技术栈：JDK 21、Spring Boot 3.3.x、MyBatis、MySQL 8.0.16+、Vue 3 + Vite + Pinia + Element Plus、GitHub

## 目录结构

```text
campus-secondhand/
├─ AGENTS.md                 # 项目规则入口
├─ README.md                 # 本文件
├─ docs/                     # 需求、架构、API、数据库、分工、流程、评审
├─ sql/                      # schema.sql（建表）、data.sql（测试数据）
├─ backend/                  # Spring Boot 后端（M2 起）
└─ frontend/                 # Vue 3 前端（M2 起）
```

## 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 21 |
| Maven | 3.9+（或使用仓库中的 Maven Wrapper `mvnw.cmd`） |
| Node.js | 20+（npm 10+） |
| MySQL | 8.0.16+（推荐 8.4） |

## 数据库初始化

```powershell
# PowerShell 不支持 < 重定向，用 cmd /c
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\schema.sql"
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\data.sql"
```

脚本会创建数据库 `campus_secondhand` 并重建 `user`、`item` 两张表；`data.sql` 可重复执行（先删后插）。

**测试账号（仅本地演示环境）**

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 ADMIN |
| `student` | `123456` | 普通用户 USER |

> 上述口令仅用于课程开发与演示，禁止用于任何真实环境；后端数据库密码通过环境变量注入，不要写入仓库。

## 启动

### 后端

```powershell
cd backend
$env:DB_USERNAME = "root"        # 按本机 MySQL 账号调整
$env:DB_PASSWORD = "<本机密码>"   # 不要提交到仓库
.\mvnw.cmd spring-boot:run       # 未使用 Wrapper 时：mvn spring-boot:run
```

后端默认 `http://localhost:8080`，接口统一前缀 `/api`。

### 前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认 `http://localhost:5173`，开发期 `/api` 由 Vite 代理到后端 8080（Cookie 同源）。前端路由使用 hash 模式。

前端目录结构（`frontend/src/`）：

| 目录/文件 | 职责 |
|---|---|
| `api/` | 接口封装（`auth.js` / `item.js`），页面唯一调用入口，不直接拼后端 URL |
| `utils/request.js` | axios 实例：`baseURL '/api'`、`withCredentials`；统一错误处理（401 跳登录、400 透出 `fieldErrors`、403 统一提示） |
| `stores/user.js` | Pinia 登录态；`canManage` 仅控制按钮显隐，权限由后端强制校验 |
| `router/` | hash 模式路由 + 登录守卫（启动时经 `/api/auth/me` 恢复登录态） |
| `views/` | Login / Register / ItemList / ItemDetail / ItemForm（发布与编辑共用，400 `fieldErrors` 逐字段回填） |
| `components/` | AppHeader / ItemCard |

## 构建与部署（单 Jar 同域）

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
# 浏览器访问 http://localhost:8080
```

## 接口文档

- 完整契约：[`docs/api-spec.md`](docs/api-spec.md)（9 个接口、错误码、示例）
- 表结构与种子数据：[`docs/database-design.md`](docs/database-design.md)

## 协作流程（简版）

- 分支：`main` 可运行、禁止直接 push；功能分支 `feature/<成员>-<模块>`，修复 `fix/<简述>`
- 提交：Conventional Commits + 中文说明，小步提交、每人保留自己的提交
- PR：A + 1 名相关成员评审；**Merge Commit，禁止 Squash**
- 文档/决策同步：A 确认决定或修改 `docs/`、`AGENTS.md` 后直接提交并 push `main`，队友及时 `git pull` 同步
- 详细规则：[`docs/development-workflow.md`](docs/development-workflow.md)

## 当前阶段

M4 收尾 / M5 准备（2026-10-24）：B 表单/删除/README、C Mapper 评审与写操作验证已合并并验收；D 的错误文案修复（PR #7 无实际改动）待重做；M5 单 Jar 部署冒烟已通过。D 修复合并后做最终全量回归、打 tag `v1.0`、生成交付 ZIP。
