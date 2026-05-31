package com.ruoyi.chigua.service.web.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.vo.web.WebVideoVO;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.service.ITagService;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.web.IWebTagsService;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * Web端标签Service业务层处理
 */
@Service
public class WebTagsServiceImpl implements IWebTagsService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebTagsServiceImpl.class);
    
    @Autowired
    private ITagService tagService;
    
    @Autowired
    private IVideoService videoService;
    
    @Autowired
    private VideoMapper videoMapper;

    /**
     * 自我注入：用于在同类方法间调用带 @Cacheable 的方法（解决Spring AOP代理问题）
     */
    @Autowired
    private IWebTagsService self;
    
    /**
     * 获取所有标签（带缓存）
     */
    @Override
    @Cacheable(value = "tagsList", key = "'all_tags'", unless = "#result == null")
    public AjaxResult getAllTags() {
        try {
            logger.info("🏷️ 获取所有标签");
            
            // 查询所有启用的标签（使用缓存）
            Tag queryTag = new Tag();
            queryTag.setStatus(1); // 只查询启用的标签
            List<Tag> tags = tagService.selectTagList(queryTag);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> tagList = tags.stream()
                .map(this::convertTagToMap)
                .collect(Collectors.toList());
            
            logger.info("✅ 获取所有标签成功: {}个标签", tagList.size());
            
            return AjaxResult.success(tagList);
            
        } catch (Exception e) {
            logger.error("❌ 获取所有标签失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取标签失败");
        }
    }

    /**
     * 分页获取标签（带缓存）
     */
    @Override
    @Cacheable(value = "tagsList", key = "'tags_paged_' + (#pageNum == null || #pageNum < 1 ? 1 : #pageNum) + '_' + (#pageSize == null || #pageSize < 1 ? 20 : #pageSize)", unless = "#result == null")
    public AjaxResult getTagsPaginated(Integer pageNum, Integer pageSize) {
        try {
            int safePage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
            int safeSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
            logger.info("📄 分页获取标签: pageNum={}, pageSize={}", safePage, safeSize);
            
            // 设置分页
            PageHelper.startPage(safePage, safeSize);
            
            // 查询所有启用的标签（使用缓存）
            Tag queryTag = new Tag();
            queryTag.setStatus(1); // 只查询启用的标签
            List<Tag> tags = tagService.selectTagList(queryTag);
            
            PageInfo<Tag> pageInfo = new PageInfo<>(tags);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> tagList = tags.stream()
                .map(this::convertTagToMap)
                .collect(Collectors.toList());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("list", tagList);
            responseData.put("total", pageInfo.getTotal());
            responseData.put("pageNum", pageInfo.getPageNum());
            responseData.put("pageSize", pageInfo.getPageSize());
            responseData.put("pages", pageInfo.getPages());
            
            logger.info("✅ 分页获取标签成功: 第{}页, 共{}个标签", safePage, tagList.size());
            
            return AjaxResult.success(responseData);
            
        } catch (Exception e) {
            logger.error("❌ 分页获取标签失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取标签失败");
        }
    }

    /**
     * 标签视频基础数据缓存（不含签名URL，仅DB实体 + 总数）
     * 返回 Map：key="list" 为 List&lt;Video&gt;，key="total" 为 Long 总数
     * 注意：total 取自 PageHelper 的 _COUNT 查询，与 list 同一逻辑过滤口径，避免 total/list 不一致。
     */
    @Override
    @Cacheable(value = "tagVideoList",
               key = "'tag_videos_basic_' + #tagId + '_' + (#pageNum == null || #pageNum < 1 ? 1 : #pageNum) + '_' + (#pageSize == null || #pageSize < 1 ? 20 : #pageSize)",
               unless = "#result == null")
    public Map<String, Object> getCachedTagVideoBaseData(Long tagId, Integer pageNum, Integer pageSize) {
        int safePage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safeSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        PageHelper.startPage(safePage, safeSize);
        List<Video> videos = videoMapper.selectVideosByTagId(tagId);
        if (videos == null) {
            videos = Collections.emptyList();
        }
        long total = (videos instanceof com.github.pagehelper.Page)
                ? ((com.github.pagehelper.Page<?>) videos).getTotal()
                : videos.size();
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>(videos));
        result.put("total", total);
        return result;
    }

    /**
     * 根据标签ID获取相关视频
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 AjaxResult，避免封面 URL 跨 region 污染。
     * 改为：基础数据走 getCachedTagVideoBaseList / getCachedTagVideoTotal 缓存，
     * 封面签名URL在 convertToWebVideoVO 中通过 getCachedCoverSignedUrl 实时按 region 缓存。
     */
    @Override
    public AjaxResult getVideosByTag(Long tagId, Integer pageNum, Integer pageSize) {
        try {
            int safePage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
            int safeSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
            logger.info("🏷️ 根据标签ID获取相关视频: tagId={}, pageNum={}, pageSize={}", tagId, safePage, safeSize);

            // 1) 基础数据走缓存（list + total 一起缓存，保证一致性）
            Map<String, Object> baseData = self.getCachedTagVideoBaseData(tagId, safePage, safeSize);
            @SuppressWarnings("unchecked")
            List<Video> videos = (List<Video>) baseData.get("list");
            if (videos == null) {
                videos = Collections.emptyList();
            }
            Number totalNum = (Number) baseData.get("total");
            long total = totalNum == null ? 0L : totalNum.longValue();

            List<Long> videoIds = videos.stream()
                    .filter(Objects::nonNull)
                    .map(Video::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<Long, List<Category>> categoriesByVideo = videoIds.isEmpty()
                    ? Collections.emptyMap()
                    : videoService.selectCategoriesByVideoIdsBatch(videoIds);
            Map<Long, List<Tag>> tagsByVideo = videoIds.isEmpty()
                    ? Collections.emptyMap()
                    : videoService.selectTagsByVideoIdsBatch(videoIds);

            // 2) 出口实时签 URL（封面通过 getCachedCoverSignedUrl 走 region 区分缓存）
            List<WebVideoVO> videoList = videos.stream()
                .map(video -> convertToWebVideoVO(video, categoriesByVideo, tagsByVideo))
                .collect(Collectors.toList());
            
            // 获取标签信息（使用缓存）
            Tag tag = tagService.selectTagById(tagId);
            Map<String, Object> tagInfo = new HashMap<>();
            if (tag != null) {
                tagInfo.put("id", tag.getId());
                tagInfo.put("name", tag.getName());
                tagInfo.put("color", tag.getColor());
            }
            
            // 构建响应数据
            int pages = safeSize > 0 ? (int) Math.ceil(total / (double) safeSize) : 0;
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("list", videoList);
            responseData.put("total", total);
            responseData.put("pageNum", safePage);
            responseData.put("pageSize", safeSize);
            responseData.put("pages", pages);
            responseData.put("tagInfo", tagInfo);
            
            logger.info("✅ 根据标签获取视频成功: 第{}页, 共{}个视频", safePage, videoList.size());
            
            return AjaxResult.success(responseData);
            
        } catch (Exception e) {
            logger.error("❌ 根据标签获取视频失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取标签相关视频失败");
        }
    }

    /**
     * 根据标签ID获取标签信息（带缓存）
     */
    @Override
    @Cacheable(value = "tagsList", key = "'tag_info_' + #tagId", unless = "#result == null")
    public AjaxResult getTagInfo(Long tagId) {
        try {
            logger.info("🏷️ 获取标签信息: tagId={}", tagId);
            
            Tag tag = tagService.selectTagById(tagId);
            
            if (tag == null) {
                return AjaxResult.error("标签不存在");
            }
            
            Map<String, Object> tagInfo = new HashMap<>();
            tagInfo.put("id", tag.getId());
            tagInfo.put("name", tag.getName());
            tagInfo.put("color", tag.getColor());
            tagInfo.put("status", tag.getStatus());
            tagInfo.put("createTime", tag.getCreatedAt());
            
            logger.info("✅ 获取标签信息成功: {}", tag.getName());
            
            return AjaxResult.success(tagInfo);
            
        } catch (Exception e) {
            logger.error("❌ 获取标签信息失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取标签信息失败");
        }
    }

    /**
     * 转换Video对象为WebVideoVO格式
     */
    private WebVideoVO convertToWebVideoVO(Video video,
                                          Map<Long, List<Category>> categoriesByVideo,
                                          Map<Long, List<Tag>> tagsByVideo) {
        WebVideoVO vo = new WebVideoVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setAuthor(video.getAuthor());
        vo.setPublishedAt(video.getPublishedAt());
        vo.setCreateTime(video.getCreatedAt());
        vo.setCategoryId(video.getCategoryId());

        // 生成封面图片URL
        String coverPath = video.getCoverUrl();
        if (coverPath == null || coverPath.isEmpty()) {
            coverPath = video.getCoverImage();
        }
        if (coverPath != null && !coverPath.isEmpty()) {
            try {
                String coverUrl = videoService.getCachedCoverSignedUrl(video.getId(), coverPath);
                vo.setCoverImageUrl(coverUrl != null ? sanitizeSignedUrl(coverUrl) : coverPath);
            } catch (Exception e) {
                logger.warn("生成封面URL失败: {}", e.getMessage());
            }
        }

        // 批量补齐分类信息
        List<Category> categories = categoriesByVideo.get(video.getId());
        if (categories != null && !categories.isEmpty()) {
            List<WebVideoVO.WebCategoryInfo> webCategories = categories.stream()
                    .filter(Objects::nonNull)
                    .map(category -> new WebVideoVO.WebCategoryInfo(category.getId(), category.getName()))
                    .collect(Collectors.toList());
            vo.setCategories(webCategories);

            if (StringUtils.isBlank(vo.getCategoryName())) {
                vo.setCategoryName(categories.get(0).getName());
            }
        } else if (StringUtils.isNotBlank(video.getCategoryName())) {
            vo.setCategoryName(video.getCategoryName());
        }

        // 批量补齐标签信息
        List<Tag> tags = tagsByVideo.get(video.getId());
        if (tags != null && !tags.isEmpty()) {
            List<WebVideoVO.WebTagInfo> webTags = tags.stream()
                    .filter(Objects::nonNull)
                    .map(tag -> new WebVideoVO.WebTagInfo(tag.getId(), tag.getName(), tag.getColor()))
                    .collect(Collectors.toList());
            vo.setTags(webTags);
        }

        return vo;
    }

    private String sanitizeSignedUrl(String signedUrl) {
        if (signedUrl == null) {
            return null;
        }
        return signedUrl.replace("&decrypt=true", "");
    }

    /**
     * 转换Tag对象为Map格式
     */
    private Map<String, Object> convertTagToMap(Tag tag) {
        Map<String, Object> tagMap = new HashMap<>();
        tagMap.put("id", tag.getId());
        tagMap.put("name", tag.getName());
        tagMap.put("color", tag.getColor());
        tagMap.put("status", tag.getStatus());
        tagMap.put("createTime", tag.getCreatedAt());
        return tagMap;
    }
}
