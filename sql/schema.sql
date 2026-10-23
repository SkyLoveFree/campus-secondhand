-- 校园二手交易系统 建表脚本（ddl）
-- 适用：MySQL 8.0.16+（CHECK 需 8.0.16+）；InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
-- 用途：开发/演示环境建库建表，可重复执行；执行顺序：本脚本 → sql/data.sql
-- 执行（仓库根目录）：cmd /c "mysql -u<user> -p --default-character-set=utf8mb4 < sql\schema.sql"
-- 无建库权限时：注释下方 CREATE DATABASE 与 USE，用已授权库执行

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS campus_secondhand
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_secondhand;

-- 先删子表 item，再删父表 user
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id            BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '用户ID，主键，自增',
  username      VARCHAR(50)  NOT NULL                             COMMENT '登录用户名，唯一；3-20 位由后端校验',
  password_hash VARCHAR(100) NOT NULL                             COMMENT 'BCrypt 密码哈希（cost 10），禁止明文，接口不返回',
  role          VARCHAR(16)  NOT NULL DEFAULT 'USER'              COMMENT '角色：USER / ADMIN',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP         COMMENT '更新时间，自动刷新',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表：注册用户与管理员（游客不落库）';

CREATE TABLE item (
  id           BIGINT        NOT NULL AUTO_INCREMENT              COMMENT '商品ID，主键，自增',
  item         VARCHAR(100)  NOT NULL                             COMMENT '商品名称（模糊搜索字段），1-100 字符',
  item_price   DECIMAL(10,2) NOT NULL                             COMMENT '商品价格（元），范围 (0, 999999.99]',
  contact_info VARCHAR(100)  NOT NULL                             COMMENT '联系方式，1-100 字符',
  user_id      BIGINT        NOT NULL                             COMMENT '发布者用户ID，外键 user.id，由后端 Session 写入',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '发布时间，默认列表排序字段',
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP          COMMENT '更新时间，修改商品时自动刷新',
  PRIMARY KEY (id),
  KEY idx_item_user_id (user_id)                                  COMMENT '外键索引',
  KEY idx_item_created_at (created_at)                            COMMENT '默认列表排序 created_at DESC, id DESC',
  KEY idx_item_price (item_price)                                 COMMENT '价格排序（可选）',
  CONSTRAINT fk_item_user FOREIGN KEY (user_id) REFERENCES `user` (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_item_price_range CHECK (item_price > 0 AND item_price <= 999999.99)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品表：用户发布的二手商品';

-- 验证（可选）：SHOW TABLES; / SHOW INDEX FROM item; / SHOW CREATE TABLE `user`;
