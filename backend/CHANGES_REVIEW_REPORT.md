# 分类视频排序改造 - 完整性与正确性检查报告

## 一、改动完整性 ✅

### 1. 用户端视频列表调用链（已全部移除 category_video_sort）

| 入口 | 调用链 | 状态 |
|------|--------|------|
| `/web/api/category/videos/optimized` | WebCategoryController → WebCategoryServiceImpl.selectWebVideoListOptimized → getCachedBaseVideos / getCachedVideoTotal | ✅ 已移除混合排序 |
| getCachedBaseVideos | → videoMapper.selectCategoryVideoPage (无过滤) / selectShortVideoPage (有过滤) | ✅ |
| getCachedVideoTotal | → videoMapper.countCategoryVideoPage / countShortVideoPage | ✅ |
| VideoServiceImpl.getCachedVideoListOnlyPaged | → videoMapper.selectVideoListOnlyPaged | ✅ |
| VideoServiceImpl.selectWebVideoListBasicPagedWithSignedUrls | → getCachedVideoListBasicPaged → videoMapper.selectWebVideoListBasicPaged | ✅ |
| VideoServiceImpl.getCachedCategoryVideoListWithMixedSort | → videoMapper.selectWebVideoListBasicPaged（已改为走 mapper） | ✅ |

### 2. 已修改的 SQL 排序（ORDER BY）

| Mapper 方法 | 文件位置 | 新排序 |
|-------------|----------|--------|
| selectCategoryVideoPage | VideoMapper.xml:440 | ✅ is_recommended, is_hot, sort_order, published_at, created_at |
| selectShortVideoPage | VideoMapper.xml:412 | ✅ 同上 |
| selectVideoListOnlyPaged | VideoMapper.xml:1372 | ✅ 同上 |
| selectWebVideoListBasicPaged | VideoMapper.xml:562 | ✅ 同上 |

### 3. 未修改的 SQL（符合预期）

| Mapper 方法 | 用途 | 说明 |
|-------------|------|------|
| selectWebVideoListBasic | 后台管理列表 | 非用户端，保持原样 |
| selectVideoListForSearch / selectVideoListForSearchPaged | 搜索 | 可选：若希望搜索结果也按推荐/热门排序，可后续统一 |

### 4. 保留的后台能力 ✅

- CategoryVideoSortController、CategoryVideoSortServiceImpl、CategoryVideoSortMapper 完整保留
- 后台管理仍可使用 category_video_sort 手动排序
- 用户端已不再依赖 category_video_sort

---

## 二、正确性检查 ✅

### 1. getCachedVideoListOnlyPaged 逻辑

```java
Boolean excludeShort = (video != null && video.getCategoryId() == null) ? Boolean.TRUE : null;
```

- `video == null`：excludeShort = null，mapper 中 `excludeShort == true` 为 false，不排除短视频
- `video.categoryId == null`（全部视频）：excludeShort = TRUE ✅
- `video.categoryId != null`（分类查询）：excludeShort = null ✅

**注意**：若存在 `video == null` 的调用，当前会不排除短视频；当前代码中未见此类调用，可保持现状。

### 2. selectVideoListOnlyPaged 的 excludeShort

Mapper 条件：`video.categoryId == null and excludeShort == true`

- 仅当「全部视频」且 `excludeShort == true` 时排除短视频
- 分类查询时不会触发该条件 ✅

### 3. 缓存一致性

- `getCachedCategoryVideoListWithMixedSort` 已从 `categoryVideoSort` 改为 `videoList` 缓存
- 旧 `categoryVideoSort` 缓存键 `mixed_sort_*` 不再被写入，仅后台操作会清理
- 建议：部署后如使用 Redis，可执行 `KEYS categoryVideoSort:*` 检查并视情况清理

---

## 三、SQL 性能分析

### 1. 当前索引情况

**videos 表**（chigua.sql）：
- 有：`idx_videos_status_type_quality`, `idx_videos_transcode_domain` 等
- 缺：针对 `(status, is_recommended, is_hot, sort_order, published_at, created_at)` 的复合索引

**video_category_tag_relations 表**：
- 仅有主键
- 缺：`(video_id, relation_type, category_id)` 等用于 EXISTS 的索引

### 2. 查询特征

- `WHERE status = 1` + 多种 EXISTS 子查询 + `ORDER BY is_recommended, is_hot, sort_order, published_at, created_at`
- 无合适索引时，易出现：全表扫描、filesort、EXISTS 子查询效率低

### 3. 推荐索引（已生成）

已生成 `sql/idx_video_list_sort_performance.sql`：

```sql
-- videos：status + 排序字段
CREATE INDEX idx_videos_status_recommended_hot_sort 
ON videos (status, is_recommended, is_hot, sort_order, published_at, created_at);

-- video_category_tag_relations：EXISTS 优化
CREATE INDEX idx_vctr_video_relation_category 
ON video_category_tag_relations (video_id, relation_type, category_id);

CREATE INDEX idx_vctr_video_relation_tag 
ON video_category_tag_relations (video_id, relation_type, tag_id);
```

### 4. 验证方式

```sql
EXPLAIN SELECT v.id, v.title, ... 
FROM videos v 
WHERE v.status = 1 
  AND EXISTS (SELECT 1 FROM video_category_tag_relations vctr WHERE vctr.video_id = v.id AND vctr.relation_type = 1 AND vctr.category_id = 1)
ORDER BY v.is_recommended DESC, v.is_hot DESC, v.sort_order DESC, v.published_at DESC, v.created_at DESC
LIMIT 0, 20;
```

关注：`type`（避免 ALL）、`Extra`（避免 Using filesort）、`key`（是否使用新索引）。

---

## 四、总结

| 项目 | 结论 |
|------|------|
| 完整性 | ✅ 用户端视频列表已全部移除 category_video_sort 依赖 |
| 正确性 | ✅ 排序逻辑与 excludeShort 行为正确 |
| SQL 性能 | ⚠️ 建议执行 `idx_video_list_sort_performance.sql` 添加索引 |

**建议操作**：
1. 执行 `sql/idx_video_list_sort_performance.sql` 创建索引
2. 用 EXPLAIN 验证关键列表查询的执行计划
3. 部署后做一次分类列表、首页、全部视频的回归测试
