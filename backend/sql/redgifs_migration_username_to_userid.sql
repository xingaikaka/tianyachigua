/*
 RedGifs视频表迁移脚本：将 username 外键改为 user_id
 
 执行前请备份数据库！
 
 创建日期: 2026-02-11
 说明: 将 redgifs_videos.username 改为 redgifs_videos.user_id，并关联到 redgifs_users.id
*/

USE chigua7;

-- 1. 删除旧的外键约束
ALTER TABLE `redgifs_videos` 
DROP FOREIGN KEY `fk_redgifs_videos_username`;

-- 2. 添加新的 user_id 列
ALTER TABLE `redgifs_videos` 
ADD COLUMN `user_id` int NOT NULL COMMENT '所属用户ID（关联redgifs_users.id）' AFTER `gif_id`;

-- 3. 从 username 迁移数据到 user_id
UPDATE `redgifs_videos` v
INNER JOIN `redgifs_users` u ON v.`username` = u.`username`
SET v.`user_id` = u.`id`;

-- 4. 删除旧的 username 列和索引
ALTER TABLE `redgifs_videos` 
DROP INDEX `idx_username`,
DROP COLUMN `username`;

-- 5. 创建新的索引
ALTER TABLE `redgifs_videos` 
ADD INDEX `idx_user_id` (`user_id`);

-- 6. 添加新的外键约束
ALTER TABLE `redgifs_videos` 
ADD CONSTRAINT `fk_redgifs_videos_user_id` 
FOREIGN KEY (`user_id`) REFERENCES `redgifs_users` (`id`) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- 7. 验证数据
SELECT 
    '迁移完成！' as message,
    COUNT(*) as total_videos,
    COUNT(DISTINCT user_id) as total_users
FROM redgifs_videos;
