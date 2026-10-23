# 校园二手交易系统 — 开发协作流程（精简版）

> 文档编号：WF-001 · 版本：v1.1（精简版） · 状态：评审中 · 维护人：A · 最后更新：2026-10-23
> 关联文档：[分工计划](team-assignments.md) · [需求规格](requirements.md) · [系统架构](architecture.md) · [API 契约](api-spec.md) · [数据库设计](database-design.md)

## 1. 分支与仓库

- 仓库：https://github.com/SkyLoveFree/campus-secondhand（公开，默认 `main`）。
- `main` 为可运行分支，**禁止直接 push / force push**；功能分支 `feature/<成员>-<模块>`（如 `feature/d-item-api`、`feature/b-item-list`、`feature/c-db-init`），修复 `fix/<简述>`，文档 `docs/<主题>`。
- 流程：最新 `main` 切出 → 小步提交 → 自测 → rebase `main` → PR → A 合并（Merge Commit）→ 删除分支。

## 2. Commit 规范

- 格式：`<type>(<scope>): <中文简述>`；type 用 `feat`/`fix`/`docs`/`test`/`chore`/`refactor`，scope 用 `auth`/`item`/`web`/`db`/`api`/`common`。
- 示例：
  - `feat(auth): 实现登录接口并建立 Session`
  - `feat(item): 完成商品列表分页与价格排序`
  - `feat(web): 完成商品列表页搜索与分页`
  - `chore(db): 增加建表与种子数据脚本`
  - `docs(api): 补充分页参数与错误码说明`
- 小步提交：一个提交一个目标；每人保留自己的提交，禁止把他人工作合成一个提交；提交前 `git status` / `git diff` 检查，不提交 `target/`、`node_modules/`、`dist/`、本地配置。

## 3. PR 与合并

- 标题：`[任务编号] type(scope): 简述`（如 `[D3] feat(item): 商品列表查询接口`）；描述写变更内容、影响范围、验证方式（命令与结果）。
- Reviewer：**A + 1 名相关成员**；B/C/D 不得自行合并自己的 PR。
- 检查清单（合并前 5 项）：
  - [ ] 构建通过（后端 `mvn clean package` / `mvn test`、前端 `npm run build`）
  - [ ] 无调试代码、无敏感信息、无构建产物
  - [ ] 文档已同步（契约 / 表结构 / README 受影响部分）
  - [ ] 未破坏既有接口与公共契约
  - [ ] 提交按功能拆分、作者为本人
- 合并：**必须 Merge Commit，禁止 Squash**（保留每人提交记录）；合并后删除功能分支。

## 4. 冲突处理

- 先 `git fetch origin && git rebase origin/main`；解决冲突后测试通过，再 `git push --force-with-lease`（仅自己的功能分支）。
- 「谁负责谁定稿」：`pom.xml`/`application.yml`→D，`request.js`/`router`/`vite.config.js`→B，`sql/schema.sql`、`sql/data.sql`→C，`docs/`、`AGENTS.md`、`README.md`→A；公共文件改动前先在群里同步。
- 不得整文件丢弃他人代码；冲突无法达成一致时保留现场并报 A 裁决。

## 5. API / 数据库变更（5 步）

1. 提出：说明变更内容、原因、影响范围、替代方案。
2. A 确认；表结构变更须 C+D+A 三方确认。
3. **先改文档**：[api-spec.md](api-spec.md) 或 [database-design.md](database-design.md)。
4. 再改代码 / 脚本，并通知 B/C/D 同步。
5. PR 关联文档变更，A 验收「文档—代码—脚本」一致后合并。

> 禁止先改代码后补文档；接口以 [api-spec.md](api-spec.md) 为唯一契约。

## 6. 联调顺序与阶段出口

| 阶段 | 联调内容 | 出口条件 |
|---|---|---|
| M1 设计冻结 | 契约 / 表结构 / 字段评审 | 接口与字段一致，确认事项已有结论 |
| M2 骨架与认证 | C 脚本 → D 骨架 / 认证（F-01～F-04）→ B 请求层 / 登录页 | 登录态联调通过 |
| M3 商品功能 | D 查询接口（F-05～F-07）↔ B 列表 / 详情 | F-05～F-07 验收通过 |
| M4 联调验收 | D 写接口 + 权限（F-08～F-10）↔ B 表单 / 删除 | 401/403/404 正确，无 P0 缺陷 |
| M5 交付 | 单 Jar 部署 + 冒烟 | 冒烟全过，A 验收签字 |

- 无 Mock 框架时：B 先按契约写请求函数与静态页面（本地假数据不提交），D 宣布接口可联调（自测通过、错误码一致）后再接真实接口。

## 7. 构建与部署

- 开发：后端 `mvn spring-boot:run`（8080）；前端 `npm run dev`（5173，Vite 代理 `/api` → 8080，Cookie 同源）。
- 交付：单 Jar 同域部署，在仓库根目录执行（PowerShell）：

```powershell
# 初始化数据库（PowerShell 不支持 < 重定向，用 cmd /c 包一层）
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\schema.sql"
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\data.sql"
# 构建前端并把 dist 复制到后端 static
cd frontend; npm install; npm run build
$static = "..\backend\src\main\resources\static"
New-Item -ItemType Directory -Force $static | Out-Null
Remove-Item "$static\*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item ".\dist\*" -Destination $static -Recurse -Force
# 打包后端并启动单 Jar
cd ..\backend; $env:DB_USERNAME="root"; $env:DB_PASSWORD="<本机密码>"
.\mvnw.cmd -q clean package
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1
java -jar $jar.FullName
# 浏览器访问 http://localhost:8080
```

- 部署前确认：数据库已初始化、环境变量已设置、`static/` 已更新；`dist/`、`target/` 等构建产物不入库 🟡。

## 8. 验收与交付（6 条）

1. `main` 构建通过：后端 `mvn clean package`（含 `mvn test`）、前端 `npm run build`。
2. 冒烟全过：首页、登录、刷新保持、游客浏览、搜索、排序、分页、发布、修改、删除、越权 403、参数 400、未登录 401、404、中文无乱码。
3. README 完整：环境、数据库初始化、前后端启动、单 Jar 部署、测试账号（admin / student，仅演示）、常见问题。
4. 仓库无敏感信息，`.gitignore` 覆盖 `target/`、`node_modules/`、`dist/`、本地配置；每人提交记录可追溯。
5. 在验收通过的 `main` commit 打 tag `v1.0`；按课程要求打包 ZIP（命名以课程要求为准）。
6. A 在验收记录上签字确认后交付。

## 9. 常见问题

| 现象 | 处理 |
|---|---|
| 刷新 / 后续请求 401 | 统一用 5173 + Vite 代理；Axios `withCredentials: true`；后端重启会丢 Session 属预期 |
| 中文乱码 | 表与连接统一 `utf8mb4`；执行脚本加 `--default-character-set=utf8mb4` |
| 端口被占用 | `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force`，或改端口并同步文档 |
| 数据库连接失败 | 检查 `DB_USERNAME` / `DB_PASSWORD` 环境变量与 MySQL 服务；禁止硬编码密码 |

---
> 相关：[分工计划](team-assignments.md) · [需求规格](requirements.md) · [系统架构](architecture.md) · [API 契约](api-spec.md) · [数据库设计](database-design.md)
