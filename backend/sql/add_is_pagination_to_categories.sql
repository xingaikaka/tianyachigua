-- 为categories表添加is_pagination字段
-- 添加时间: 2025-01-XX
-- 说明: 添加是否分页模式字段，用于控制分类下的视频列表是否使用分页模式

ALTER TABLE `categories` 
ADD COLUMN `is_pagination` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否分页模式：1-是，0-否' 
AFTER `is_short`;

