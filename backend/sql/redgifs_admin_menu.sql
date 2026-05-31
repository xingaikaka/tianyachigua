-- ----------------------------
-- RedGifs视频管理菜单 SQL
-- ----------------------------

-- 父菜单（挂载到吃瓜管理下，parent_id=2000）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('RedGifs管理', 2000, 6, 'redgifs', NULL, 1, 0, 'M', '0', '0', NULL, 'video', 'admin', NOW(), '', NULL, 'RedGifs用户和视频管理');

-- 获取刚插入的父菜单ID（假设ID为最大ID）
SET @parent_menu_id = LAST_INSERT_ID();

-- RedGifs用户管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('用户管理', @parent_menu_id, 1, 'user', 'chigua/redgifs/user/index', 1, 0, 'C', '0', '0', 'chigua:redgifs:user:list', 'peoples', 'admin', NOW(), '', NULL, 'RedGifs用户管理');

SET @user_menu_id = LAST_INSERT_ID();

-- RedGifs用户管理按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('用户查询', @user_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:user:query', '#', 'admin', NOW(), '', NULL, ''),
('用户新增', @user_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:user:add', '#', 'admin', NOW(), '', NULL, ''),
('用户修改', @user_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:user:edit', '#', 'admin', NOW(), '', NULL, ''),
('用户删除', @user_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:user:remove', '#', 'admin', NOW(), '', NULL, ''),
('用户导出', @user_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:user:export', '#', 'admin', NOW(), '', NULL, '');

-- RedGifs视频管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('视频管理', @parent_menu_id, 2, 'video', 'chigua/redgifs/video/index', 1, 0, 'C', '0', '0', 'chigua:redgifs:video:list', 'video', 'admin', NOW(), '', NULL, 'RedGifs视频管理');

SET @video_menu_id = LAST_INSERT_ID();

-- RedGifs视频管理按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
('视频查询', @video_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:video:query', '#', 'admin', NOW(), '', NULL, ''),
('视频新增', @video_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:video:add', '#', 'admin', NOW(), '', NULL, ''),
('视频修改', @video_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:video:edit', '#', 'admin', NOW(), '', NULL, ''),
('视频删除', @video_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:video:remove', '#', 'admin', NOW(), '', NULL, ''),
('视频导出', @video_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'chigua:redgifs:video:export', '#', 'admin', NOW(), '', NULL, '');
