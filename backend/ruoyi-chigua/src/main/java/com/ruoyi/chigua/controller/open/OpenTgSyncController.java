package com.ruoyi.chigua.controller.open;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.domain.TgPost;
import com.ruoyi.chigua.domain.TgMedia;
import com.ruoyi.chigua.domain.dto.TgIngestReq;
import com.ruoyi.chigua.domain.dto.TgMediaItem;
import com.ruoyi.chigua.mapper.TgPostMapper;

/**
 * TG 内容同步开放接口（无需鉴权）
 *
 * GET  /open/api/tg/check?sourceId=xxx  → 查询是否已同步
 * POST /open/api/tg/ingest              → 写入帖子 + 媒体
 */
@Anonymous
@RestController
@RequestMapping("/open/api/tg")
@CrossOrigin(origins = "*")
public class OpenTgSyncController
{
    private static final Logger log = LoggerFactory.getLogger(OpenTgSyncController.class);

    @Autowired
    private TgPostMapper tgPostMapper;

    /**
     * 查询 source_id 是否已入库
     *
     * 响应：
     *   { code:200, data:{ exists:false } }
     *   { code:200, data:{ exists:true, id:123 } }
     */
    @GetMapping("/check")
    public AjaxResult check(@RequestParam String sourceId)
    {
        if (!StringUtils.hasText(sourceId)) {
            return AjaxResult.error("sourceId 不能为空");
        }
        Long id = tgPostMapper.selectIdBySourceId(sourceId);
        if (id == null) {
            AjaxResult result = AjaxResult.success();
            result.put("exists", false);
            return result;
        }
        AjaxResult result = AjaxResult.success();
        result.put("exists", true);
        result.put("id", id);
        return result;
    }

    /**
     * 写入帖子及媒体
     *
     * 请求体示例：
     * {
     *   "categoryId": 5,
     *   "sourceId": "chan_001_12345",
     *   "caption": "今日更新",
     *   "postDate": "2026-03-07 10:00:00",
     *   "sortOrder": 0,
     *   "media": [
     *     {
     *       "mediaType": "photo",
     *       "localUrl": "https://cdn.example.com/img/001.jpg",
     *       "width": 1080, "height": 1440,
     *       "mimeType": "image/jpeg",
     *       "sortOrder": 0
     *     },
     *     {
     *       "mediaType": "video",
     *       "localUrl": "https://cdn.example.com/vid/001.mp4",
     *       "width": 720, "height": 1280,
     *       "duration": 66.0,
     *       "mimeType": "video/mp4",
     *       "thumbUrl": "https://cdn.example.com/thumb/001.jpg",
     *       "sortOrder": 1
     *     }
     *   ]
     * }
     *
     * 响应：
     *   { code:200, data:{ id:123, mediaCount:2 } }
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/ingest")
    public AjaxResult ingest(@RequestBody TgIngestReq req)
    {
        // ── 参数校验 ──
        if (req.getCategoryId() == null) {
            return AjaxResult.error("categoryId 不能为空");
        }
        if (!StringUtils.hasText(req.getSourceId())) {
            return AjaxResult.error("sourceId 不能为空");
        }
        if (req.getMedia() == null || req.getMedia().isEmpty()) {
            return AjaxResult.error("media 不能为空");
        }

        // ── 去重检查 ──
        Long existId = tgPostMapper.selectIdBySourceId(req.getSourceId());
        if (existId != null) {
            AjaxResult result = AjaxResult.success("已存在，跳过");
            result.put("id", existId);
            result.put("skipped", true);
            return result;
        }

        // ── 统计媒体数量 ──
        List<TgMediaItem> items = req.getMedia();
        int photoCount = 0, videoCount = 0;
        for (TgMediaItem item : items) {
            if ("photo".equals(item.getMediaType())) photoCount++;
            else if ("video".equals(item.getMediaType())) videoCount++;
        }

        // ── 写入帖子 ──
        TgPost post = new TgPost();
        post.setCategoryId(req.getCategoryId());
        post.setSourceId(req.getSourceId());
        post.setCaption(req.getCaption());
        post.setPostDate(req.getPostDate() != null ? req.getPostDate() : new Date());
        post.setMediaCount(items.size());
        post.setPhotoCount(photoCount);
        post.setVideoCount(videoCount);
        post.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        post.setStatus(1);
        tgPostMapper.insertTgPost(post);   // id 自动回填

        // ── 写入媒体列表 ──
        List<TgMedia> mediaList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            TgMediaItem item = items.get(i);
            TgMedia m = new TgMedia();
            m.setPostId(post.getId());
            m.setMediaType(item.getMediaType());
            m.setLocalPath(item.getLocalPath());
            m.setLocalUrl(item.getLocalUrl());
            m.setWidth(item.getWidth());
            m.setHeight(item.getHeight());
            m.setFileSize(item.getFileSize());
            m.setMimeType(item.getMimeType());
            m.setDuration(item.getDuration());
            m.setSupportsStreaming(item.getSupportsStreaming() != null ? item.getSupportsStreaming() : 0);
            m.setThumbUrl(item.getThumbUrl());
            m.setThumbWidth(item.getThumbWidth());
            m.setThumbHeight(item.getThumbHeight());
            m.setFirstFrameUrl(item.getFirstFrameUrl());
            m.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
            mediaList.add(m);
        }
        tgPostMapper.insertTgMediaBatch(mediaList);

        log.info("[TgIngest] 入库成功 sourceId={} postId={} media={}", req.getSourceId(), post.getId(), items.size());

        AjaxResult result = AjaxResult.success("入库成功");
        result.put("id", post.getId());
        result.put("mediaCount", items.size());
        return result;
    }
}
