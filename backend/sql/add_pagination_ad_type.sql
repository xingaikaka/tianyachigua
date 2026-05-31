-- 添加分页模式广告类型字典数据
-- 添加时间: 2025-12-08
-- 说明: 为广告类型(ad_type)添加分页模式广告选项

-- 插入广告类型字典数据：分页模式广告
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
VALUES (6, '分页模式广告', '6', 'ad_type', NULL, 'default', 'N', '0', 'admin', NOW(), '用于分页模式分类的广告');

-- 如果短视频广告类型(5)不存在，也一并添加
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 5, '短视频广告', '5', 'ad_type', NULL, 'default', 'N', '0', 'admin', NOW(), '用于短视频分类的广告'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'ad_type' AND `dict_value` = '5');

