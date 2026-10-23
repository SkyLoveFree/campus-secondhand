# 校园二手交易系统 — API 接口规范（精简版）

> API-001 ｜ 版本 v1.1（精简版）｜ 状态：评审中 ｜ 最后更新：2026-10-23
> 上游：[需求](requirements.md) · [架构](architecture.md) ｜ 关联：[数据库设计](database-design.md) · [分工](team-assignments.md) · [开发流程](development-workflow.md) · [设计评审](design-review.md)

## 1. 通用约定

- 基路径 `/api`；请求/响应 `application/json; charset=UTF-8`（无文件上传）；JSON camelCase（`itemPrice`/`contactInfo`/`userId`/`createdAt`），数据库 snake_case，映射以 [database-design.md](database-design.md) 为准。

**统一响应**：成功 `{"code":0,"message":"ok","data":...}`；失败 `{"code":<业务码>,"message":"<中文提示>","data":null}`；参数校验失败 `data` 为 `{"fieldErrors":{"字段名":"错误说明"}}`。

| HTTP | code | 含义 | 触发场景 |
|---|---|---|---|
| 200 | `0` | 成功 | 所有成功响应（含创建/删除/登出） |
| 400 | `10001` | 参数校验失败 | 字段缺失/格式/长度/范围/枚举不合法；`data.fieldErrors` |
| 400 | `10007` | 请求格式错误 | JSON 解析失败、请求体字段类型错误 |
| 401 | `10002` | 未登录 | 无有效 Session 访问受保护接口 |
| 401 | `10003` | 登录失败 | 用户名不存在或密码错误（统一提示） |
| 403 | `10004` | 无权限 | 已登录但非本人且非管理员改删商品 |
| 404 | `10005` | 资源不存在 | 商品不存在或已删除 |
| 405 | `10008` | 方法不支持 | 请求方法不匹配 |
| 409 | `10006` | 用户名已存在 | 注册重复用户名（含并发唯一键冲突） |
| 500 | `10500` | 服务器错误 | 未预期/数据库异常，响应不含堆栈 |

- **认证与会话**：Session + Cookie（`JSESSIONID`，HttpOnly）；白名单 `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/items`、`GET /api/items/{id}`，其余 `/api/**` 需登录；前端 Axios `withCredentials:true`，开发期 Vite 代理 `/api` → `http://localhost:8080`。
- **权限**：游客可浏览/搜索/详情；USER 可发布并改删本人商品；ADMIN 可改删任意商品；归属只取 Session 的 `currentUserId`，请求中的 `userId`/`user_id` 一律忽略；判定顺序：查商品（404）→ ADMIN 放行 → 本人放行 → 其他 403。
- **401/403/404 语义**：未登录 401/`10002`；登录凭据错误 401/`10003`；已登录越权 403/`10004`；商品不存在 404/`10005`。
- **分页/搜索/排序**：`page` 默认 1（≥1）、`size` 默认 10（1–50）；`keyword` 可选（trim 后 ≤50，空=全部，LIKE 模糊并转义 `%`/`_`/`\`）；`sort` 可选 `price_asc`/`price_desc`（空=按创建时间倒序 `created_at DESC, id DESC`，同价 `id DESC`）；响应 `{"list":[...],"total":N,"page":P,"size":S}`。
- **校验与格式**：后端 `@Valid` + Service 二次校验，前端校验只提升体验；字符串 trim 后校验/入库；校验失败不写库；时间 `yyyy-MM-dd HH:mm:ss`（Asia/Shanghai）；金额 JSON number（`DECIMAL(10,2)`，>0 且 ≤999999.99，最多两位小数）；删除/登出成功 `data:null`；写操作 `@Transactional`。
- **前端封装**：所有请求经 `src/utils/request.js`（axios `baseURL:'/api'`、`withCredentials:true`）；401 清状态跳登录、403 提示无权限、400 展示 `fieldErrors`、500 通用提示；页面只经 `src/api/` 调用。
- **列表展示约定**：列表卡片只展示 `item`、`itemPrice`、`createdAt`，不渲染 `contactInfo`；联系方式在详情页展示。接口仍返回 `contactInfo`，后端无需改动。

## 2. 权限与接口总览

> 白名单为 #1/#2/#5/#6；其余接口需登录。未登录访问受保护接口返回 401/`10002`，已登录越权返回 403/`10004`。F-11（管理员管理）复用 #8/#9，不新增接口。

| # | 方法 | 路径 | 登录 | 允许角色 | 需求 |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | 否 | 游客 | F-01 |
| 2 | POST | `/api/auth/login` | 否 | 游客 | F-02 |
| 3 | POST | `/api/auth/logout` | 是 | USER/ADMIN | F-03 |
| 4 | GET | `/api/auth/me` | 是 | USER/ADMIN | F-04 |
| 5 | GET | `/api/items` | 否 | 所有人 | F-05/F-06 |
| 6 | GET | `/api/items/{id}` | 否 | 所有人 | F-07 |
| 7 | POST | `/api/items` | 是 | USER/ADMIN | F-08 |
| 8 | PUT | `/api/items/{id}` | 是 | USER（本人）/ADMIN | F-09 |
| 9 | DELETE | `/api/items/{id}` | 是 | USER（本人）/ADMIN | F-10 |

## 3. 接口契约

> 9 个接口不增不减；成功均 HTTP 200 + `code=0`；所有接口通用错误为 405/`10008`、500/`10500`；错误 `message` 见第 1 节错误码表。

### 3.1 用户注册 `POST /api/auth/register`（公开）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 创建普通用户（`role` 固定 `USER`，非幂等）；`POST /api/auth/register` |
| 登录 / 角色 | 无需登录（白名单）；游客，已登录调用不影响当前登录态 |
| 请求参数（body） | `username` 必填，trim 后 3–20 位，`^[A-Za-z0-9_]+$`，唯一（不区分大小写）；`password` 必填，6–20 位，无空白字符；`role` 等未知字段忽略 |
| 请求示例 | `{"username":"buyer_2024","password":"abc12345"}` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":2,"username":"buyer_2024","role":"USER"}}` |
| 主要错误 | 400/`10001`（`fieldErrors`）、409/`10006`（用户名已存在）、400/`10007`（JSON 格式错误） |
| 后端逻辑 / DB | 校验→查重（409）→BCrypt（cost 10）→插入；`SELECT COUNT(*) FROM user WHERE username=?`；`INSERT INTO user(username,password_hash,role) VALUES(?,?,'USER')`；`@Transactional` |
| 前端调用 | `register(data)`（`src/api/auth.js`）→ `Register.vue`；成功提示后跳 `/login`；400 按 `fieldErrors` 回填，409 提示用户名重复 |

### 3.2 用户登录 `POST /api/auth/login`（公开）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 校验凭据并建立 Session；`POST /api/auth/login` |
| 登录 / 角色 | 无需登录（白名单）；游客，已登录也可调用 |
| 请求参数（body） | `username` 必填，trim 后 ≤50；`password` 必填，≤64；格式错误不单独提示，统一 401 |
| 请求示例 | `{"username":"student","password":"123456"}` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":2,"username":"student","role":"USER"}}`；响应头 `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax` |
| 主要错误 | 401/`10003`（用户名或密码错误，统一提示）、400/`10001`、400/`10007` |
| 后端逻辑 / DB | 查用户→BCrypt `matches`→失败 401/`10003`→新建 Session 并保存 `currentUserId`/`currentUsername`/`currentRole`；`SELECT id,username,password_hash,role FROM user WHERE username=?`；不写库 |
| 前端调用 | `login(data)`（`src/api/auth.js`）→ `Login.vue`；成功保存用户状态并跳 `redirect` 或 `/items`；401 表单内提示、清空密码 |

### 3.3 用户登出 `POST /api/auth/logout`（需登录）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 登出并失效 Session（已登出再调返回 401）；`POST /api/auth/logout` |
| 登录 / 角色 | 需要登录；USER/ADMIN |
| 请求参数 | 无 |
| 请求示例 | `POST /api/auth/logout`（携带 Cookie，无请求体） |
| 成功响应（200） | `{"code":0,"message":"ok","data":null}` |
| 主要错误 | 401/`10002`（未登录或已过期） |
| 后端逻辑 / DB | 当前 Session `invalidate()`；无 DB 操作 |
| 前端调用 | `logout()`（`src/api/auth.js`）→ `AppHeader.vue`；清空用户状态并跳 `/login`；401 同样清理 |

### 3.4 获取当前用户 `GET /api/auth/me`（需登录）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 返回当前登录用户，用于刷新后恢复登录态；`GET /api/auth/me`（只读） |
| 登录 / 角色 | 需要登录；USER/ADMIN |
| 请求参数 | 无 |
| 请求示例 | `GET /api/auth/me`（携带 Cookie） |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":2,"username":"student","role":"USER"}}` |
| 主要错误 | 401/`10002`（未登录或已过期） |
| 后端逻辑 / DB | 读取 Session 的 `currentUserId`/`currentUsername`/`currentRole` 组装 `UserVO`（不查库）；无 DB 操作 |
| 前端调用 | `getCurrentUser()`（`src/api/auth.js`）；应用启动/路由守卫恢复登录态；401 静默视为游客 |

### 3.5 商品列表 `GET /api/items`（公开）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 商品列表：浏览/搜索/排序/分页；`GET /api/items` |
| 登录 / 角色 | 无需登录（白名单）；所有人 |
| 请求参数（query） | `page` 默认 1（≥1）；`size` 默认 10（1–50）；`keyword` 可选（trim ≤50，空=全部，LIKE 模糊）；`sort` 可选 `price_asc`/`price_desc`（空=创建时间倒序，同价 `id DESC`） |
| 请求示例 | `GET /api/items?page=1&size=10&keyword=%E4%B9%A6&sort=price_asc` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"list":[{"id":11,"item":"二手书","itemPrice":15.50,"contactInfo":"QQ 123456","userId":2,"createdAt":"2026-10-22 09:12:35","updatedAt":"2026-10-22 09:12:35"}],"total":1,"page":1,"size":10}}` |
| 主要错误 | 400/`10001`（`page`/`size`/`keyword`/`sort` 非法） |
| 后端逻辑 / DB | 参数校验→`COUNT(*)`→分页查询（`ORDER BY` 白名单）；`SELECT COUNT(*) FROM item [WHERE item LIKE CONCAT('%',#{escapedKeyword},'%') ESCAPE '\\']`；`SELECT id,item,item_price,contact_info,user_id,created_at,updated_at FROM item ... ORDER BY <白名单> LIMIT #{size} OFFSET #{offset}` |
| 前端调用 | `listItems(params)`（`src/api/item.js`）→ `ItemList.vue`；维护 `page`/`size`/`keyword`/`sort`；空结果显示空状态；400 提示参数错误 |

### 3.6 商品详情 `GET /api/items/{id}`（公开）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 商品详情；`GET /api/items/{id}`（只读） |
| 登录 / 角色 | 无需登录（白名单）；所有人 |
| 请求参数（path） | `id` 正整数（非法 400/`10001`） |
| 请求示例 | `GET /api/items/12` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":12,"item":"二手自行车","itemPrice":199.00,"contactInfo":"微信 bike_2024","userId":2,"createdAt":"2026-10-23 18:30:00","updatedAt":"2026-10-23 18:30:00"}}` |
| 主要错误 | 400/`10001`（id 非法）、404/`10005`（不存在或已删除） |
| 后端逻辑 / DB | 按 id 查询，空→404；`SELECT id,item,item_price,contact_info,user_id,created_at,updated_at FROM item WHERE id=?` |
| 前端调用 | `getItem(id)`（`src/api/item.js`）→ `ItemDetail.vue`；404 显示空态并返回列表；本人或 ADMIN 显示编辑/删除入口 |

### 3.7 发布商品 `POST /api/items`（需登录）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 发布商品，发布者取 Session 当前用户；`POST /api/items`（非幂等） |
| 登录 / 角色 | 需要登录；USER/ADMIN |
| 请求参数（body） | `item` 必填（trim 1–100）；`itemPrice` 必填（>0，≤999999.99，最多两位小数）；`contactInfo` 必填（trim 1–100）；`userId` 一律忽略，归属取 Session |
| 请求示例 | `{"item":"二手自行车","itemPrice":199.00,"contactInfo":"微信 bike_2024"}` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":12,"item":"二手自行车","itemPrice":199.00,"contactInfo":"微信 bike_2024","userId":2,"createdAt":"2026-10-23 18:30:00","updatedAt":"2026-10-23 18:30:00"}}` |
| 主要错误 | 401/`10002`、400/`10001`、400/`10007` |
| 后端逻辑 / DB | 校验→`userId=currentUserId`→插入（时间走 DB 默认）；`INSERT INTO item(item,item_price,contact_info,user_id) VALUES(?,?,?,?)`；`@Transactional`；返回 `ItemVO` |
| 前端调用 | `createItem(data)`（`src/api/item.js`）→ `ItemForm.vue`；成功跳详情/列表；400 按 `fieldErrors` 回填；401 跳登录 |

### 3.8 修改商品 `PUT /api/items/{id}`（需登录）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 修改商品（全量更新，幂等）；`PUT /api/items/{id}` |
| 登录 / 角色 | 需要登录；USER（本人商品）/ADMIN（任意商品） |
| 请求参数（path+body） | `id` 正整数；body `item`/`itemPrice`/`contactInfo` 均必填（校验同发布）；`userId` 忽略 |
| 请求示例 | `{"item":"二手自行车（9成新）","itemPrice":159.00,"contactInfo":"微信 bike_2024"}` |
| 成功响应（200） | `{"code":0,"message":"ok","data":{"id":12,"item":"二手自行车（9成新）","itemPrice":159.00,"contactInfo":"微信 bike_2024","userId":2,"createdAt":"2026-10-23 18:30:00","updatedAt":"2026-10-23 18:47:12"}}` |
| 主要错误 | 401/`10002`、403/`10004`（非本人且非管理员）、404/`10005`、400/`10001`、400/`10007` |
| 后端逻辑 / DB | 校验→查商品（404）→权限（ADMIN/本人/403）→更新；`SELECT id,user_id FROM item WHERE id=?`；`UPDATE item SET item=?,item_price=?,contact_info=?,updated_at=CURRENT_TIMESTAMP WHERE id=?`；`@Transactional`；返回更新后 `ItemVO` |
| 前端调用 | `updateItem(id,data)`（`src/api/item.js`）→ `ItemForm.vue`；仅本人/ADMIN 显示编辑入口；成功回详情；403/404 提示并刷新 |

### 3.9 删除商品 `DELETE /api/items/{id}`（需登录）

| 项 | 内容 |
|---|---|
| 用途 / 方法 | 删除商品（物理删除，重复删除 404）；`DELETE /api/items/{id}` |
| 登录 / 角色 | 需要登录；USER（本人商品）/ADMIN（任意商品） |
| 请求参数（path） | `id` 正整数（非法 400/`10001`） |
| 请求示例 | `DELETE /api/items/12` |
| 成功响应（200） | `{"code":0,"message":"ok","data":null}` |
| 主要错误 | 401/`10002`、403/`10004`、404/`10005`、400/`10001` |
| 后端逻辑 / DB | 校验→查商品（404）→权限（403）→物理删除；`SELECT id,user_id FROM item WHERE id=?`；`DELETE FROM item WHERE id=?`；`@Transactional` |
| 前端调用 | `deleteItem(id)`（`src/api/item.js`）→ 列表/详情删除按钮；`ElMessageBox.confirm` 二次确认；成功刷新列表；403/404 提示 |

## 4. 边界与异常速查

| 场景 | 规定响应 |
|---|---|
| 重复注册（含并发唯一键） | 409/`10006`，前端在 `username` 下提示 |
| 账号不存在或密码错误 | 401/`10003`，统一「用户名或密码错误」 |
| 未登录访问受保护接口 | 401/`10002`，前端清状态跳登录 |
| 登录用户伪造 `userId` 发布 | 忽略该字段，归属为 Session 当前用户（200/`0`）；未登录 401/`10002` |
| USER 改删他人商品 | 403/`10004`，数据不变；ADMIN 改删任意商品成功（200/`0`） |
| 商品不存在/已删除（详情/改/删） | 404/`10005` |
| 参数不合法（缺字段/超范围/超长/枚举非法） | 400/`10001` + `fieldErrors`；JSON 解析失败 400/`10007` |
| 数据库异常或并发删改 | 500/`10500`；写操作影响行数为 0 → 404/`10005` |

## 5. 变更记录

| 日期 | 版本 | 修改人 | 变更内容 |
|---|---|---|---|
| 2026-10-23 | v1.1（精简版） | A | 精简为课程版：保留统一响应、错误码、权限矩阵与 9 接口契约；删除安全扩展、18 类边界展开、风险与自检章节 |
