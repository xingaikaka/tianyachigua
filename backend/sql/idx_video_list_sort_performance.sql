-- ============================================================
-- 视频列表排序性能优化索引
-- 用于支持 ORDER BY is_recommended DESC, is_hot DESC, sort_order DESC, published_at DESC, created_at DESC
-- 执行前请用 EXPLAIN 验证查询计划；若索引已存在会报错，可忽略或先 DROP INDEX
-- ============================================================

-- 1. videos 表：列表查询主索引（status 过滤 + 排序字段）
-- 覆盖 selectCategoryVideoPage, selectShortVideoPage, selectVideoListOnlyPaged, selectWebVideoListBasicPaged
-- MySQL 8.0+ 支持 DESC 索引，低版本可去掉 DESC 关键字
CREATE INDEX idx_videos_status_recommended_hot_sort 
ON videos (status, is_recommended, is_hot, sort_order, published_at, created_at);

-- 2. video_category_tag_relations 表：EXISTS 子查询优化
-- 用于 category_id 筛选、relation_type 区分
CREATE INDEX idx_vctr_video_relation_category 
ON video_category_tag_relations (video_id, relation_type, category_id);

-- 3. video_category_tag_relations：按 relation_type 查询（标签筛选等）
CREATE INDEX idx_vctr_video_relation_tag 
ON video_category_tag_relations (video_id, relation_type, tag_id);
