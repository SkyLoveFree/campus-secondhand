# 校园二手交易系统 — 设计文档交叉评审记录（精简版）

> 文档编号：REV-001 ｜ 版本：v1.1（精简版）｜ 状态：评审完成（A 已确认 5 项、SQL 已实机验证，2026-10-23）｜ 最后更新：2026-10-23
> 评审整理：A（项目负责人）｜ 关联：[需求](requirements.md) · [架构](architecture.md) · [API](api-spec.md) · [数据库](database-design.md) · [分工](team-assignments.md) · [流程](development-workflow.md) · [AGENTS](../AGENTS.md)

## 1. 评审结论

- 8 份文档 + SQL 脚本齐备，交叉核对通过，**未发现硬冲突**。
- 已按「4 人课程大作业」定位整体精简：文档总量约 **5,216 行 / 369 KB → 约 1,060 行 / 75 KB**；删除安全扩展、流程教程与逐项决策清单。
- 安全按基础要求：BCrypt 密码、MyBatis 参数化、后端权限校验、不提交密码/密钥；不做 CSRF、HTTPS、限流等额外加固。
- A 已于 2026-10-23 确认 5 项事务性事项（见 §4）：A 不写实际代码、SQL 由 A 实机验证、测试账号写入 README；成员信息与排期暂不处理。另确认前端 4 项细化决策（§4 Q6～Q9：Pinia、hash 路由、URL query 同步、列表不展示联系方式）。SQL 已实机验证通过（MySQL 8.4.11，代理复验一致）。M1 可冻结，剩余 git/README/Maven 为施工准备。

## 2. 交付清单（v1.1 精简版）

| 文档 | 编号 | 行数 | 维护人 |
|---|---|---|---|
| [AGENTS.md](../AGENTS.md) | — | 150 | A |
| [requirements.md](requirements.md) | REQ-001 | 118 | A |
| [architecture.md](architecture.md) | ARC-001 | 122 | A |
| [api-spec.md](api-spec.md) | API-001 | 187 | A |
| [database-design.md](database-design.md) | DB-001 | 118 | C（A 审核） |
| [team-assignments.md](team-assignments.md) | TEAM-001 | 92 | A |
| [development-workflow.md](development-workflow.md) | WF-001 | 108 | A |
| 本文件 | REV-001 | 79 | A |
| `sql/schema.sql`、`sql/data.sql` | — | 56 / 31 | C |

合计约 **1,060 行 / 75 KB**（原 5,216 行 / 369 KB）。

## 3. 交叉核对结果（任务书 9 项）

| # | 检查项 | 结论 | 说明 |
|---|---|---|---|
| 1 | API 字段 ↔ 数据库字段 | ✅ | `itemPrice↔item_price`、`contactInfo↔contact_info`、`userId↔user_id`、`createdAt↔created_at`、`updatedAt↔updated_at`；API 无 snake_case；`password` 只入不出 |
| 2 | 前端请求 ↔ 后端接口 | ✅ | 9 个接口；B1～B6 任务全部以 api-spec 为唯一契约，无自造接口；字段 camelCase 一致 |
| 3 | 登录状态与角色 | ✅ | Session + Cookie；白名单 4 条；401/403/404 语义、30 分钟超时、`/me` 恢复登录态一致 |
| 4 | 商品修改/删除归属 | ✅ | 判定顺序「查商品（404）→ ADMIN → 本人 → 403」在需求/架构/API/数据库/AGENTS/流程中一致；请求 `userId` 忽略 |
| 5 | 成员任务边界 | ✅ | 22 项任务（A5+B6+C5+D6）均含负责人/交付物/依赖/验收；C/D 互不改目录；前后端以 api-spec 为准 |
| 6 | 任务验收条件 | ✅ | 每项任务有可验证验收要点；M1～M5 出口条件明确；无「完成开发」式空泛验收 |
| 7 | AGENTS 与详细文档 | ✅ | 技术栈、响应/错误码、字段命名、Session/权限、目录归属、命令、Git 规则一致；AGENTS 为规则入口，细节链接到 docs |
| 8 | 未决事项未被写成已确认 | ✅ | §4 已确认 5 项，无未决阻塞项；其余 🟡 方案统一按建议执行，已注明有异议再改 |
| 9 | 课程规模合理性 | ✅ | 2 张表、9 个接口、单体 Spring Boot + Vue；无 Redis/Docker/微服务/CI 门槛；文档已精简到可快速通读 |

## 4. A 已确认事项（2026-10-23）

| # | 事项 | 确认结果 |
|---|---|---|
| Q1 | 成员姓名 / 学号 / GitHub 账号 | 暂不登记，需要时再补 |
| Q2 | 课程截止日期与 M1～M5 排期 | 暂不写入文档，由 A 自行掌握 |
| Q3 | A 是否参与具体编码 | ✅ A **不写实际代码**，负责评审、整合与验收 |
| Q4 | SQL 实机验证 | ✅ 已由 A 于 2026-10-23 在 MySQL 8.4.11 实机验证通过（schema/data 可重复执行、约束生效；代理复验一致） |
| Q5 | 测试账号（admin/student，口令 123456）是否写入 README | ✅ 写入 README，注明仅演示环境使用 |
| Q6 | 前端状态管理 | ✅ 引入 Pinia（仅管理用户/列表状态，不引入其他状态库） |
| Q7 | 前端路由模式 | ✅ hash 模式（单 Jar 刷新不 404，无需后端 fallback） |
| Q8 | 列表状态同步 URL query | ✅ 做（`route.query` 为单一数据源，默认值省略，`router.replace` 更新） |
| Q9 | 列表卡片是否展示联系方式 | ✅ 不展示（只展示名称/价格/时间），详情页展示；列表接口仍返回 `contactInfo` |

> 其余设计细节（Session、MyBatis、分页、响应格式、字段命名等）已统一按建议执行，**不再逐项要求确认**；如不同意随时提出修改。

## 5. 本次精简与修复记录

- **精简**：需求/架构保留范围、权限、分层与关键约定；API 保留 9 接口契约 + 错误码 + 关键示例；数据库保留表结构、映射、SQL 要点与种子数据；分工/流程压缩为表格；删除安全扩展、边界场景长展开、大段检查清单、Git 教程、32 项决策表。
- **修复**：统一版本为 v1.1（精简版）；修正 database-design 中旧的章节/版本引用；数据库初始化命令改为 PowerShell 可执行的 `cmd /c` 写法；启动 Jar 命令改为变量写法；重写本评审记录；统一 MySQL 8.0.16+ 口径与权限矩阵表述。
- **已完成**：SQL 于 2026-10-23 在 MySQL 8.4.11 实机验证通过（`schema.sql` + `data.sql` 连续执行两次成功，表/索引/约束/种子数据核验一致；代理复验结果相同）。

## 6. 风险与 M1 出口清单

- 主要风险：Session/Cookie 联调（M2 第一优先打通登录态）；本机未安装 Maven；README、`.gitignore`、Git 仓库尚未初始化（SQL 已验证通过）。
- M1 出口（进入 M2 前完成）：
  - [x] A 已确认 §4 五项（2026-10-23）。
  - [x] A 已在 MySQL 8.4.11 实机验证通过（2026-10-23；`schema.sql` + `data.sql` 可重复执行、约束生效）。
  - [ ] D 安装 Maven 3.9+ 或提交 Maven Wrapper。
  - [ ] A 完成 `git init` + 远端 + `.gitignore` + README 骨架（含测试账号与启动步骤）+ GitHub 分支保护。
  - [ ] 全员确认任务与契约，设计冻结，进入 M2：C 建库 → D 骨架 + 认证 → B 骨架 + 请求层 → 登录态联调。

## 7. 变更记录

| 日期 | 版本 | 修改人 | 内容 |
|---|---|---|---|
| 2026-10-23 | v1.1 | A | 精简版：压缩至约 1,060 行；待确认收敛为 5 项；安全按基础要求；重写评审结论与 M1 出口清单；同日 A 确认 5 项（见 §4）；SQL 实机验证通过（代理复验一致）；确认前端 4 项（Pinia/hash/URL 同步/列表不展示联系方式，Q6～Q9） |
