# 分类视频排序功能 - 数据库部署指南

## 📋 部署文件说明

### 1. 完整部署脚本
**文件**: `category_video_sort_deployment.sql`
- **适用场景**: 生产环境完整部署
- **包含内容**: 
  - 数据表创建
  - 系统菜单配置
  - 权限分配
  - 性能优化
  - 存储过程和视图
  - 完整性检查
- **执行时间**: 约2-3分钟
- **特点**: 功能完整，包含优化和验证

### 2. 简化部署脚本
**文件**: `category_video_sort_simple.sql`
- **适用场景**: 快速部署，测试环境
- **包含内容**: 
  - 核心数据表
  - 基础菜单配置
  - 基本权限分配
- **执行时间**: 约30秒
- **特点**: 简洁高效，包含核心功能

### 3. 回滚脚本
**文件**: `category_video_sort_rollback.sql`
- **适用场景**: 需要撤销功能时使用
- **包含内容**: 
  - 删除所有相关数据表
  - 清理菜单和权限
  - 验证清理结果
- **执行时间**: 约10秒
- **⚠️ 注意**: 此操作不可逆，请谨慎执行

## 🚀 部署步骤

### 方案一：完整部署（推荐生产环境）

```bash
# 1. 连接数据库
mysql -h your_host -u your_username -p your_database

# 2. 执行完整部署脚本
source /path/to/category_video_sort_deployment.sql;

# 3. 验证部署结果
SELECT * FROM category_video_sort LIMIT 1;
SELECT menu_name FROM sys_menu WHERE menu_name LIKE '%分类视频排序%';
```

### 方案二：快速部署（推荐测试环境）

```bash
# 1. 连接数据库
mysql -h your_host -u your_username -p your_database

# 2. 执行简化部署脚本
source /path/to/category_video_sort_simple.sql;

# 3. 重启应用服务
systemctl restart your-app-service
```

## ✅ 部署验证

### 1. 数据表验证
```sql
-- 检查表是否创建成功
SHOW TABLES LIKE 'category_video_sort';

-- 检查表结构
DESCRIBE category_video_sort;

-- 检查索引
SHOW INDEX FROM category_video_sort;
```

### 2. 菜单验证
```sql
-- 检查菜单是否创建
SELECT menu_id, menu_name, path, perms 
FROM sys_menu 
WHERE menu_name LIKE '%分类视频排序%' 
ORDER BY parent_id, order_num;
```

### 3. 权限验证
```sql
-- 检查权限分配
SELECT r.role_name, m.menu_name, m.perms
FROM sys_role r
JOIN sys_role_menu rm ON r.role_id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.perms LIKE '%categoryVideoSort%';
```

## 🔧 配置要求

### 数据库要求
- MySQL 5.7+ 或 MySQL 8.0+
- InnoDB 存储引擎
- utf8mb4 字符集支持

### 权限要求
- CREATE TABLE 权限
- INSERT/UPDATE/DELETE 权限
- INDEX 权限
- REFERENCES 权限（外键约束）

### 依赖表
确保以下表已存在：
- `categories` - 分类表
- `videos` - 视频表
- `sys_menu` - 系统菜单表
- `sys_role` - 角色表
- `sys_role_menu` - 角色菜单关联表

## 📊 功能特性

### 1. 排序逻辑
- **混合排序**: 手动排序 + 默认排序
- **权重机制**: `sort_order` 数值越大，排序越靠前
- **存储优化**: 仅存储需要特殊排序的视频

### 2. 性能优化
- **复合索引**: `(category_id, sort_order DESC)`
- **唯一约束**: `(category_id, video_id)`
- **外键约束**: 确保数据完整性
- **缓存支持**: 集成版本化缓存机制

### 3. 管理功能
- **拖拽排序**: 支持可视化拖拽调整
- **批量操作**: 支持批量设置排序
- **权限控制**: 基于角色的访问控制

## 🔄 升级和维护

### 定期维护
```sql
-- 清理孤立数据
DELETE cvs FROM category_video_sort cvs
LEFT JOIN categories c ON cvs.category_id = c.id
LEFT JOIN videos v ON cvs.video_id = v.id
WHERE c.id IS NULL OR v.id IS NULL;

-- 优化表性能
OPTIMIZE TABLE category_video_sort;

-- 检查表状态
CHECK TABLE category_video_sort;
```

### 性能监控
```sql
-- 查看排序统计
SELECT 
    category_id,
    COUNT(*) as sorted_videos,
    MAX(sort_order) as max_order,
    MIN(sort_order) as min_order
FROM category_video_sort 
GROUP BY category_id;
```

## 🆘 故障排除

### 常见问题

1. **外键约束错误**
   ```
   ERROR 1452: Cannot add or update a child row: a foreign key constraint fails
   ```
   **解决**: 确保 `categories` 和 `videos` 表存在且有对应的记录

2. **唯一约束冲突**
   ```
   ERROR 1062: Duplicate entry for key 'uk_category_video'
   ```
   **解决**: 检查是否重复插入相同的分类-视频组合

3. **菜单不显示**
   - 检查角色权限分配
   - 重启应用服务
   - 清理浏览器缓存

### 回滚操作
如需撤销部署：
```bash
mysql -h your_host -u your_username -p your_database < category_video_sort_rollback.sql
```

## 📞 技术支持

如遇到部署问题，请检查：
1. 数据库版本和权限
2. 依赖表是否存在
3. 字符集配置是否正确
4. 应用服务是否重启

---

**部署完成后，请重启应用服务以加载新功能！** 🎉
