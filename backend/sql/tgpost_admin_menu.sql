-- ============================================================
-- TG 帖子管理菜单（挂载到吃瓜管理 parent_id=2000）
-- 执行一次即可，重复执行无副作用（IF NOT EXISTS 保护）
-- ============================================================

-- 1. 插入目录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('TG帖子管理', 2000, 10, 'tgpost', 'chigua/tgpost/index', 1, 0, 'C', '0', '0', 'chigua:tgpost:list', 'list', 'admin', NOW(), '', NULL, 'Telegram帖子及媒体管理');

SET @tgpost_menu_id = LAST_INSERT_ID();

-- 2. 插入按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
('帖子查询', @tgpost_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'chigua:tgpost:list',   '#', 'admin', NOW(), '', NULL, ''),
('帖子修改', @tgpost_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'chigua:tgpost:edit',   '#', 'admin', NOW(), '', NULL, ''),
('帖子删除', @tgpost_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'chigua:tgpost:remove', '#', 'admin', NOW(), '', NULL, '');
