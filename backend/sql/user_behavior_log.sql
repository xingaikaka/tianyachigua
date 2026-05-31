-- ============================================================
-- 用户行为流水表（user_behavior_log）
-- 用途：记录前台用户所有操作（按 IP / fingerprint / anonymousId 区分）
-- 写入方式：异步队列批量 INSERT
-- 保留策略：按月分区，建议保留近 3 个月
-- ============================================================

DROP TABLE IF EXISTS `user_behavior_log`;
CREATE TABLE `user_behavior_log` (
  `id`             bigint        NOT NULL AUTO_INCREMENT       COMMENT '主键',
  `event_time`     datetime      NOT NULL                       COMMENT '事件发生时间',
  `event_date`     date          NOT NULL                       COMMENT '事件日期（分区键）',
  `event_type`     varchar(32)   NOT NULL                       COMMENT '事件类型: page_view/search_submit/search_no_result/category_click/video_view/video_play/video_like/video_unlike/video_share/ad_click',
  `event_target`   varchar(64)   DEFAULT NULL                   COMMENT '目标 ID（videoId/categoryId/adId/postId 等）',
  `keyword`        varchar(200)  DEFAULT NULL                   COMMENT '搜索关键词（仅搜索事件）',
  `page_path`      varchar(255)  DEFAULT NULL                   COMMENT '页面路径',
  `referrer`       varchar(255)  DEFAULT NULL                   COMMENT '来源页面',

  -- 用户标识（三层降级：anonymousId > fingerprint > ip）
  `ip`             varchar(64)   NOT NULL                       COMMENT '客户端 IP',
  `ip_region`      varchar(128)  DEFAULT NULL                   COMMENT 'IP 归属地（国家|区域|省|市|ISP）',
  `is_china`       tinyint(1)    NOT NULL DEFAULT '0'           COMMENT '是否中国大陆 IP',
  `fingerprint`    varchar(32)   NOT NULL                       COMMENT 'MD5(ip|ua) 前 16 位',
  `anonymous_id`   varchar(64)   DEFAULT NULL                   COMMENT '前端 localStorage 持久 ID',
  `session_id`     varchar(64)   DEFAULT NULL                   COMMENT '前端 sessionStorage ID',

  -- 设备信息
  `user_agent`     varchar(255)  DEFAULT NULL                   COMMENT 'User-Agent 截断 255',
  `device_type`    varchar(16)   DEFAULT NULL                   COMMENT 'mobile/desktop/tablet',
  `browser`        varchar(32)   DEFAULT NULL                   COMMENT '浏览器名',
  `os`             varchar(32)   DEFAULT NULL                   COMMENT '操作系统',

  -- 业务字段
  `duration_ms`    int           DEFAULT NULL                   COMMENT '停留 / 播放时长（毫秒）',
  `extra`          varchar(1000) DEFAULT NULL                   COMMENT 'JSON 扩展字段',

  PRIMARY KEY (`id`, `event_date`),
  KEY `idx_ip_time`        (`ip`, `event_time`),
  KEY `idx_anon_time`      (`anonymous_id`, `event_time`),
  KEY `idx_fp_time`        (`fingerprint`, `event_time`),
  KEY `idx_type_time`      (`event_type`, `event_time`),
  KEY `idx_target_time`    (`event_type`, `event_target`, `event_time`),
  KEY `idx_date_type`      (`event_date`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='用户行为流水表（前台用户审计）'
PARTITION BY RANGE COLUMNS(event_date) (
  PARTITION p_202605  VALUES LESS THAN ('2026-06-01'),
  PARTITION p_202606  VALUES LESS THAN ('2026-07-01'),
  PARTITION p_202607  VALUES LESS THAN ('2026-08-01'),
  PARTITION p_202608  VALUES LESS THAN ('2026-09-01'),
  PARTITION p_202609  VALUES LESS THAN ('2026-10-01'),
  PARTITION p_max     VALUES LESS THAN MAXVALUE
);

-- ============================================================
-- 菜单 SQL：在「系统监控」(parent_id=2) 下追加「用户行为」
-- 参考 sys_oper_log (menu_id=502) 的同级位置
-- ============================================================

-- 删除已存在的同名菜单（重复执行幂等）
DELETE FROM `sys_menu`  WHERE `menu_name` = '用户行为' AND `parent_id` = 2;
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='用户行为' AND parent_id=2) t);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('用户行为', 2, 8, 'behavior', 'monitor/behavior/index', '', 1, 0, 'C', '0', '0', 'monitor:behavior:list', 'eye', 'admin', NOW(), '', NULL, '前台用户行为追踪（按 IP 区分）');

-- 自动授权给超级管理员角色（role_id=1）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE `menu_name` = '用户行为' AND `parent_id` = 2;
