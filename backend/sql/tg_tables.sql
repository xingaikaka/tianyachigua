-- =============================================================
-- Telegram 内容存储表结构
-- 更新时间：2026-03-07
-- 说明：两张表，由外部同步工具写入数据，Java API 读取，前端展示
--       先删后建，确保结构最新
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tg_media`;
DROP TABLE IF EXISTS `tg_posts`;

SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────────────────────────
-- 1. 帖子表  tg_posts
--    一条 = 一个媒体组（一次发布的若干图片/视频）
-- ─────────────────────────────────────────────────────────────
CREATE TABLE `tg_posts` (
  `id`          INT          NOT NULL AUTO_INCREMENT                    COMMENT '主键',
  `category_id` INT          DEFAULT NULL                               COMMENT '关联网站分类ID',
  `source_id`   VARCHAR(200) DEFAULT NULL                               COMMENT '同步源原始ID（同步工具写入，用于去重：写前先查此字段）',
  `caption`     TEXT         DEFAULT NULL                               COMMENT '帖子正文（标题/描述/标签）',
  `post_date`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '内容发布时间（来自同步源）',
  `media_count` INT          NOT NULL DEFAULT 0                         COMMENT '媒体总数',
  `photo_count` INT          NOT NULL DEFAULT 0                         COMMENT '图片数量',
  `video_count` INT          NOT NULL DEFAULT 0                         COMMENT '视频数量',
  `views`       INT          NOT NULL DEFAULT 0                         COMMENT '站内浏览次数',
  `sort_order`  INT          NOT NULL DEFAULT 0                         COMMENT '排序权重（越大越靠前）',
  `status`      TINYINT(1)   NOT NULL DEFAULT 1                         COMMENT '状态：1=显示 0=隐藏',
  `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '入库时间',
  `updated_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP                COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_id`    (`source_id`),
  KEY        `idx_category_id` (`category_id`),
  KEY        `idx_post_date`   (`post_date`),
  KEY        `idx_status_sort` (`status`, `sort_order`, `post_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='帖子表（一个媒体组=一条记录）';


-- ─────────────────────────────────────────────────────────────
-- 2. 媒体文件表  tg_media
--    一条 = 一张图片或一个视频
-- ─────────────────────────────────────────────────────────────
CREATE TABLE `tg_media` (
  `id`                 INT          NOT NULL AUTO_INCREMENT             COMMENT '主键',
  `post_id`            INT          NOT NULL                            COMMENT '关联帖子ID（tg_posts.id）',
  `media_type`         ENUM('photo','video') NOT NULL                   COMMENT '媒体类型',

  -- 存储地址
  `local_path`         VARCHAR(500) DEFAULT NULL                        COMMENT '服务器本地文件路径',
  `local_url`          VARCHAR(500) DEFAULT NULL                        COMMENT '前端可访问URL（图片/视频直链）',

  -- 尺寸信息
  `width`              INT          DEFAULT NULL                        COMMENT '宽度（像素）',
  `height`             INT          DEFAULT NULL                        COMMENT '高度（像素）',
  `file_size`          BIGINT       DEFAULT NULL                        COMMENT '文件大小（字节）',
  `mime_type`          VARCHAR(50)  DEFAULT NULL                        COMMENT 'MIME类型，如 image/jpeg、video/mp4',

  -- 视频专用
  `duration`           DECIMAL(10,3) DEFAULT NULL                       COMMENT '视频时长（秒，支持小数）',
  `supports_streaming` TINYINT(1)   DEFAULT 0                           COMMENT '是否支持流媒体播放：1=是 0=否',

  -- 封面图（视频用）
  `thumb_url`          VARCHAR(500) DEFAULT NULL                        COMMENT '封面图/缩略图URL',
  `thumb_width`        INT          DEFAULT NULL                        COMMENT '封面图宽度',
  `thumb_height`       INT          DEFAULT NULL                        COMMENT '封面图高度',

  -- 排序
  `sort_order`         INT          NOT NULL DEFAULT 0                  COMMENT '帖子内显示顺序（从0开始正序）',
  `created_at`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '入库时间',

  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='媒体文件表（每张图片/每个视频一条）';
