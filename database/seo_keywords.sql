-- =====================================================
-- SEO关键词表（独立模块）
-- 用途：存储50000+关键词，生成SEO聚合页面
-- 创建时间：2026-01-14
-- =====================================================

CREATE TABLE IF NOT EXISTS `seo_keywords` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关键词ID',
  `keyword` varchar(255) NOT NULL COMMENT '关键词内容',
  `seo_title` varchar(255) DEFAULT NULL COMMENT 'SEO标题（可自定义，为空时自动生成）',
  `seo_description` varchar(500) DEFAULT NULL COMMENT 'SEO描述（可自定义，为空时自动生成）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `priority` decimal(2,1) NOT NULL DEFAULT '0.6' COMMENT 'SEO权重（用于sitemap，0.0-1.0）',
  `search_count` int(11) NOT NULL DEFAULT '0' COMMENT '搜索次数统计',
  `click_count` int(11) NOT NULL DEFAULT '0' COMMENT '点击次数统计',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`) COMMENT '关键词唯一索引',
  KEY `idx_status` (`status`) COMMENT '状态索引',
  KEY `idx_search_count` (`search_count` DESC) COMMENT '搜索次数索引（用于热门排序）',
  KEY `idx_click_count` (`click_count` DESC) COMMENT '点击次数索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO关键词表（独立于标签系统）';

-- 创建索引以优化查询性能
CREATE INDEX idx_created_at ON seo_keywords(created_at DESC);
CREATE INDEX idx_priority ON seo_keywords(priority DESC);

