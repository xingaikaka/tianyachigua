/*
 Navicat MySQL Dump SQL

 Source Server         : chigua
 Source Server Type    : MySQL
 Source Server Version : 90300 (9.3.0)
 Source Host           : localhost:3306
 Source Schema         : chigua

 Target Server Type    : MySQL
 Target Server Version : 90300 (9.3.0)
 File Encoding         : 65001

 Date: 24/07/2025 02:56:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for advertisements
-- ----------------------------
DROP TABLE IF EXISTS `advertisements`;
CREATE TABLE `advertisements` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '广告ID，主键',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '广告标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '广告描述',
  `ad_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '广告类型：1-横幅广告，2-logo广告，3-文字链接广告，4-弹窗广告',
  `app_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用类型（logo广告专用）：1-热门应用，2-最新上架，3-必备精品',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '广告位置，多个位置用逗号分隔，如：1,2,3',
  `category_id` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属分类ID，多个分类用逗号分隔，如：1,2,3',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '广告图片URL',
  `icon_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标名称',
  `link_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击跳转链接',
  `link_text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '链接文字（文字广告使用）',
  `click_count` int NOT NULL DEFAULT '0' COMMENT '点击次数统计',
  `impression_count` int NOT NULL DEFAULT '0' COMMENT '展示次数统计',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `start_date` date DEFAULT NULL COMMENT '投放开始日期',
  `end_date` date DEFAULT NULL COMMENT '投放结束日期',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_global` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否全分类显示：1-全分类显示（忽略category_id），0-按分类显示',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储网站各种类型的广告信息';

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID，主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '分类描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，数字越大排序越靠前',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `ad_display_mode` tinyint(1) NOT NULL DEFAULT '1' COMMENT '广告显示模式：1-集中显示(所有广告先显示)，2-交替显示(广告与内容穿插)，3-不显示广告',
  `ad_interval` int NOT NULL DEFAULT '3' COMMENT '广告显示间隔：交替显示模式下，每几个内容项之间插入一个广告',
  `is_collection` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否视频合集：0-否，1-是',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ad_display_mode` (`ad_display_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储网站内容分类信息，如学生校园、热门大瓜等分类';

-- ----------------------------
-- Table structure for collections
-- ----------------------------
DROP TABLE IF EXISTS `collections`;
CREATE TABLE `collections` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '合集ID，主键',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合集标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '合集描述',
  `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合集封面图片URL',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合集作者',
  `category_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属分类ID，多个分类用逗号分隔，如：1,2,3',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '观看次数',
  `video_count` int NOT NULL DEFAULT '0' COMMENT '包含视频数量',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频合集信息，如每日大瓜TOP10等合集';

-- ----------------------------
-- Table structure for comments
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `video_id` bigint NOT NULL COMMENT '视频ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱（可选）',
  `content` text NOT NULL COMMENT '评论内容',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
  `reply_count` int DEFAULT '0' COMMENT '回复数量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `is_sticky` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频评论表';

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
  `status` char(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度日志表';

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=146 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2050 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=402 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';

-- ----------------------------
-- Table structure for tags
-- ----------------------------
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '标签ID，主键',
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `color` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签颜色，16进制色值',
  `usage_count` int NOT NULL DEFAULT '0' COMMENT '使用次数统计',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储内容标签信息，用于内容分类和搜索';

-- ----------------------------
-- Table structure for video_category_tag_relations
-- ----------------------------
DROP TABLE IF EXISTS `video_category_tag_relations`;
CREATE TABLE `video_category_tag_relations` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关系ID，主键',
  `video_id` int NOT NULL COMMENT '视频ID',
  `category_id` int DEFAULT NULL COMMENT '分类ID',
  `tag_id` int DEFAULT NULL COMMENT '标签ID',
  `relation_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '关系类型：1-分类关系，2-标签关系',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_vctr_video_relation_category` (`video_id`,`relation_type`,`category_id`),
  KEY `idx_vctr_video_relation_tag` (`video_id`,`relation_type`,`tag_id`),
  KEY `idx_vctr_category_relation_video` (`category_id`,`relation_type`,`video_id`),
  KEY `idx_vctr_tag_relation_video` (`tag_id`,`relation_type`,`video_id`)
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频与分类、标签的多对多关系';

-- ----------------------------
-- Table structure for video_collections
-- ----------------------------
DROP TABLE IF EXISTS `video_collections`;
CREATE TABLE `video_collections` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关系ID，主键',
  `collection_id` int NOT NULL COMMENT '合集ID',
  `video_id` int NOT NULL COMMENT '视频ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '在合集中的排序',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频与合集的关系，一个视频可以属于多个合集';

-- ----------------------------
-- Table structure for video_images
-- ----------------------------
DROP TABLE IF EXISTS `video_images`;
CREATE TABLE `video_images` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '图片ID，主键',
  `video_id` int NOT NULL COMMENT '视频ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '图片描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL地址',
  `file_size` int DEFAULT NULL COMMENT '图片文件大小（字节）',
  `width` int DEFAULT NULL COMMENT '图片宽度（像素）',
  `height` int DEFAULT NULL COMMENT '图片高度（像素）',
  `alt_text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片替代文本，用于SEO和无障碍访问',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，数字越大排序越靠前',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为主图：1-是，0-否',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频相关的图片信息，如预览图、截图、宣传图等';

-- ----------------------------
-- Table structure for video_transcodes
-- ----------------------------
DROP TABLE IF EXISTS `video_transcodes`;
CREATE TABLE `video_transcodes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传用户ID',
  `video_type` enum('short','long') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'short' COMMENT '视频类型：short=短视频, long=长视频',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频描述',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单次观看价格',
  `coin_price` int NOT NULL DEFAULT '0' COMMENT '金币价格',
  `points_price` int NOT NULL DEFAULT '0' COMMENT '积分价格',
  `discount_price` decimal(10,2) DEFAULT NULL COMMENT '优惠价格',
  `discount_coin_price` int DEFAULT NULL COMMENT '优惠金币价格',
  `discount_start_at` timestamp NULL DEFAULT NULL COMMENT '优惠开始时间',
  `discount_end_at` timestamp NULL DEFAULT NULL COMMENT '优惠结束时间',
  `creator_revenue_rate` decimal(5,4) NOT NULL DEFAULT '0.7000' COMMENT '创作者分成比例(0-1)',
  `required_vip_level` int NOT NULL DEFAULT '0' COMMENT '所需VIP等级(0=无要求)',
  `is_vip_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否VIP专享',
  `is_premium_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否高级会员专享',
  `rpath` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '相对路径',
  `thumbnails` json DEFAULT NULL COMMENT '缩略图列表',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `content_type` enum('free','ppv','subscription','vip','premium','coin','points') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'free' COMMENT '内容类型',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否精选',
  `is_editor_choice` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否编辑推荐',
  `is_hot` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_new` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否新品',
  `is_exclusive` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否独家',
  `is_trending` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否私密',
  `is_adult` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否成人内容',
  `resolution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分辨率(如1920x1080)',
  `transcode_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '转码系统返回的_id',
  `md5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件MD5值',
  `shareid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享ID',
  `orgfile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主域名',
  `picdomain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片域名',
  `mp4domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MP4域名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频路径',
  `suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件后缀',
  `bitrate` int NOT NULL DEFAULT '0' COMMENT '码率(kbps)',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小(bytes)',
  `duration` int NOT NULL DEFAULT '0' COMMENT '时长(秒)',
  `fps` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '帧率',
  `width` int NOT NULL DEFAULT '0' COMMENT '视频宽度',
  `height` int NOT NULL DEFAULT '0' COMMENT '视频高度',
  `views_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '播放次数',
  `likes_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '点赞数',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '踩数',
  `favorite_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '收藏数',
  `share_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '分享数',
  `download_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '下载次数',
  `purchase_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '购买次数',
  `total_revenue` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总收益',
  `daily_view_limit` int NOT NULL DEFAULT '0' COMMENT '每日观看限制(0=无限制)',
  `comments_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '评论数',
  `free_preview_duration` int NOT NULL DEFAULT '0' COMMENT '免费预览时长(秒)',
  `allow_preview` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许预览',
  `age_rating` int NOT NULL DEFAULT '0' COMMENT '年龄分级(0-21)',
  `status` enum('processing','completed','failed','published','draft','deleted') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'processing' COMMENT '视频状态',
  `quality` enum('sd','hd','fhd','4k','8k') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'hd' COMMENT '视频质量',
  `allow_comments` tinyint(1) NOT NULL DEFAULT '1' COMMENT '允许评论',
  `allow_downloads` tinyint(1) NOT NULL DEFAULT '0' COMMENT '允许下载',
  `allowed_countries` json DEFAULT NULL COMMENT '允许观看的国家',
  `blocked_countries` json DEFAULT NULL COMMENT '禁止观看的国家',
  `language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en' COMMENT '视频语言',
  `subtitles` json DEFAULT NULL COMMENT '字幕信息',
  `transcode_begin` timestamp NULL DEFAULT NULL COMMENT '转码开始时间',
  `transcode_end` timestamp NULL DEFAULT NULL COMMENT '转码结束时间',
  `transcode_result` enum('ok','failed','pending') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '转码结果',
  `output_formats` json DEFAULT NULL COMMENT '输出格式信息',
  `info_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '种子哈希值',
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO友好的URL',
  `meta_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO标题',
  `meta_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'SEO描述',
  `meta_keywords` json DEFAULT NULL COMMENT 'SEO关键词',
  `published_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `videos_transcode_id_unique` (`transcode_id`),
  UNIQUE KEY `videos_md5_unique` (`md5`),
  UNIQUE KEY `videos_shareid_unique` (`shareid`),
  UNIQUE KEY `videos_slug_unique` (`slug`),
  KEY `videos_uid_status_index` (`uid`,`status`),
  KEY `videos_content_type_status_index` (`content_type`,`status`),
  KEY `videos_is_featured_status_index` (`is_featured`,`status`),
  KEY `videos_is_trending_status_index` (`is_trending`,`status`),
  KEY `videos_created_at_status_index` (`created_at`,`status`),
  KEY `videos_view_count_index` (`views_count`),
  KEY `videos_published_at_index` (`published_at`),
  KEY `videos_content_type_is_featured_index` (`content_type`,`is_featured`),
  KEY `videos_is_vip_only_status_index` (`is_vip_only`,`status`),
  KEY `videos_coin_price_status_index` (`coin_price`,`status`),
  KEY `videos_discount_start_at_discount_end_at_index` (`discount_start_at`,`discount_end_at`),
  KEY `videos_purchase_count_index` (`purchase_count`),
  KEY `videos_required_vip_level_index` (`required_vip_level`),
  KEY `videos_likes_count_index` (`likes_count`),
  KEY `idx_videos_user_status_type_updated` (`uid`,`status`,`video_type`,`updated_at`),
  KEY `idx_videos_views_status` (`views_count`,`status`),
  KEY `idx_videos_type_status_created` (`video_type`,`status`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频转码记录表（基于pronhub videos表结构）';

-- ----------------------------
-- Table structure for video_transcodes_backup
-- ----------------------------
DROP TABLE IF EXISTS `video_transcodes_backup`;
CREATE TABLE `video_transcodes_backup` (
  `id` bigint unsigned NOT NULL DEFAULT '0',
  `uid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传用户ID',
  `video_type` enum('short','long') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'short' COMMENT '视频类型：short=短视频, long=长视频',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频描述',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单次观看价格',
  `coin_price` int NOT NULL DEFAULT '0' COMMENT '金币价格',
  `points_price` int NOT NULL DEFAULT '0' COMMENT '积分价格',
  `discount_price` decimal(10,2) DEFAULT NULL COMMENT '优惠价格',
  `discount_coin_price` int DEFAULT NULL COMMENT '优惠金币价格',
  `discount_start_at` timestamp NULL DEFAULT NULL COMMENT '优惠开始时间',
  `discount_end_at` timestamp NULL DEFAULT NULL COMMENT '优惠结束时间',
  `creator_revenue_rate` decimal(5,4) NOT NULL DEFAULT '0.7000' COMMENT '创作者分成比例(0-1)',
  `required_vip_level` int NOT NULL DEFAULT '0' COMMENT '所需VIP等级(0=无要求)',
  `is_vip_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否VIP专享',
  `is_premium_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否高级会员专享',
  `transcode_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '转码系统返回的_id',
  `md5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件MD5值',
  `shareid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享ID',
  `orgfile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `rpath` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '相对路径',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主域名',
  `picdomain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片域名',
  `mp4domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MP4域名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频路径',
  `suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件后缀',
  `resolution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分辨率(如1920x1080)',
  `width` int NOT NULL DEFAULT '0' COMMENT '视频宽度',
  `height` int NOT NULL DEFAULT '0' COMMENT '视频高度',
  `views_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '播放次数',
  `likes_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '点赞数',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '踩数',
  `favorite_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '收藏数',
  `share_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '分享数',
  `download_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '下载次数',
  `purchase_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '购买次数',
  `total_revenue` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总收益',
  `daily_view_limit` int NOT NULL DEFAULT '0' COMMENT '每日观看限制(0=无限制)',
  `comments_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '评论数',
  `free_preview_duration` int NOT NULL DEFAULT '0' COMMENT '免费预览时长(秒)',
  `allow_preview` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许预览',
  `age_rating` int NOT NULL DEFAULT '0' COMMENT '年龄分级(0-21)',
  `duration` int NOT NULL DEFAULT '0' COMMENT '时长(秒)',
  `bitrate` int NOT NULL DEFAULT '0' COMMENT '码率(kbps)',
  `fps` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '帧率',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小(bytes)',
  `quality` enum('sd','hd','fhd','4k','8k') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'hd' COMMENT '视频质量',
  `allow_comments` tinyint(1) NOT NULL DEFAULT '1' COMMENT '允许评论',
  `allow_downloads` tinyint(1) NOT NULL DEFAULT '0' COMMENT '允许下载',
  `allowed_countries` json DEFAULT NULL COMMENT '允许观看的国家',
  `blocked_countries` json DEFAULT NULL COMMENT '禁止观看的国家',
  `language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en' COMMENT '视频语言',
  `subtitles` json DEFAULT NULL COMMENT '字幕信息',
  `transcode_result` enum('ok','failed','pending') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '转码结果',
  `transcode_begin` timestamp NULL DEFAULT NULL COMMENT '转码开始时间',
  `transcode_end` timestamp NULL DEFAULT NULL COMMENT '转码结束时间',
  `output_formats` json DEFAULT NULL COMMENT '输出格式信息',
  `info_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '种子哈希值',
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO友好的URL',
  `meta_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO标题',
  `meta_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'SEO描述',
  `meta_keywords` json DEFAULT NULL COMMENT 'SEO关键词',
  `published_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `thumbnails` json DEFAULT NULL COMMENT '缩略图列表',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `content_type` enum('free','ppv','subscription','vip','premium','coin','points') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'free' COMMENT '内容类型',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否精选',
  `is_editor_choice` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否编辑推荐',
  `is_hot` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_new` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否新品',
  `is_exclusive` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否独家',
  `is_trending` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否私密',
  `is_adult` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否成人内容',
  `status` enum('processing','completed','failed','published','draft','deleted') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'processing' COMMENT '视频状态',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for video_urls
-- ----------------------------
DROP TABLE IF EXISTS `video_urls`;
CREATE TABLE `video_urls` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '视频地址ID，主键',
  `video_id` int NOT NULL COMMENT '视频ID',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频标题（如：第1集、高清版等）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频描述',
  `video_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频播放地址URL',
  `episode_number` int DEFAULT NULL COMMENT '集数编号，用于多集视频排序',
  `quality` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频清晰度：480P,720P,1080P,4K等',
  `format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频格式：MP4,AVI,MKV,M3U8等',
  `duration` int DEFAULT NULL COMMENT '视频时长（秒）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `bitrate` int DEFAULT NULL COMMENT '视频码率（kbps）',
  `resolution` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分辨率：1920x1080,1280x720等',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为主要播放地址：1-是，0-否',
  `play_count` int NOT NULL DEFAULT '0' COMMENT '播放次数统计',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，数字越大排序越靠前',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-可用，0-不可用，-1-已删除',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频的多个播放地址信息，支持多集、多清晰度、多播放源';

-- ----------------------------
-- Table structure for videos
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '视频ID，主键',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频标题',
  `subtitle` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频副标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频详细描述',
  `video_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频内容/副文本',
  `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频封面图片URL',
  `duration` int DEFAULT NULL COMMENT '视频时长（秒）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频作者',
  `category_id` int DEFAULT NULL COMMENT '主分类ID',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '观看次数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数量',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数量',
  `share_count` int NOT NULL DEFAULT '0' COMMENT '分享次数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `is_recommended` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：1-是，0-否',
  `is_hot` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门：1-是，0-否',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-已发布，0-草稿，-1-已删除',
  `published_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `last_edited_at` timestamp NULL DEFAULT NULL COMMENT '最后编辑时间',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `transcode_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '转码任务ID',
  `md5` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件MD5值',
  `shareid` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分享ID',
  `orgfile` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  `rpath` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相对路径',
  `domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '域名',
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件路径',
  `suffix` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件后缀',
  `resolution` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分辨率',
  `bitrate` int DEFAULT NULL COMMENT '码率',
  `fps` decimal(8,2) DEFAULT NULL COMMENT '帧率',
  `width` int DEFAULT NULL COMMENT '视频宽度',
  `height` int DEFAULT NULL COMMENT '视频高度',
  `transcode_status` enum('processing','completed','failed') COLLATE utf8mb4_unicode_ci DEFAULT 'processing' COMMENT '转码状态',
  `thumbnails` json DEFAULT NULL COMMENT '缩略图列表',
  `output_formats` json DEFAULT NULL COMMENT '输出格式',
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'URL别名',
  `meta_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO标题',
  `meta_description` text COLLATE utf8mb4_unicode_ci COMMENT 'SEO描述',
  `meta_keywords` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO关键词',
  `video_type` enum('short','long') COLLATE utf8mb4_unicode_ci DEFAULT 'short' COMMENT '视频类型',
  `quality` enum('sd','hd','fhd','4k') COLLATE utf8mb4_unicode_ci DEFAULT 'hd' COMMENT '视频质量',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
  `allow_comments` tinyint(1) DEFAULT '1' COMMENT '允许评论',
  `language` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'zh' COMMENT '语言',
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`),
  KEY `idx_videos_transcode_status` (`transcode_status`),
  KEY `idx_videos_slug` (`slug`),
  KEY `idx_videos_video_type` (`video_type`),
  KEY `idx_videos_quality` (`quality`),
  KEY `idx_videos_featured` (`is_featured`),
  KEY `idx_videos_resolution` (`resolution`),
  KEY `idx_videos_status_type_quality` (`status`,`video_type`,`quality`),
  KEY `idx_videos_transcode_domain` (`transcode_status`,`domain`),
  KEY `idx_videos_status_published_created` (`status`,`published_at` DESC,`created_at` DESC),
  KEY `idx_videos_category_status_published` (`category_id`,`status`,`published_at` DESC),
  KEY `idx_videos_sort_published_created` (`sort_order` DESC,`published_at` DESC,`created_at` DESC),
  KEY `idx_videos_status_recommend_hot_sort` (`status`,`is_recommended`,`is_hot`,`sort_order`,`published_at`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储视频内容的详细信息';

-- ----------------------------
-- Table structure for videos_test
-- ----------------------------
DROP TABLE IF EXISTS `videos_test`;
CREATE TABLE `videos_test` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传用户ID',
  `video_type` enum('short','long') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'short' COMMENT '视频类型：short=短视频, long=长视频',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '视频描述',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单次观看价格',
  `coin_price` int NOT NULL DEFAULT '0' COMMENT '金币价格',
  `points_price` int NOT NULL DEFAULT '0' COMMENT '积分价格',
  `discount_price` decimal(10,2) DEFAULT NULL COMMENT '优惠价格',
  `discount_coin_price` int DEFAULT NULL COMMENT '优惠金币价格',
  `discount_start_at` timestamp NULL DEFAULT NULL COMMENT '优惠开始时间',
  `discount_end_at` timestamp NULL DEFAULT NULL COMMENT '优惠结束时间',
  `creator_revenue_rate` decimal(5,4) NOT NULL DEFAULT '0.7000' COMMENT '创作者分成比例(0-1)',
  `required_vip_level` int NOT NULL DEFAULT '0' COMMENT '所需VIP等级(0=无要求)',
  `is_vip_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否VIP专享',
  `is_premium_only` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否高级会员专享',
  `rpath` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '相对路径',
  `thumbnails` json DEFAULT NULL COMMENT '缩略图列表',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `content_type` enum('free','ppv','subscription','vip','premium','coin','points') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'free' COMMENT '内容类型',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否精选',
  `is_editor_choice` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否编辑推荐',
  `is_hot` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_new` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否新品',
  `is_exclusive` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否独家',
  `is_trending` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否热门',
  `is_private` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否私密',
  `is_adult` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否成人内容',
  `resolution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分辨率(如1920x1080)',
  `transcode_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '转码系统返回的_id',
  `md5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件MD5值',
  `shareid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享ID',
  `orgfile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主域名',
  `picdomain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片域名',
  `mp4domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MP4域名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频路径',
  `suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件后缀',
  `bitrate` int NOT NULL DEFAULT '0' COMMENT '码率(kbps)',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小(bytes)',
  `duration` int NOT NULL DEFAULT '0' COMMENT '时长(秒)',
  `fps` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '帧率',
  `width` int NOT NULL DEFAULT '0' COMMENT '视频宽度',
  `height` int NOT NULL DEFAULT '0' COMMENT '视频高度',
  `views_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '播放次数',
  `likes_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '点赞数',
  `dislike_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '踩数',
  `favorite_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '收藏数',
  `share_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '分享数',
  `download_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '下载次数',
  `purchase_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '购买次数',
  `total_revenue` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总收益',
  `daily_view_limit` int NOT NULL DEFAULT '0' COMMENT '每日观看限制(0=无限制)',
  `comments_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT '评论数',
  `free_preview_duration` int NOT NULL DEFAULT '0' COMMENT '免费预览时长(秒)',
  `allow_preview` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许预览',
  `age_rating` int NOT NULL DEFAULT '0' COMMENT '年龄分级(0-21)',
  `status` enum('processing','completed','failed','published','draft','deleted') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'processing' COMMENT '视频状态',
  `quality` enum('sd','hd','fhd','4k','8k') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'hd' COMMENT '视频质量',
  `allow_comments` tinyint(1) NOT NULL DEFAULT '1' COMMENT '允许评论',
  `allow_downloads` tinyint(1) NOT NULL DEFAULT '0' COMMENT '允许下载',
  `allowed_countries` json DEFAULT NULL COMMENT '允许观看的国家',
  `blocked_countries` json DEFAULT NULL COMMENT '禁止观看的国家',
  `language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en' COMMENT '视频语言',
  `subtitles` json DEFAULT NULL COMMENT '字幕信息',
  `transcode_begin` timestamp NULL DEFAULT NULL COMMENT '转码开始时间',
  `transcode_end` timestamp NULL DEFAULT NULL COMMENT '转码结束时间',
  `transcode_result` enum('ok','failed','pending') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '转码结果',
  `output_formats` json DEFAULT NULL COMMENT '输出格式信息',
  `info_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '种子哈希值',
  `slug` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO友好的URL',
  `meta_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SEO标题',
  `meta_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'SEO描述',
  `meta_keywords` json DEFAULT NULL COMMENT 'SEO关键词',
  `published_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分类每日点击明细统计表
CREATE TABLE IF NOT EXISTS `stats_daily_category_clicks` (
  `date`        DATE    NOT NULL COMMENT '统计日期',
  `category_id` BIGINT  NOT NULL COMMENT '分类ID',
  `clicks`      BIGINT  NOT NULL DEFAULT 0 COMMENT '当日点击次数',
  PRIMARY KEY (`date`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类每日点击明细统计';

-- 搜索关键词统计表（每日热词排行持久化）
CREATE TABLE IF NOT EXISTS `stats_search_keywords` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
  `stat_date`    DATE         NOT NULL COMMENT '统计日期',
  `keyword`      VARCHAR(200) NOT NULL COMMENT '搜索关键词（小写）',
  `search_count` BIGINT       NOT NULL DEFAULT 0 COMMENT '搜索次数',
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_date_keyword` (`stat_date`, `keyword`),
  INDEX `idx_date_count` (`stat_date`, `search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索关键词每日统计';

-- 广告每日点击/曝光统计表
CREATE TABLE IF NOT EXISTS `ad_statistics` (
  `id`               BIGINT       AUTO_INCREMENT PRIMARY KEY,
  `ad_id`            BIGINT       NOT NULL                COMMENT '广告ID',
  `ad_title`         VARCHAR(200) DEFAULT NULL            COMMENT '广告标题（冗余，避免关联查询）',
  `ad_type`          VARCHAR(10)  DEFAULT NULL            COMMENT '广告类型',
  `position`         VARCHAR(100) DEFAULT NULL            COMMENT '广告位置',
  `stat_date`        DATE         NOT NULL                COMMENT '统计日期',
  `click_count`      INT          NOT NULL DEFAULT 0      COMMENT '当日点击次数',
  `impression_count` INT          NOT NULL DEFAULT 0      COMMENT '当日曝光次数',
  `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ad_date` (`ad_id`, `stat_date`),
  INDEX `idx_stat_date` (`stat_date`),
  INDEX `idx_ad_type` (`ad_type`),
  INDEX `idx_position` (`position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告每日点击/曝光统计';

-- 为 stats_daily_overview 补充 new_users 列（若列已存在请跳过此语句）
ALTER TABLE `stats_daily_overview` ADD COLUMN `new_users` BIGINT NOT NULL DEFAULT 0 COMMENT '当日新增用户数';

-- 为已存在的 ad_statistics 表补充 impression_count 列（若列已存在请跳过此语句）
ALTER TABLE `ad_statistics` ADD COLUMN `impression_count` INT NOT NULL DEFAULT 0 COMMENT '当日曝光次数';

-- 广告类型字典：九宫格弹窗广告
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (7, '九宫格弹窗广告', '7', 'ad_type', '', 'default', 'N', '0', 'admin', NOW(), '九宫格弹窗广告，先展示单个弹窗广告后展示');

SET FOREIGN_KEY_CHECKS = 1;
