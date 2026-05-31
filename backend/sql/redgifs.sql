/*
 RedGifs站点集成 - 数据库表结构
 
 创建日期: 2026-02-11
 说明: 包含RedGifs用户表和视频表
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for redgifs_users
-- ----------------------------
DROP TABLE IF EXISTS `redgifs_users`;
CREATE TABLE `redgifs_users` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名（唯一标识）',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户描述',
  `profile_image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL（R2相对路径）',
  `followers` int NOT NULL DEFAULT '0' COMMENT '粉丝数',
  `following` int NOT NULL DEFAULT '0' COMMENT '关注数',
  `gifs_count` int NOT NULL DEFAULT '0' COMMENT '总视频数',
  `published_gifs_count` int NOT NULL DEFAULT '0' COMMENT '已发布视频数',
  `views` bigint NOT NULL DEFAULT '0' COMMENT '总观看次数',
  `verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否认证：1-是，0-否',
  `profile_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'RedGifs主页URL',
  `creation_time` bigint DEFAULT NULL COMMENT '账号创建时间（时间戳）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `sync_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '同步状态：0-未同步，1-同步中，2-已同步',
  `last_sync_at` timestamp NULL DEFAULT NULL COMMENT '最后同步时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_verified` (`verified`),
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_followers` (`followers`),
  KEY `idx_views` (`views`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RedGifs用户信息表';

-- ----------------------------
-- Table structure for redgifs_videos
-- ----------------------------
DROP TABLE IF EXISTS `redgifs_videos`;
CREATE TABLE `redgifs_videos` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `gif_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'RedGifs视频ID（唯一标识）',
  `user_id` int NOT NULL COMMENT '所属用户ID（关联redgifs_users.id）',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频标题（自动生成）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频描述',
  `duration` decimal(10,3) DEFAULT NULL COMMENT '视频时长（秒）',
  `width` int DEFAULT NULL COMMENT '视频宽度',
  `height` int DEFAULT NULL COMMENT '视频高度',
  `resolution` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分辨率（如：1920x1080）',
  `has_audio` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否有音频：1-是，0-否',
  `likes` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `views` int NOT NULL DEFAULT '0' COMMENT '观看数',
  `verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否认证：1-是，0-否',
  `tags` json DEFAULT NULL COMMENT '标签（JSON数组）',
  `sexuality` json DEFAULT NULL COMMENT '类型（JSON数组）',
  `niches` json DEFAULT NULL COMMENT '分类（JSON数组）',
  `hd_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '高清MP4 URL（R2相对路径）',
  `sd_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标清MP4 URL（R2相对路径）',
  `poster_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图URL（R2相对路径）',
  `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图URL（R2相对路径）',
  `redgifs_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'RedGifs原始URL',
  `create_date` bigint DEFAULT NULL COMMENT 'RedGifs创建时间（时间戳）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-已发布，0-草稿',
  `sync_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '同步状态：0-未同步，1-同步中，2-已同步',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gif_id` (`gif_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_date` (`create_date`),
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_likes` (`likes`),
  KEY `idx_views` (`views`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_redgifs_videos_user_id` FOREIGN KEY (`user_id`) REFERENCES `redgifs_users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RedGifs视频信息表';

SET FOREIGN_KEY_CHECKS = 1;
