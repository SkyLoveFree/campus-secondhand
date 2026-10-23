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

## 8. 测试数据（`sql/data.sql`，仅本地测试，生产禁用）

| 账号 | 角色 | user.id | 测试口令 | password_hash |
|---|---|---|---|---|
| `admin` | ADMIN | 1 | `123456` | `$2b$10$5qMnxPnrvW8Sw0bqJ6hhzOeDYu2B19t1sMomx4CbfMpgOwXpSi.5q` |
| `student` | USER | 2 | `123456` | 同上（复用同一哈希仅为测试简化） |

- BCrypt cost 10（`$2b$` 前缀，Spring Security `BCryptPasswordEncoder` 支持）；D 实现登录时用 `matches("123456", hash)` 复核；生产禁用。
- 商品：id 1–5，全部归属 student（user_id=2），价格 280.00 / 25.50 / 39.90 / 89.00 / 15.00，用于验证默认列表与价格排序。

## 9. 与 API 契约一致性
- 字段按 §5 映射，与 api-spec v1.1 核对一致；`password` 只入不出，响应无密码字段；未发现与 architecture/requirements 冲突。
- 长度/必填：`item`、`contactInfo` 各 100，价格精度与上限 `(0, 999999.99]` 与 API 一致；`username` 列宽 50（业务 3–20 由后端校验）；分页 `LIMIT` 由 Service 传 offset/size（size≤50 后端校验），响应 `{list,total,page,size}` 由 `COUNT(*)` + 分页查询支撑。

## 10. 默认方案（按此执行，有异议再改）
- 表名保持 `user`（不改 `app_user`）；`role` 用 VARCHAR(16)；本期物理删除，不加逻辑删除、不加 `description`、不加空白字符串 CHECK。
- MySQL 下限 8.0.16+（CHECK 生效）；测试账号/口令仅本地使用，按 2026-10-23 确认写入 README 并注明仅演示环境。

## 11. 变更记录
2026-10-23：v1.0-draft（C，详细版）→ v1.1 精简版（C，按课程定位压缩文档与脚本注释，表结构/口径不变）；A 已确认按当前方案执行；SQL 于 2026-10-23 实机验证通过（MySQL 8.4.11）。
