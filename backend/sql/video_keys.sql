-- 视频AES加密密钥表
-- 存储爬虫从原站获取的 AES-128 密钥，供播放器解密使用
-- 创建时间: 2026-03-15

CREATE TABLE IF NOT EXISTS `video_keys` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT        COMMENT '主键',
  `video_id`   BIGINT        NOT NULL                       COMMENT '关联 videos 表的 id',
  `source_id`  VARCHAR(200)  DEFAULT NULL                   COMMENT '爬虫来源唯一标识（如 missav_start-408）',
  `key_data`   VARBINARY(16) NOT NULL                       COMMENT 'AES-128 密钥原始字节（16字节明文）',
  `key_iv`     VARCHAR(64)   DEFAULT NULL                   COMMENT 'IV 十六进制字符串（如 0x419b2bcf...）',
  `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频 AES-128 加密密钥表';
