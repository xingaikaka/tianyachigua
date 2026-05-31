-- 主表 tg_posts 添加"置顶"字段
ALTER TABLE tg_posts
    ADD COLUMN is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶：1=是 0=否'
    AFTER status;

-- 子表 tg_media 添加"推荐"字段
ALTER TABLE tg_media
    ADD COLUMN is_recommend TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐：1=是 0=否'
    AFTER sort_order;
