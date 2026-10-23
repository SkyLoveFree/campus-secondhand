-- 校园二手交易系统 测试/种子数据（dml）
-- 前置：先执行 sql/schema.sql；本脚本可重复执行（先删后插）
-- 执行（仓库根目录）：cmd /c "mysql -u<user> -p --default-character-set=utf8mb4 < sql\data.sql"
-- 安全：数据均为虚构；测试口令 123456 仅本地测试，生产环境禁用并删除种子数据

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE campus_secondhand;

-- 先删子表 item，再删父表 user
DELETE FROM item;
DELETE FROM `user`;

-- 复位自增值（种子固定占用 user.id 1-2、item.id 1-5）
ALTER TABLE item AUTO_INCREMENT = 1;
ALTER TABLE `user` AUTO_INCREMENT = 1;

-- 测试账号：密码均为 123456 的 BCrypt 哈希（cost 10），两账号复用同一哈希仅为测试简化
INSERT INTO `user` (id, username, password_hash, role) VALUES
  (1, 'admin',   '$2b$10$5qMnxPnrvW8Sw0bqJ6hhzOeDYu2B19t1sMomx4CbfMpgOwXpSi.5q', 'ADMIN'),
  (2, 'student', '$2b$10$5qMnxPnrvW8Sw0bqJ6hhzOeDYu2B19t1sMomx4CbfMpgOwXpSi.5q', 'USER');

-- 示例商品：全部归属 student（user_id=2），价格互不相同，便于验证价格升/降序
INSERT INTO item (id, item, item_price, contact_info, user_id) VALUES
  (1, '二手自行车（九成新）',       280.00, '微信：student_demo', 2),
  (2, '高等数学教材（同济第七版）',  25.50, 'QQ：100000001',      2),
  (3, '护眼台灯',                    39.90, '微信：student_demo', 2),
  (4, '蓝牙耳机',                    89.00, '电话：13800000000', 2),
  (5, '考研英语真题（近十年）',      15.00, 'QQ：100000001',      2);

-- 验证（可选）：SELECT id, username, role FROM `user`; / SELECT COUNT(*) FROM item;（期望 2 账号、5 商品）
