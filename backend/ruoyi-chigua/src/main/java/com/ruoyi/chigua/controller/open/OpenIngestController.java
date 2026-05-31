package com.ruoyi.chigua.controller.open;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.dto.SubmitVideoReq;
import com.ruoyi.chigua.domain.dto.Cg51IngestReq;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.IVideoKeyService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.mapper.TagMapper;
import com.ruoyi.chigua.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Anonymous
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OpenIngestController {

    private static final Logger logger = LoggerFactory.getLogger(OpenIngestController.class);

    private final IVideoService videoService;
    private final ICategoryService categoryService;
    private final IVideoKeyService videoKeyService;
    private final ChiguaUrlService chiguaUrlService;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;

    @PostMapping("/submit-video")
    public AjaxResult submit(@RequestBody SubmitVideoReq req) {
        // 校验最小必填
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            return AjaxResult.error("title 必填");
        }
        if (req.getCategoryId() == null) {
            return AjaxResult.error("categoryId 必填");
        }

        String title = cleanTitle(req.getTitle().trim());
        Long categoryId = req.getCategoryId();
        String coverImage = req.getCoverImage();

        // 1) 入库 videos 基础字段
        Video v = new Video();
        v.setTitle(title);
        v.setDescription(title);
        v.setVideoContent(buildRichContent(title, req.getAuthor(), req.getTags(), coverImage));
        if (coverImage != null && !coverImage.isEmpty()) {
            v.setCoverImage(coverImage);
        }
        if (req.getAuthor() != null) {
            v.setAuthor(req.getAuthor());
        }
        v.setCategoryId(categoryId);
        v.setStatus(1);
        v.setViewCount(0); v.setCommentCount(0); v.setLikeCount(0); v.setShareCount(0); v.setSortOrder(0);
        Date now = new Date();
        v.setPublishedAt(now); v.setCreatedAt(now); v.setUpdatedAt(now);

        // 分类关系
        v.setCategoryIds(Collections.singletonList(categoryId));

        // 标签处理：查无则建、有则用；统一建立关系
        List<Long> tagIds = new ArrayList<>();
        if (req.getTags() != null) {
            for (String tagName : req.getTags()) {
                if (tagName == null) continue;
                String name = tagName.trim();
                if (name.isEmpty()) continue;
                // 先查唯一
                Tag exist = tagMapper.checkTagNameUnique(name);
                if (exist == null) {
                    Tag t = new Tag();
                    t.setName(name);
                    tagMapper.insertTag(t);
                    tagIds.add(t.getId());
                } else {
                    tagIds.add(exist.getId());
                }
            }
        }
        v.setTagIds(tagIds);

        int inserted = videoService.insertVideo(v);
        if (inserted > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("videoId", v.getId());
            return AjaxResult.success(data);
        } else {
            return AjaxResult.error("入库失败");
        }
    }

    private String cleanTitle(String raw) {
        String cleaned = raw.replaceAll("[<>:\"/\\|?*]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 300) cleaned = cleaned.substring(0, 300);
        return cleaned;
    }

    private String buildRichContent(String title, String author, List<String> tags, String cover) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>").append(title).append("</p>");
        if (author != null && !author.isEmpty()) {
            sb.append("<p>来源用户: @").append(author).append("</p>");
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("<p>标签: ");
            for (String t : tags) {
                if (t != null && !t.isEmpty()) sb.append("#").append(t).append(" ");
            }
            sb.append("</p>");
        }
        if (cover != null && !cover.isEmpty()) {
            sb.append("<p><img data-resource-key=\"").append(cover).append("\" src=\"")
              .append(cover).append("\" alt=\"封面\"></p>");
        }
        return sb.toString();
    }

    /**
     * 获取所有分类列表（用于51吃瓜爬虫判断分类）
     * GET /api/cg51/categories
     */
    @GetMapping("/cg51/categories")
    public AjaxResult getCategories() {
        try {
            Category query = new Category();
            query.setStatus(1); // 只查询启用的分类
            List<Category> categories = categoryService.selectCategoryList(query);
            
            // 转换为简单的Map格式，方便爬虫使用
            List<Map<String, Object>> result = categories.stream()
                .map(cat -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", cat.getId());
                    map.put("name", cat.getName());
                    return map;
                })
                .collect(Collectors.toList());
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据分类名称列表查询分类ID列表
     * POST /api/cg51/categories/match
     * Body: {"categoryNames": ["今日吃瓜", "网红黑料"]}
     */
    @PostMapping("/cg51/categories/match")
    public AjaxResult matchCategories(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> categoryNames = request.get("categoryNames");
            if (categoryNames == null || categoryNames.isEmpty()) {
                return AjaxResult.success(Collections.emptyList());
            }

            List<Long> matchedIds = new ArrayList<>();
            for (String name : categoryNames) {
                if (StringUtils.hasText(name)) {
                    Category exist = categoryMapper.checkCategoryNameUnique(name.trim());
                    if (exist != null && exist.getStatus() != null && exist.getStatus() == 1) {
                        matchedIds.add(exist.getId());
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("categoryIds", matchedIds);
            result.put("matchedCount", matchedIds.size());
            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("匹配分类失败: " + e.getMessage());
        }
    }

    /**
     * 51吃瓜视频入库接口
     * POST /api/cg51/ingest
     */
    @PostMapping("/cg51/ingest")
    public AjaxResult ingestCg51Video(@RequestBody Cg51IngestReq req) {
        try {
            // 校验必填字段
            if (!StringUtils.hasText(req.getTitle())) {
                return AjaxResult.error("title 必填");
            }
            if (req.getCategoryId() == null && (req.getCategoryIds() == null || req.getCategoryIds().isEmpty())) {
                return AjaxResult.error("categoryId 或 categoryIds 必填");
            }

            // 🔧 新增：检查sourceId是否已存在
            if (StringUtils.hasText(req.getSourceId())) {
                String trimmedSourceId = req.getSourceId().trim();
                List<String> sourceIdsToCheck = new ArrayList<>();
                sourceIdsToCheck.add(trimmedSourceId);
                
                // 使用专门的批量查询方法，确保精确匹配 sourceId
                List<Video> existingVideos = videoService.selectVideosBySourceIds(sourceIdsToCheck);
                
                if (existingVideos != null && !existingVideos.isEmpty()) {
                    // 视频已存在，返回已存在的视频信息
                    Video existingVideo = existingVideos.get(0);
                    Map<String, Object> data = new HashMap<>();
                    data.put("videoId", existingVideo.getId());
                    data.put("title", existingVideo.getTitle());
                    data.put("sourceId", existingVideo.getSourceId());
                    data.put("exists", true);  // 标记为已存在
                    data.put("message", "视频已存在（通过sourceId匹配）");
                    return AjaxResult.success(data);
                }
            }

            // 处理分类ID
            List<Long> categoryIds = new ArrayList<>();
            if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
                categoryIds.addAll(req.getCategoryIds());
            } else if (req.getCategoryId() != null) {
                categoryIds.add(req.getCategoryId());
            }

            // 如果提供了分类名称列表，尝试匹配
            if (req.getCategoryNames() != null && !req.getCategoryNames().isEmpty()) {
                for (String name : req.getCategoryNames()) {
                    if (StringUtils.hasText(name)) {
                        Category exist = categoryMapper.checkCategoryNameUnique(name.trim());
                        if (exist != null && exist.getStatus() != null && exist.getStatus() == 1) {
                            if (!categoryIds.contains(exist.getId())) {
                                categoryIds.add(exist.getId());
                            }
                        }
                    }
                }
            }

            // 如果没有找到任何分类，使用默认分类ID（如果提供了）
            if (categoryIds.isEmpty() && req.getCategoryId() != null) {
                categoryIds.add(req.getCategoryId());
            }

            if (categoryIds.isEmpty()) {
                return AjaxResult.error("未找到匹配的分类，请提供有效的categoryId或categoryNames");
            }

            // 创建Video对象
            Video v = new Video();
            v.setTitle(cleanTitle(req.getTitle().trim()));
            v.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription() : v.getTitle());
            
            // 设置视频来源ID
            if (StringUtils.hasText(req.getSourceId())) {
                v.setSourceId(req.getSourceId());
            }
            
            // 使用提供的videoContent（富文本HTML），如果没有则使用contentHtml
            if (StringUtils.hasText(req.getVideoContent())) {
                v.setVideoContent(req.getVideoContent());
            } else if (StringUtils.hasText(req.getContentHtml())) {
                v.setVideoContent(req.getContentHtml());
            } else {
                v.setVideoContent(buildRichContent(v.getTitle(), req.getUsername(), req.getTags(), req.getCoverImage()));
            }

            // 设置first_video_url
            if (StringUtils.hasText(req.getFirstVideoUrl())) {
                v.setFirstVideoUrl(req.getFirstVideoUrl());
            }

            // 设置first_frame_url（首帧图片）
            if (StringUtils.hasText(req.getFirstFrameUrl())) {
                v.setFirstFrameUrl(req.getFirstFrameUrl());
            }

            // 设置视频时长
            if (req.getDuration() != null) {
                v.setDuration(req.getDuration());
            }

            // 设置封面
            if (StringUtils.hasText(req.getCoverImage())) {
                v.setCoverImage(req.getCoverImage());
            }

            // 设置作者
            if (StringUtils.hasText(req.getUsername())) {
                v.setAuthor(req.getUsername());
            }

            // 设置主分类ID（使用第一个分类ID）
            v.setCategoryId(categoryIds.get(0));
            v.setCategoryIds(categoryIds);

            // 设置状态和统计信息
            v.setStatus(1);
            v.setViewCount(0);
            v.setCommentCount(0);
            v.setLikeCount(0);
            v.setShareCount(0);
            v.setSortOrder(0);

            // 设置时间
            Date now = new Date();
            if (req.getPublishedAt() != null) {
                v.setPublishedAt(req.getPublishedAt());
            } else {
                v.setPublishedAt(now);
            }
            v.setCreatedAt(now);
            v.setUpdatedAt(now);

            // 处理标签
            List<Long> tagIds = new ArrayList<>();
            if (req.getTags() != null) {
                for (String tagName : req.getTags()) {
                    if (!StringUtils.hasText(tagName)) continue;
                    String name = tagName.trim();
                    if (name.isEmpty()) continue;
                    
                    // 先查唯一
                    Tag exist = tagMapper.checkTagNameUnique(name);
                    if (exist == null) {
                        Tag t = new Tag();
                        t.setName(name);
                        tagMapper.insertTag(t);
                        tagIds.add(t.getId());
                    } else {
                        tagIds.add(exist.getId());
                    }
                }
            }
            v.setTagIds(tagIds);

            // 插入视频
            int inserted = videoService.insertVideo(v);
            if (inserted > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("videoId", v.getId());
                data.put("title", v.getTitle());
                data.put("sourceId", v.getSourceId());
                data.put("categoryIds", categoryIds);
                data.put("exists", false);
                data.put("message", "视频入库成功");

                // 若携带加密 key，入库并返回签名访问路径
                if (StringUtils.hasText(req.getKeyHex())) {
                    boolean keySaved = videoKeyService.saveKey(
                            v.getId(),
                            req.getSourceId(),
                            req.getKeyHex(),
                            req.getKeyIv()
                    );
                    if (keySaved) {
                        String keyPath = chiguaUrlService.generateKeyPath(v.getId());
                        data.put("keyPath", keyPath);
                        logger.info("[Ingest] 密钥入库成功，keyPath={}", keyPath);
                    } else {
                        logger.warn("[Ingest] 密钥入库失败，videoId={}", v.getId());
                        data.put("keyPath", null);
                    }
                }

                return AjaxResult.success(data);
            } else {
                return AjaxResult.error("视频入库失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("视频入库异常: " + e.getMessage());
        }
    }

    /**
     * 单独存储视频加密密钥
     * POST /api/cg51/key/store
     * Body: { "videoId": 123, "keyHex": "aabbcc...", "keyIv": "0x419b..." }
     *
     * 由爬虫在 processM3U8 完成后调用，将 key 存入数据库，
     * 与 /open/key/{videoId} 接口配合使用。
     */
    @PostMapping("/cg51/key/store")
    public AjaxResult storeVideoKey(@RequestBody Map<String, Object> request) {
        try {
            Object videoIdObj = request.get("videoId");
            String keyHex     = (String) request.get("keyHex");
            String keyIv      = (String) request.get("keyIv");
            String sourceId   = (String) request.get("sourceId");

            if (!StringUtils.hasText(keyHex)) {
                return AjaxResult.error("keyHex 必填");
            }
            if (!StringUtils.hasText(sourceId)) {
                return AjaxResult.error("sourceId 必填");
            }

            // videoId 允许为 null（爬虫在 storeKey 时可能尚未获得 videoId）
            Long videoId = (videoIdObj != null) ? Long.valueOf(videoIdObj.toString()) : null;
            boolean saved = videoKeyService.saveKey(videoId, sourceId, keyHex, keyIv);

            if (saved) {
                Map<String, Object> data = new HashMap<>();
                data.put("sourceId", sourceId);
                if (videoId != null) {
                    data.put("videoId", videoId);
                }
                return AjaxResult.success(data);
            } else {
                return AjaxResult.error("密钥保存失败（可能已存在或格式不正确）");
            }
        } catch (Exception e) {
            return AjaxResult.error("密钥保存异常: " + e.getMessage());
        }
    }

    /**
     * 根据标题检查视频是否已存在
     * POST /api/cg51/videos/check
     * Body: {"title": "视频标题"}
     * Response: {"code": 200, "data": {"exists": true, "videoId": 123}}
     */
    @PostMapping("/cg51/videos/check")
    public AjaxResult checkVideoExists(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            if (!StringUtils.hasText(title)) {
                return AjaxResult.error("title 必填");
            }

            // 清理标题（与入库时保持一致）
            String cleanedTitle = cleanTitle(title.trim());

            // 使用精确匹配查询视频（查询所有状态的视频）
            Video query = new Video();
            query.setTitle(cleanedTitle);
            // 不设置status，查询所有状态的视频
            List<Video> videos = videoService.selectVideoList(query);

            Map<String, Object> result = new HashMap<>();
            if (videos != null && !videos.isEmpty()) {
                // 找到匹配的视频，返回第一个视频的ID
                Video foundVideo = videos.get(0);
                result.put("exists", true);
                result.put("videoId", foundVideo.getId());
            } else {
                // 未找到匹配的视频
                result.put("exists", false);
            }

            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("检查视频是否存在异常: " + e.getMessage());
        }
    }

    /**
     * 根据source_id批量检查视频是否已存在，支持可选的title二次判断
     * POST /api/cg51/videos/check-batch
     *
     * 请求格式一（旧格式，向后兼容，仅按sourceId判断）：
     *   {"sourceIds": ["id1", "id2"]}
     *
     * 请求格式二（新格式，支持按title补充判断）：
     *   {"items": [{"sourceId": "id1", "title": "可选标题"}, {"sourceId": "id2"}]}
     *
     * 判断逻辑：
     *   1. 优先按 sourceId 精确匹配
     *   2. sourceId 未命中且传入了 title，则再按 title 精确匹配
     *   3. 任一命中即视为已存在（matchedBy: "sourceId" 或 "title"）
     */
    @PostMapping("/cg51/videos/check-batch")
    public AjaxResult checkVideosExistBatch(@RequestBody Map<String, Object> request) {
        try {
            // ---- 解析请求，兼容旧格式(sourceIds)和新格式(items) ----
            // 每个检查项：sourceId（必填）+ title（可选）
            List<String> checkSourceIds = new ArrayList<>();
            List<String> checkTitles = new ArrayList<>(); // 与 checkSourceIds 一一对应，无title时为null

            Object itemsObj = request.get("items");
            if (itemsObj instanceof List) {
                // 新格式：items 列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                for (Map<String, Object> item : items) {
                    Object sid = item.get("sourceId");
                    if (sid == null || !StringUtils.hasText(sid.toString())) continue;
                    checkSourceIds.add(sid.toString().trim());
                    Object t = item.get("title");
                    checkTitles.add((t != null && StringUtils.hasText(t.toString())) ? t.toString().trim() : null);
                }
            } else {
                // 旧格式：sourceIds 字符串列表
                @SuppressWarnings("unchecked")
                List<String> sourceIds = (List<String>) request.get("sourceIds");
                if (sourceIds == null || sourceIds.isEmpty()) {
                    return AjaxResult.error("sourceIds 必填且不能为空");
                }
                for (String sid : sourceIds) {
                    if (!StringUtils.hasText(sid)) continue;
                    checkSourceIds.add(sid.trim());
                    checkTitles.add(null); // 旧格式不带title
                }
            }

            if (checkSourceIds.isEmpty()) {
                return AjaxResult.error("没有有效的 sourceId");
            }

            // ---- 第一步：按 sourceId 批量查询 ----
            List<Video> videosBySourceId = videoService.selectVideosBySourceIds(checkSourceIds);
            Map<String, Video> sourceIdToVideoMap = new HashMap<>();
            for (Video v : videosBySourceId) {
                if (v.getSourceId() != null) {
                    sourceIdToVideoMap.put(v.getSourceId(), v);
                }
            }

            // ---- 第二步：收集 sourceId 未命中但有 title 的项，批量按 title 查询 ----
            List<String> titlesToQuery = new ArrayList<>();
            for (int i = 0; i < checkSourceIds.size(); i++) {
                String sid = checkSourceIds.get(i);
                String title = checkTitles.get(i);
                if (!sourceIdToVideoMap.containsKey(sid) && title != null) {
                    titlesToQuery.add(title);
                }
            }

            Map<String, Video> titleToVideoMap = new HashMap<>();
            if (!titlesToQuery.isEmpty()) {
                List<Video> videosByTitle = videoService.selectVideosByTitles(titlesToQuery);
                for (Video v : videosByTitle) {
                    if (v.getTitle() != null) {
                        // 同一title可能有多条，保留首条即可
                        titleToVideoMap.putIfAbsent(v.getTitle(), v);
                    }
                }
            }

            // ---- 第三步：构建返回结果 ----
            List<Map<String, Object>> results = new ArrayList<>();
            int existsCount = 0;
            int notExistsCount = 0;

            for (int i = 0; i < checkSourceIds.size(); i++) {
                String sourceId = checkSourceIds.get(i);
                String title = checkTitles.get(i);

                Map<String, Object> resultItem = new HashMap<>();
                resultItem.put("sourceId", sourceId);
                if (title != null) {
                    resultItem.put("title", title);
                }

                Video foundVideo = sourceIdToVideoMap.get(sourceId);
                String matchedBy = null;

                if (foundVideo != null) {
                    matchedBy = "sourceId";
                } else if (title != null) {
                    foundVideo = titleToVideoMap.get(title);
                    if (foundVideo != null) {
                        matchedBy = "title";
                    }
                }

                if (foundVideo != null) {
                    resultItem.put("exists", true);
                    resultItem.put("videoId", foundVideo.getId());
                    resultItem.put("matchedBy", matchedBy);
                    if (foundVideo.getTitle() != null) {
                        resultItem.put("existingTitle", foundVideo.getTitle());
                    }
                    existsCount++;
                } else {
                    resultItem.put("exists", false);
                    notExistsCount++;
                }

                results.add(resultItem);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("existsCount", existsCount);
            data.put("notExistsCount", notExistsCount);
            data.put("totalCount", results.size());

            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error("批量检查视频是否存在异常: " + e.getMessage());
        }
    }
}
