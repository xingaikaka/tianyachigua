# Telegram（纸飞机）模块表结构文档

> 数据库：`chigua7`
> 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 存储引擎：`InnoDB`

## 一、表关系概览

```
tg_posts (帖子主表，一个媒体组=一条)
   │
   │ 1 : N
   │
   └── tg_media (媒体文件表，一张图/一个视频=一条)
```

- 一个 `tg_posts.id` 对应多条 `tg_media`，通过 `tg_media.post_id` 外键关联。
- 同步源 Telegram 中的"媒体组"（多张图 + 1 个视频）合并存为一条帖子。
- 通过 `tg_posts.category_id` 关联到通用 `category` 表（站点分类）。

---

## 二、`tg_posts`（帖子表）

> 一个媒体组 = 一条记录，承载帖子的标题/描述/统计/状态等元信息。

| 字段 | 类型 | 约束 / 默认 | 说明 |
|------|------|-----------|------|
| `id` | `int` | PK, AUTO_INCREMENT | 主键 |
| `category_id` | `int` | NULL | 关联站点 `category` 表的分类 ID |
| `source_id` | `varchar(200)` | UNIQUE | 同步源原始 ID（用于去重，写入前必须先查此字段） |
| `caption` | `text` | NULL | 帖子正文（标题/描述/标签合并） |
| `post_date` | `timestamp` | NOT NULL, 默认 `CURRENT_TIMESTAMP` | 内容发布时间（来自同步源） |
| `media_count` | `int` | NOT NULL, 默认 `0` | 该帖媒体总数（= `photo_count + video_count`） |
| `photo_count` | `int` | NOT NULL, 默认 `0` | 图片数量 |
| `video_count` | `int` | NOT NULL, 默认 `0` | 视频数量 |
| `views` | `int` | NOT NULL, 默认 `0` | 站内浏览次数（用户点击时累加） |
| `sort_order` | `int` | NOT NULL, 默认 `0` | 排序权重（越大越靠前） |
| `status` | `tinyint(1)` | NOT NULL, 默认 `1` | 状态：`1=显示`，`0=隐藏` |
| `is_top` | `tinyint(1)` | NOT NULL, 默认 `0` | 是否置顶：`1=是`，`0=否` |
| `created_at` | `timestamp` | NOT NULL, 默认 `CURRENT_TIMESTAMP` | 入库时间 |
| `updated_at` | `timestamp` | NOT NULL, 默认 `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 最后更新时间 |

### 索引

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `PRIMARY` | `id` | 主键 |
| `uk_source_id` | `source_id` (UNIQUE) | 同步去重保护 |
| `idx_category_id` | `category_id` | 按分类筛选 |
| `idx_post_date` | `post_date` | 按时间倒序 |
| `idx_status_sort` | `status, sort_order, post_date` | 复合索引：列表页主查询（"显示中 + 权重 + 时间"） |

### 业务约定

- **去重**：同步工具每次写入前必须 `SELECT id FROM tg_posts WHERE source_id = ?`，命中则更新，未命中再插入。
- **删除策略**：使用软删除 `status=0`，不物理删除。
- **置顶 + 排序**：列表查询 SQL 推荐 `ORDER BY is_top DESC, sort_order DESC, post_date DESC`。

---

## 三、`tg_media`（媒体文件表）

> 每张图片/每个视频一条记录，与 `tg_posts` 一对多。

| 字段 | 类型 | 约束 / 默认 | 说明 |
|------|------|-----------|------|
| `id` | `int` | PK, AUTO_INCREMENT | 主键 |
| `post_id` | `int` | NOT NULL | 关联 `tg_posts.id` |
| `media_type` | `enum('photo','video')` | NOT NULL | 媒体类型 |
| `local_path` | `varchar(500)` | NULL | 服务器本地文件路径（如 `/files/images/2026/03/25/xxx.jpg`） |
| `local_url` | `varchar(500)` | NULL | 前端可访问 URL（图片/视频直链，会被 `ChiguaUrlService` 包装签名） |
| `width` | `int` | NULL | 宽度（像素） |
| `height` | `int` | NULL | 高度（像素） |
| `file_size` | `bigint` | NULL | 文件大小（字节） |
| `mime_type` | `varchar(50)` | NULL | MIME 类型（如 `image/jpeg`、`video/mp4`） |
| `duration` | `decimal(10,3)` | NULL | 视频时长（秒，支持小数） |
| `supports_streaming` | `tinyint(1)` | 默认 `0` | 是否支持流媒体播放：`1=是`，`0=否` |
| `thumb_url` | `varchar(500)` | NULL | 封面图 / 缩略图 URL |
| `first_frame_url` | `varchar(500)` | NULL | 视频首帧图片 URL |
| `thumb_width` | `int` | NULL | 封面图宽度 |
| `thumb_height` | `int` | NULL | 封面图高度 |
| `sort_order` | `int` | NOT NULL, 默认 `0` | 帖子内显示顺序（从 0 开始正序） |
| `is_recommend` | `tinyint(1)` | NOT NULL, 默认 `0` | 是否推荐：`1=是`，`0=否` |
| `created_at` | `timestamp` | NOT NULL, 默认 `CURRENT_TIMESTAMP` | 入库时间 |

### 索引

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `PRIMARY` | `id` | 主键 |
| `idx_post_id` | `post_id` | 按帖子拉取媒体列表（详情页查询） |

### 业务约定

- **图片**：通常只有 `local_url`、`width`、`height`、`file_size`、`mime_type`；`duration / supports_streaming / first_frame_url / thumb_*` 全部为空。
- **视频**：必须填 `duration`、`first_frame_url`（首帧）；`thumb_url` 可作为封面（移动端展示更轻）；建议 `supports_streaming=1` 以启用 HLS。
- **加密 / CDN**：`local_url` 字段存的是相对路径，前端展示前会经 `ChiguaUrlService` 根据 IP 路由（国内→腾讯 CDN，国外→Cloudflare Worker）并加 HMAC 签名。

---

## 四、典型查询示例

### 1. 列表页（按分类拉取已显示的帖子）

```sql
SELECT *
FROM tg_posts
WHERE category_id = 28
  AND status = 1
ORDER BY is_top DESC, sort_order DESC, post_date DESC
LIMIT 20 OFFSET 0;
```

### 2. 详情页（取一条帖子 + 全部媒体）

```sql
SELECT * FROM tg_posts WHERE id = 1011;
SELECT *
FROM tg_media
WHERE post_id = 1011
ORDER BY sort_order ASC;
```

### 3. 站点地图全量导出（仅 status=1）

```sql
SELECT id, updated_at FROM tg_posts
WHERE status = 1
ORDER BY id ASC;
```

### 4. 同步去重（写入前必查）

```sql
SELECT id FROM tg_posts WHERE source_id = ?;
-- 命中 → UPDATE；未命中 → INSERT
```

---

## 五、当前数据量统计（2026-05-23）

| 表 | 总行数 | 备注 |
|----|------|------|
| `tg_posts` | **1,663** | 其中 `status=1` 显示 `1,660`，`status=0` 隐藏 `3` |
| `tg_media` | **7,589** | 其中 `photo` 2,624、`video` 4,965 |

平均每条帖子约 **4.56** 个媒体（接近 Telegram 媒体组上限 10）。

---

## 六、相关代码位置

| 文件 | 作用 |
|------|------|
| `backend/sql/tg_tables.sql` | 建表 DDL |
| `backend/sql/add_is_top_and_is_recommend.sql` | 后续添加 `is_top` / `is_recommend` 字段的迁移脚本 |
| `backend/ruoyi-chigua/src/main/java/com/ruoyi/chigua/domain/TgPost.java` | 帖子领域对象 |
| `backend/ruoyi-chigua/src/main/java/com/ruoyi/chigua/domain/TgMedia.java` | 媒体领域对象 |
| `backend/ruoyi-chigua/src/main/java/com/ruoyi/chigua/mapper/TgPostMapper.java` | MyBatis Mapper |
| `backend/ruoyi-chigua/src/main/resources/mapper/chigua/TgPostMapper.xml` | SQL 映射 |
| `backend/ruoyi-chigua/src/main/java/com/ruoyi/chigua/service/TgSitemapService.java` | 站点地图生成 |
| `backend/ruoyi-chigua/src/main/java/com/ruoyi/chigua/controller/web/WebSitemapController.java` | 站点地图 HTTP 接口 |
