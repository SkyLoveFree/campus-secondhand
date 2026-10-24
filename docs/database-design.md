# 校园二手交易系统 — 数据库设计文档（精简版）

> 文档编号：DB-001 ｜ 版本：v1.1（精简版）｜ 状态：评审中（🟡 按当前方案执行，有异议再改）｜ 维护人：C（A 审核）｜ 最后更新：2026-10-23
> 上游：[需求](requirements.md) · [架构](architecture.md) ｜ 关联：[API](api-spec.md) · [AGENTS](../AGENTS.md) ｜ 脚本：[schema.sql](../sql/schema.sql) · [data.sql](../sql/data.sql)

## 1. 规范与 ER
MySQL 8.0.16+（本机 8.4.11）；InnoDB；`utf8mb4` / `utf8mb4_0900_ai_ci`；表/列 snake_case；主键 `id` BIGINT 自增；时间 `DATETIME`；金额 `DECIMAL(10,2)`；每列 `COMMENT`。游客不落库。

```text
user 1 ── N item：user.id ← item.user_id（fk_item_user，ON DELETE RESTRICT）
```
## 2. 表结构
### 2.1 user（用户表）

| 列名 | 类型 | 可空 | 默认值 | 键/约束 | 注释 |
|---|---|---|---|---|---|
| `id` | BIGINT | 否 | 自增 | PK | 用户ID |
| `username` | VARCHAR(50) | 否 | — | UNIQUE `uk_user_username`（大小写不敏感） | 登录用户名 |
| `password_hash` | VARCHAR(100) | 否 | — | — | BCrypt 哈希（cost 10），禁止明文，接口不返回 |
| `role` | VARCHAR(16) | 否 | `'USER'` | CHECK `chk_user_role`：USER/ADMIN | 角色 |
| `created_at` | DATETIME | 否 | CURRENT_TIMESTAMP | — | 创建时间（Asia/Shanghai） |
| `updated_at` | DATETIME | 否 | CURRENT_TIMESTAMP，ON UPDATE | — | 更新时间 |

### 2.2 item（商品表）

| 列名 | 类型 | 可空 | 默认值 | 键/约束 | 注释 |
|---|---|---|---|---|---|
| `id` | BIGINT | 否 | 自增 | PK | 商品ID |
| `item` | VARCHAR(100) | 否 | — | — | 商品名称（模糊搜索字段），1–100 字符 |
| `item_price` | DECIMAL(10,2) | 否 | — | CHECK `chk_item_price_range`：>0 且 ≤999999.99 | 价格（元） |
| `contact_info` | VARCHAR(100) | 否 | — | — | 联系方式，1–100 字符 |
| `user_id` | BIGINT | 否 | — | FK `fk_item_user`→`user.id` ON DELETE RESTRICT；索引 `idx_item_user_id` | 发布者（后端 Session 写入） |
| `created_at` | DATETIME | 否 | CURRENT_TIMESTAMP | 索引 `idx_item_created_at` | 发布时间（默认排序） |
| `updated_at` | DATETIME | 否 | CURRENT_TIMESTAMP，ON UPDATE | — | 更新时间 |

## 3. 索引

| 索引 | 列 | 用途 |
|---|---|---|
| `uk_user_username` | user.username | 唯一；登录、注册查重 |
| `idx_item_user_id` | item.user_id | 外键与按发布者查询 |
| `idx_item_created_at` | item.created_at | 默认排序 `created_at DESC, id DESC` |
| `idx_item_price` | item.item_price | 价格排序（可选，可裁剪） |

搜索 `LIKE '%kw%'` 无法使用普通索引，课程数据量（≤1 万）全表扫描可接受，不引入全文索引/ES。

## 4. 约束与删除规则
- 主键/非空/唯一：两表 `id` BIGINT 自增主键，业务列全部 `NOT NULL`，`username` 唯一。
- CHECK：角色仅 `USER`/`ADMIN`；价格 `>0 且 ≤999999.99`（MySQL 8.0.16+ 生效）。
- 删除：商品物理删除（不可恢复）；用户本期不提供删除；外键 `RESTRICT` 不级联，删有商品的用户会被拒绝。

## 5. 字段与 API JSON 映射（与 api-spec 一致）

| 业务含义 | API JSON | 数据库列 | 表 | 说明 |
|---|---|---|---|---|
| 用户 ID | `id` | `id` | user | 响应返回，不可客户端指定 |
| 用户名 | `username` | `username` | user | 唯一 |
| 密码（仅请求） | `password` | —（存 `password_hash`） | user | 只入不出 |
| 角色 | `role` | `role` | user | USER/ADMIN |
| 商品 ID | `id` | `id` | item | 自增主键 |
| 商品名称 | `item` | `item` | item | 模糊搜索字段 |
| 商品价格 | `itemPrice` | `item_price` | item | DECIMAL(10,2) |
| 联系方式 | `contactInfo` | `contact_info` | item | 长度 100 |
| 发布者 ID | `userId` | `user_id` | item | 后端 Session 写入 |
| 创建时间 | `createdAt` | `created_at` | 两表 | `yyyy-MM-dd HH:mm:ss` |
| 更新时间 | `updatedAt` | `updated_at` | 两表 | 修改时刷新 |

## 6. 查询要点（MyBatis `#{}` 参数化，禁止 `${}`）

```sql
-- 列表：COUNT(*) 与列表用同一 WHERE；offset=(page-1)*size
SELECT id, item, item_price, contact_info, user_id, created_at, updated_at FROM item ORDER BY created_at DESC, id DESC LIMIT #{offset}, #{size};
-- 搜索：Java 层先转义 \、%、_，再 #{} 绑定；ESCAPE '\\'
SELECT id, item, item_price, contact_info, user_id, created_at, updated_at FROM item WHERE item LIKE CONCAT('%', #{escapedKeyword}, '%') ESCAPE '\\' ORDER BY created_at DESC, id DESC LIMIT #{offset}, #{size};
-- 价格排序：ORDER BY item_price ASC, id DESC / item_price DESC, id DESC（ORDER BY 白名单映射）
-- 详情 / 发布 / 修改 / 删除
SELECT id, item, item_price, contact_info, user_id, created_at, updated_at FROM item WHERE id = #{id};
INSERT INTO item (item, item_price, contact_info, user_id) VALUES (#{item}, #{itemPrice}, #{contactInfo}, #{currentUserId});
UPDATE item SET item = #{item}, item_price = #{itemPrice}, contact_info = #{contactInfo}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id};
DELETE FROM item WHERE id = #{id};
```

分页 `page≥1`、`1≤size≤50`（默认 1/10）；UPDATE/DELETE 影响行数为 0 时按 404 处理。

## 6.1 EXPLAIN 实机验证记录（C4）

验证环境：MySQL 8.0.46（C 本机，Windows），库 `campus_secondhand`；分别在 5 行种子数据与 10005 行压测数据下执行（压测后已重建干净库，恢复 2 用户 + 5 商品）。验证人：C，日期：2026-10-23。

| 查询 | 5 行 | 10005 行 | 结论 |
|---|---|---|---|
| `ORDER BY created_at DESC, id DESC LIMIT 10` | ALL + filesort | `idx_item_created_at`，Backward index scan，rows=10 | 索引生效 |
| `ORDER BY item_price DESC, id DESC LIMIT 10` | — | `idx_item_price`，Backward index scan，rows=10 | 索引生效 |
| `ORDER BY item_price ASC, id DESC LIMIT 10` | ALL + filesort | ALL + filesort（FORCE INDEX 与覆盖索引实测均无效） | 已知取舍 |
| `LIKE '%书%'` + 默认排序 | index 扫描 | 借 `idx_item_created_at` 免 filesort + WHERE 过滤 | 符合预期 |
| `WHERE username='student'` | const，`uk_user_username`，rows=1 | 同左 | 唯一索引生效 |

说明：
1. 小数据量（5 行）下优化器选全表扫描属正常行为，不代表索引失效。
2. 价格升序为混合方向排序（`item_price ASC` + `id DESC`），MySQL 索引无法直接支持；万级数据量下 filesort 为毫秒级（LIMIT 优先队列排序），课程规模可接受，不额外加索引。
3. 同向全 DESC 排序（默认列表、价格降序）可利用索引反向扫描早退，分页首页仅读取 size 行。

## 6.2 Mapper SQL 评审记录（C5 / M4-C1）

评审对象：`backend/src/main/resources/mapper/ItemMapper.xml`（D3 查询 + D4 写操作，随 PR #2 合并，提交 `faf837e`）。评审人：C，日期：2026-10-24。评审方式：逐条核对 XML 与 `ItemServiceImpl` 调用语义。

| 检查项 | 结论 | 依据 |
|---|---|---|
| 参数化（禁 `${}`） | ✅ 通过 | `insert`/`update`/`deleteById`/`selectById`/`selectList`/`countList` 全部 `#{}` |
| user_id 不可更新 | ✅ 通过 | UPDATE 的 SET 仅 item/item_price/contact_info/updated_at；insert 的 userId 由 Service 从 Session 写入（`createItem` 忽略请求体 userId） |
| updated_at 刷新 | ✅ 通过 | UPDATE 显式 `updated_at = CURRENT_TIMESTAMP`，不依赖 ON UPDATE 隐式行为 |
| 影响行数 0 → 404 | ✅ 通过 | Service `updateItem`/`deleteItem` 判断影响行数为 0 抛 NOT_FOUND |
| 排序白名单 | ✅ 通过 | XML `<choose>` 固定映射 3 种 ORDER BY；Service `ALLOWED_SORTS` 二次校验，非法 sort 抛 400（C5 初审提醒项已落实） |
| COUNT 与列表同 WHERE | ✅ 通过 | 共用 `<sql id="searchCondition">` 片段 |
| LIKE 转义 | ✅ 通过 | Java 层转义 `\`、`%`、`_`，SQL 端固定 `ESCAPE '\\'`，无重复/遗漏 |
| 权限与归属判定 | ✅ 通过 | 先 selectById 判 404，再比对归属（ADMIN 放行，非本人 403）；不信任请求体 userId |

已知提醒（非阻塞，不属 SQL 层）：
1. 404/403 的 message 与 api-spec 固定文案存在差异（"商品不存在"/"只能操作自己发布的商品" vs "商品不存在或已删除"/"无权限执行该操作"），已由 M4-D1（`fix/d-contract-messages`）跟进对齐。
2. UPDATE/DELETE 的 WHERE 仅按 `id`，未附加 `user_id` 条件：因 user_id 永不变更、Service 已先行权限校验且写操作 `@Transactional`，无竞态风险，无需修改。

**结论：ItemMapper.xml 写/查 SQL 评审通过（C5 / M4-C1 完成）。**

## 6.3 写操作实机验证记录（M4-C2）

验证环境：MySQL 8.0.46（C 本机），库 `campus_secondhand`；先重放 `schema.sql` + `data.sql` 恢复干净种子，再按 Mapper 同款 SQL 手工执行「发布 → 修改 → 删除」，最后重放 `data.sql` 恢复种子。验证人：C，日期：2026-10-24。

| 步骤 | 操作 | 结果 | 判定 |
|---|---|---|---|
| 1 | 重放后初始状态 | users=2、items=5 | ✅ 干净种子 |
| 2 | 发布：INSERT（user_id=2，模拟 Session 归属） | 影响行数 1；新 id=6；created_at=updated_at 非空 | ✅ |
| 3 | 修改：UPDATE（不含 user_id，显式 `updated_at=CURRENT_TIMESTAMP`） | 影响行数 1；user_id 仍为 2 未变；updated_at(14:51:44) > created_at(14:51:43) | ✅ 归属不变、时间刷新 |
| 4 | 修改不存在 id=999999 | 影响行数 0 | ✅ 对应 Service 404 分支 |
| 5 | 删除 id=6；再重复删除 | 影响行数 1；重复删除 0 | ✅ 首次删除成功、重复删除对应 404 |
| 6 | 重放 `data.sql` 恢复 | users=2、items=5（id 1–5，均归属 student） | ✅ 种子可恢复 |

**结论：写操作数据正确性实机验证通过（M4-C2 完成），数据库已恢复干净种子状态。**

## 7. 初始化与验证
执行顺序：`sql/schema.sql`（建库、删表重建）→ `sql/data.sql`（先删后插，可重复执行）。脚本 UTF-8 无 BOM。

```bat
:: cmd（仓库根目录）
mysql -u<user> -p --default-character-set=utf8mb4 < sql\schema.sql
mysql -u<user> -p --default-character-set=utf8mb4 < sql\data.sql
:: PowerShell 不支持 < 重定向，改为：cmd /c "mysql -u<user> -p --default-character-set=utf8mb4 < sql\schema.sql"（data 同理）
```

验证：`SHOW TABLES;`（2 表）、`SHOW INDEX FROM item;`（4 索引）、`SELECT id, username, role FROM user;`、`SELECT COUNT(*) FROM item;`（5）。重置：`DROP DATABASE campus_secondhand;` 后重跑两个脚本（仅开发/演示环境）。

实机验证（2026-10-23 通过，MySQL 8.4.11）：`schema.sql` + `data.sql` 连续执行两次均成功，重复执行后 users=2、items=5；表 2 张（InnoDB / utf8mb4_0900_ai_ci）；索引 user=PRIMARY+uk_user_username、item=PRIMARY+idx_item_user_id+idx_item_created_at+idx_item_price；重复用户名（含大小写变体）、价格 0/超上限、非法角色、非法外键均被拒绝，合法插入/删除正常。

### 7.1 M5 最终校验记录（M5-C，交付前最终验证）

验证环境：MySQL 8.0.46（C 本机），日期：2026-10-24。流程：`DROP DATABASE campus_secondhand` → 按 README 命令重放 `schema.sql` → `data.sql` → 校验 → 二次执行 `data.sql` 验幂等。全部通过：

| 校验项 | 结果 | 判定 |
|---|---|---|
| 表清单 | `item`、`user` 共 2 张 | ✅ |
| 索引 | user=PRIMARY+uk_user_username（2）；item=PRIMARY+idx_item_user_id+idx_item_created_at+idx_item_price（4） | ✅ |
| 引擎/字符集 | 均 InnoDB / utf8mb4_0900_ai_ci | ✅ |
| 种子计数 | users=2、items=5 | ✅ |
| 演示账号 | admin(ADMIN)/student(USER)，哈希前缀均为 `$2b$10$`（BCrypt cost 10） | ✅ |
| CHECK 约束 | chk_item_price_range、chk_user_role 均已定义 | ✅ |
| 重复用户名 `STUDENT`（大小写变体） | ERROR 1062（uk_user_username）拒绝 | ✅ |
| 价格 =0 / =1000000 | ERROR 3819（chk_item_price_range）拒绝 | ✅ |
| 非法角色 `ROOT` | ERROR 3819（chk_user_role）拒绝 | ✅ |
| 非法外键 user_id=999 | ERROR 1452（fk_item_user）拒绝 | ✅ |
| `data.sql` 二次执行 | 仍 users=2、items=5，幂等 | ✅ |

**结论：SQL 脚本最终校验通过，干净环境按 README 可完整初始化，可重复执行。**

## 8. 测试数据（`sql/data.sql`，仅本地测试，生产禁用）

| 账号 | 角色 | user.id | 测试口令 | password_hash |
|---|---|---|---|---|
| `admin` | ADMIN | 1 | `123456` | `$2b$10$5qMnxPnrvW8Sw0bqJ6hhzOeDYu2B19t1sMomx4CbfMpgOwXpSi.5q` |
| `student` | USER | 2 | `123456` | 同上（复用同一哈希仅为测试简化） |

- BCrypt cost 10（`$2b$` 前缀，Spring Security `BCryptPasswordEncoder` 支持）；D 实现登录时用 `matches("123456", hash)` 复核；生产禁用。
- 商品：id 1–5，全部归属 student（user_id=2），价格 280.00 / 25.50 / 39.90 / 89.00 / 15.00，用于验证默认列表与价格排序。

### 8.1 演示数据说明（M5-C，演示场景对照）

| 演示场景 | 使用数据 | 说明 |
|---|---|---|
| 登录 / 登出 / 获取当前用户 | admin、student（口令 `123456`） | 覆盖 USER 与 ADMIN 两种角色 |
| 商品列表（默认排序） | 商品 id 1–5 | created_at 递减插入，默认 `created_at DESC, id DESC` 即 id 5→1 |
| 价格升序 / 降序 | 价格 15.00～280.00 互不相同 | 升序首条为「考研英语真题」，降序首条为「二手自行车」 |
| 名称模糊搜索 | 名称含「教材」「耳机」等常见词 | 搜索「书」命中 0 条（演示空态），搜「英语」「耳机」各命中 1 条 |
| 权限演示（改 / 删） | 商品全部归属 student | student 可改删全部 5 条；admin 演示管理员放行；如需演示 403，用 admin 之外的第二个 USER 注册新账号后操作他人商品 |
| 联系方式展示 | 微信/QQ/电话三种格式 | 覆盖 `contact_info` 常见写法 |

> 演示前重置：`DROP DATABASE campus_secondhand;` 后按 §7（README）重放两脚本即可回到上述状态。

## 9. 与 API 契约一致性
- 字段按 §5 映射，与 api-spec v1.1 核对一致；`password` 只入不出，响应无密码字段；未发现与 architecture/requirements 冲突。
- 长度/必填：`item`、`contactInfo` 各 100，价格精度与上限 `(0, 999999.99]` 与 API 一致；`username` 列宽 50（业务 3–20 由后端校验）；分页 `LIMIT` 由 Service 传 offset/size（size≤50 后端校验），响应 `{list,total,page,size}` 由 `COUNT(*)` + 分页查询支撑。

## 10. 默认方案（按此执行，有异议再改）
- 表名保持 `user`（不改 `app_user`）；`role` 用 VARCHAR(16)；本期物理删除，不加逻辑删除、不加 `description`、不加空白字符串 CHECK。
- MySQL 下限 8.0.16+（CHECK 生效）；测试账号/口令仅本地使用，按 2026-10-23 确认写入 README 并注明仅演示环境。

## 11. 变更记录
2026-10-23：v1.0-draft（C，详细版）→ v1.1 精简版（C，按课程定位压缩文档与脚本注释，表结构/口径不变）；A 已确认按当前方案执行；SQL 于 2026-10-23 实机验证通过（MySQL 8.4.11）。
2026-10-23：追加 §6.1 EXPLAIN 实机验证记录（C4，C 于本机 MySQL 8.0.46 完成；含 10005 行压测与"混合方向排序不走索引"的取舍说明）。
2026-10-24：追加 §6.2 Mapper SQL 评审记录（C5 / M4-C1，C 评审 D 写操作 PR #2 的 ItemMapper.xml，结论通过；含 2 条非阻塞提醒）；追加 §6.3 写操作实机验证记录（M4-C2，干净库重放后手工验证发布/修改/删除，通过并恢复种子）。
2026-10-24：追加 §7.1 M5 最终校验记录与 §8.1 演示数据说明（M5-C，C 于本机 MySQL 8.0.46 完成 DROP 重建 + 重放 + 约束抽查 + 幂等验证，全部通过）。
