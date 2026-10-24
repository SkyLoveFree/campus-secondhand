# 校园二手交易系统 — 团队分工与任务分配（精简版）

> 文档编号：TEAM-001 · 版本：v1.1（精简版） · 状态：评审中 · 维护人：A · 最后更新：2026-10-23
> 关联文档：[需求规格](requirements.md) · [系统架构](architecture.md) · [API 契约](api-spec.md) · [数据库设计](database-design.md) · [协作流程](development-workflow.md)

## 1. 角色与决策权

| 成员 | 角色 | 职责 | 决策权 |
|---|---|---|---|
| **A** | 项目负责人 / 总体架构 | 需求、架构、API 契约、数据库设计审核、协调与 PR 评审、联调验收、README 与交付；**不承担具体业务编码** | **最终设计决策权**：契约、表结构、目录归属、验收结论由 A 定稿；B/C/D 可建议，不得擅自改公共设计 |
| **B** | 前端负责人 | Vue 3 + Vite + Element Plus 页面、请求层、表单校验、前后端联调、前端构建 | 前端内部实现；涉及契约 / 交互的决策提请 A |
| **C** | 数据库负责人 | 数据模型、表结构 / 索引 / 约束、`schema.sql` / `data.sql`、查询 SQL 评审 | 数据库设计与 SQL 口径；表结构变更须 C+D+A 确认 |
| **D** | 后端负责人 | Spring Boot 工程、统一响应 / 异常 / 校验、认证与 Session、商品接口与权限、Mapper、测试与打包 | 后端实现；接口契约以 [api-spec.md](api-spec.md) 为准，不擅自变更 |

**成员姓名 / 学号**：2026-10-23 确认暂不登记，需要时再补（影响个人贡献统计）。**分工边界**：

- **C / D**：C 负责表结构、索引约束、`schema.sql` / `data.sql`、数据库文档与 SQL 评审；D 负责 Java 业务逻辑与 Mapper/XML；**双方互不改对方目录**；表结构变更须 **C+D+A** 三方确认。
- **前后端**：B 以 [api-spec.md](api-spec.md) 为唯一接口契约，不自行设计接口 / 字段；D 不擅自改契约；字段名以 API 文档为准。
- **公共文件**：`pom.xml` / `application.yml` → D；`request.js` / `router` / `vite.config.js` → B；`sql/schema.sql`、`sql/data.sql` → C；`docs/`、`AGENTS.md`、`README.md`、`.gitignore` → A。修改前先同步归属人。

## 2. A 的任务（A1–A5）

| 编号 | 负责人 | 任务 | 交付物 | 验收要点 |
|---|---|---|---|---|
| A1 | A | 需求与范围管理 | `requirements.md`、确认结论记录 | F-01～F-11 均有可演示验收标准；歧义有结论 |
| A2 | A | 架构与 API 契约 | `architecture.md`、`api-spec.md` | 技术决策确定；9 个接口契约完整；B/D 确认无歧义 |
| A3 | A | 数据库设计审核与整合 | 审核意见、字段映射确认 | 表结构与字段映射无偏离；与 API 一致 |
| A4 | A | 协调、PR 评审与联调验收 | PR 记录、验收清单 | 代码 PR 经 A 合并，文档由 A 直接推送 `main`；P0 功能与越权测试全过 |
| A5 | A | README、交付材料与最终验收 | `README.md`、tag `v1.0`、交付 ZIP | 干净环境按 README 可启动；每人提交可追溯；A 签字 |

## 3. B（前端）任务（6 项）

| 编号 | 负责人 | 任务 | 交付物 | 前置依赖 | 验收要点（含测试） |
|---|---|---|---|---|---|
| B1 | B | 工程初始化与骨架 | `frontend/`：Vue 3 + Vite + Element Plus + Pinia + 路由（hash 模式）+ `/api` 代理 | A2 契约冻结 | `npm install && npm run dev` 启动 5173；代理生效 |
| B2 | B | 请求层与错误处理 | `utils/request.js`、`api/auth.js`、`api/item.js` | B1、api-spec | 9 个接口封装与契约一致；401/403/400/500 提示；无硬编码地址 |
| B3 | B | 路由、布局与登录态 | `router/`（hash 模式）、`stores/user.js`（Pinia）、`AppHeader.vue` | B1、B2 | 未登录跳登录；刷新经 `/me` 恢复；登出清理状态；hash 路由刷新不 404 |
| B4 | B | 商品列表页 | `ItemList.vue`、`ItemCard.vue`、`EmptyState.vue` | B2、B3、D3 | 搜索 / 价格排序 / 分页正确且同步 URL query；加载、空、错误三态；游客可访问；卡片只显示名称/价格/时间 |
| B5 | B | 详情页与删除交互 | `ItemDetail.vue`（含删除二次确认） | B4、D4 | 详情字段与列表一致；404 有提示；删除有确认；权限入口显隐正确 |
| B6 | B | 登录注册 / 发布编辑 / 构建 | `Login.vue`、`Register.vue`、`ItemForm.vue`；`npm run build` 通过 | B2、B3、D2、D4 | 注册登录可用；发布编辑校验正确；构建成功且可部署 |

## 4. C（数据库）任务（5 项）

| 编号 | 负责人 | 任务 | 交付物 | 前置依赖 | 验收要点（含测试） |
|---|---|---|---|---|---|
| C1 | C | 数据库设计文档 | `database-design.md`：ER、字段、约束、索引、字段映射、查询示例 | A2、A3 | 两表字段与 API 映射一致；约束 / 索引明确；评审通过 |
| C2 | C | 建表脚本 | `sql/schema.sql` | C1 | 干净 MySQL 可执行两遍（A 已于 2026-10-23 实机验证通过）；字符集 / 约束 / 索引与文档一致 |
| C3 | C | 种子数据 | `sql/data.sql`（admin / student + 3～5 条商品，BCrypt 哈希） | C2、D 提供哈希 | 可重复执行；无明文密码；测试账号可登录 |
| C4 | C | 查询 SQL 与索引评审 | `database-design.md` 查询样例 + `EXPLAIN` 记录 | C1、C2 | 列表 / 搜索 / 排序 / 分页 SQL 可用；索引取舍有说明 |
| C5 | C | Mapper SQL 评审与数据库交付验证 | PR 评审意见、数据库验证记录 | D3 / D4 提交、C2 | 无 `${}` 拼接；字段映射一致；按 README 初始化成功 |

## 5. D（后端）任务（6 项）

| 编号 | 负责人 | 任务 | 交付物 | 前置依赖 | 验收要点（含测试） |
|---|---|---|---|---|---|
| D1 | D | 工程与公共基础 | `pom.xml`、主类、`application.yml`、`Result` / 异常 / 校验、拦截器骨架 | A2、C2 字段 | `mvn clean package` 通过；错误码与统一响应符合契约 |
| D2 | D | 数据访问 + 认证模块 | 实体、`UserMapper` / `ItemMapper`、`AuthController` / `AuthService`（F-01～F-04） | D1、C2 | 登录 / 登出 / me 可用；401/409 正确；BCrypt 存储 |
| D3 | D | 商品查询接口 | 列表 / 搜索 / 排序 / 分页 / 详情（F-05～F-07）+ `ItemMapper.xml` | D2、C4 | 分页结构正确；排序与 LIKE 转义正确；游客可访问 |
| D4 | D | 商品写接口与权限 | 发布 / 修改 / 删除 + 归属校验（F-08～F-10） | D3 | 伪造 `userId` 无效；本人可改删；他人 403；管理员放行 |
| D5 | D | 测试与联调支持 | JUnit 5 + MockMvc 测试、联调问题记录 | D2～D4 | `mvn test` 通过；401/403/404/400 覆盖；联调问题闭环 |
| D6 | D | 打包部署 | 可运行 Jar、部署说明素材 | D1～D5 | `java -jar` 启动；单 Jar 部署后冒烟通过 |

## 6. 里程碑与出口条件

| 里程碑 | 主要交付 | 出口条件 |
|---|---|---|
| **M1 设计冻结** | 需求 / 架构 / API / 数据库设计 / 本文件 + 流程就绪 | 接口与字段一致；确认事项已有结论；SQL 已验证；仓库就绪（不启用分支保护） |
| **M2 骨架与认证** | C 脚本、D1/D2、B1～B3、B6 初版 | F-01～F-04 通过；登录态联调成功（Cookie / Session） |
| **M3 商品功能** | D3、B4/B5、C4/C5 评审 | F-05～F-07 通过；契约无未记录变更 |
| **M4 联调验收** | D4/D5、B5/B6 完成、越权回归 | F-08～F-11 通过；401/403/404 正确；无 P0 缺陷 |
| **M5 交付** | 单 Jar 部署、README、tag、ZIP | 冒烟清单全过；A 验收签字 |

> 当前进度（2026-10-23）：M1/M2 已完成；M3 已完成初版（D3 商品查询接口、B4/B5 列表/详情页，57 测试 + E2E 通过）；下一步 M4：发布/修改/删除接口 + 权限联调 + 全量回归。

## 7. 认领、变更与估算

1. **认领**：默认按角色（A*/B*/C*/D*），群内回复「认领 <编号>」并在 PR 标题带 `[编号]`；跨角色认领须 A 同意。
2. **变更**：任务范围 / 验收条件调整须 A 确认并更新本文件；契约变更走 [development-workflow.md](development-workflow.md)；表结构变更 C+D+A 确认。
3. **完成**：验收条件勾选 + PR 合并 + 文档同步才算完成；禁止代提交他人代码、禁止把他人工作合并成一个提交。
4. **工作量参考**：B≈10、C≈5.5、D≈12.5、A≈8 人天（参考估算，不含缓冲，按实际调整）。

## 8. 已确认事项（2026-10-23）

| # | 事项 | 确认结果 | 状态 |
|---|---|---|---|
| Q1 | 成员姓名 / 学号 / GitHub 账号 | 暂不登记，需要时再补 | ✅ 暂不处理 |
| Q2 | 课程截止日期与 M1～M5 具体排期 | 暂不写入文档，由 A 自行掌握 | ✅ 暂不处理 |
| Q3 | A 是否参与具体编码 | A **不写实际代码**，负责评审、整合与验收 | ✅ 已确认 |
| Q4 | 测试账号（admin / student，测试口令 123456）是否写入 README | 写入 README，注明仅演示环境使用 | ✅ 已确认 |

> 另：SQL 脚本已由 A 于 2026-10-23 在 MySQL 8.4.11 实机验证通过（schema/data 可重复执行、约束生效；代理复验一致），C 协助复核。

---
## 9. M4/M5 分工（2026-10-24）

> 当前状态：D 的写接口（faf837e）与 C 的 C4 文档（df07be8）已合并并通过 A 验收；B 的 M4 前端、C 的 C5 评审、D 的文案修复尚未完成。A 只做评审 / 整合 / 验收 / 文档，不写业务代码。

### 9.1 M4 任务分配

| 编号 | 负责人 | 分支 | 任务内容 | 交付 / 验收 | 前置依赖 |
|---|---|---|---|---|---|
| M4-D1 | D | `fix/d-contract-messages` | 将 10004/10005/10008/10500 的 message 对齐 api-spec 固定文案（无权限执行该操作 / 商品不存在或已删除 / 请求方法不支持 / 服务器内部错误，请稍后重试）；修正 ItemSaveRequest 过期注释；同步更新受影响测试 | `mvnw test` 全过；4xx JSON message 与 api-spec 一致；PR 合并 | 无（优先做） |
| M4-B1 | B | `feature/b-item-form` | ItemForm 发布/编辑：`/publish` 调 createItem、`/items/:id/edit` 调 getItem 回填 + updateItem；表单校验与 api-spec 一致；成功跳详情/列表，401 跳登录，403/404/400 提示 | 发布/编辑全流程通过；`npm run build` 通过 | D 写接口（已合并） |
| M4-B2 | B | `feature/b-item-actions` | 详情页删除（ElMessageBox 二次确认 → deleteItem → 成功返回列表）；列表/详情「编辑/删除」入口按本人或 ADMIN 显隐，游客不显示；403/404 提示 | F-09/F-10 前端流程通过；无 console error | M4-B1 |
| M4-B3 | B | `feature/b-m4-integration` | 与 D 联调发布/编辑/删除；补充 README 前端运行/构建说明；提交自测记录（角色 × 操作） | 三流程联调通过；build 通过；PR 合并 | M4-B1/B2 |
| M4-C1 | C | `feature/c-mapper-review` | C5：评审 `ItemMapper.xml` 写/查 SQL（insert/update/deleteById/selectList/countList/selectById）：参数化、user_id 不可更新、updated_at 刷新、影响行数 404、排序白名单 | 评审记录（database-design §6.2 或 PR comment）+ 结论；PR 合并 | D 代码（已合并） |
| M4-C2 | C | `feature/c-mapper-review` | 在干净 MySQL 重放 schema/data，手工验证「发布 → 修改 → 删除」数据正确（user_id 归属、updated_at、行数）；验证后恢复种子数据 | 命令/结果记录；2 用户 + 5 商品可恢复 | M4-C1 |
| M4-A1 | A | — | 验收已合并的 D 写接口与 C4 文档（已完成：E2E 23/23、77 测试、C4 记录） | 验收结论写入 design-review | — |
| M4-A2 | A | — | 合并 M4-D1/B1/B2/B3/C1/C2 的 PR；跑 M4 全量回归（9 接口 × 游客/USER/ADMIN）；更新文档并推送 | 回归通过；main 可构建；文档同步 | 上述任务 |
| M4-A3 | A | — | 决定错误文案方案（建议按 api-spec 对齐）、清理已合并分支 | 记录 + 合并 | M4-D1 |

**执行顺序**：M4-D1（文案修复）→ B1/B2 并行 + C1/C2 并行 → B3 联调 → A2 全量回归。每个分支提 PR 到 `main`，A 用 Merge Commit 合并（禁止 Squash），合并后删除分支；不得代提交他人代码。

### 9.2 M5 交付任务（提前认领）

| 编号 | 负责人 | 任务内容 | 交付 |
|---|---|---|---|
| M5-B | B | 前端最终构建、README 前端说明、UI 走查与截图 | `npm run build` 成功；README 前端章节；演示截图 |
| M5-C | C | 数据库初始化/重置验证记录、演示数据说明、SQL 脚本最终校验 | 验证记录；`database-design.md` 更新 |
| M5-D | D | 后端打包/单 Jar 启动说明、`mvnw test` 最终结果、部署故障排查 | 部署说明；Jar 可运行；测试记录 |
| M5-A | A | README 汇总、tag `v1.0`、交付 ZIP、总结报告/PPT、最终验收 | 交付包与验收签字 |

> [需求规格](requirements.md) · [系统架构](architecture.md) · [API 契约](api-spec.md) · [数据库设计](database-design.md) · [协作流程](development-workflow.md)
