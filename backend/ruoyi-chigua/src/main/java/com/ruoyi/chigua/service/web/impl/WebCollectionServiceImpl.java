package com.ruoyi.chigua.service.web.impl;

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
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.mapper.CollectionMapper;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.service.ICollectionService;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.web.IWebCollectionService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Web合集Service业务层处理
 * 
 * @author chigua
 * @date 2025-01-20
 */
@Service
public class WebCollectionServiceImpl implements IWebCollectionService 
{
    private static final Logger logger = LoggerFactory.getLogger(WebCollectionServiceImpl.class);
    
    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private ICollectionService collectionService;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    @Autowired
    private IVideoService videoService;

    /**
     * 自我注入：用于在同类方法间调用带 @Cacheable 的方法（解决Spring AOP代理问题）
     */
    @Autowired
    private IWebCollectionService self;

    /**
     * Web合集基础列表缓存（不含签名URL，仅DB实体）
     * 返回 Map：key="list" 为 List&lt;Collection&gt;，key="total" 为 Long 总数
     */
    @Override
    @Cacheable(value = "webCollectionList",
               key = "'list_basic_' + (#categoryId != null ? #categoryId : 'all') + '_' + (#pageNum == null || #pageNum < 1 ? 1 : #pageNum) + '_' + (#pageSize == null || #pageSize < 1 ? 20 : #pageSize)",
               unless = "#result == null")
    public Map<String, Object> getCachedWebCollectionBaseList(Long categoryId, Integer pageNum, Integer pageSize) {
        int safePage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safeSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        PageHelper.startPage(safePage, safeSize);

        Collection collection = new Collection();
        collection.setStatus(1);
        if (categoryId != null) {
            collection.setCategoryId(categoryId.toString());
        }

        List<Collection> collections = collectionService.selectCollectionList(collection);
        if (collections == null) {
            collections = new java.util.ArrayList<>();
        }
        long total = (collections instanceof com.github.pagehelper.Page)
                ? ((com.github.pagehelper.Page<?>) collections).getTotal()
                : new PageInfo<>(collections).getTotal();

        Map<String, Object> result = new java.util.HashMap<>();
        // 复制成普通 ArrayList，避免缓存的是 PageHelper 的 Page 子类（反序列化更稳定）
        result.put("list", new java.util.ArrayList<>(collections));
        result.put("total", total);
        return result;
    }

    /**
     * 根据分类ID查询Web合集列表
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 TableDataInfo，
     * 改为：基础数据走 getCachedWebCollectionBaseList 缓存（不含签名URL），
     * 封面签名URL在出口处实时按访客 IP 重新生成。
     */
    @Override
    public TableDataInfo selectWebCollectionList(Long categoryId, Integer pageNum, Integer pageSize)
    {
        int safePage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safeSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        logger.info("🔍 查询Web合集列表: categoryId={}, pageNum={}, pageSize={}", categoryId, safePage, safeSize);

        Map<String, Object> baseData = self.getCachedWebCollectionBaseList(categoryId, safePage, safeSize);
        @SuppressWarnings("unchecked")
        List<Collection> rawCollections = (List<Collection>) baseData.get("list");
        Number totalNum = (Number) baseData.get("total");
        long total = totalNum == null ? 0L : totalNum.longValue();

        // 出口实时签 URL：克隆每个 Collection，避免污染缓存中的实体
        List<Collection> collections = new java.util.ArrayList<>();
        if (rawCollections != null) {
            for (Collection raw : rawCollections) {
                if (raw == null) continue;
                Collection item = cloneCollection(raw);
                signCollectionCover(item);
                collections.add(item);
            }
        }

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(collections);
        rspData.setTotal(total);

        logger.info("✅ 查询Web合集列表成功: 共{}条", collections.size());
        return rspData;
    }

    private void signCollectionCover(Collection item) {
        String rawCover = item.getCoverImage();
        if (rawCover == null || rawCover.trim().isEmpty()) {
            return;
        }
        // 已经是签名URL（含 signature= 或以 http 开头），跳过
        if (rawCover.contains("signature=") || rawCover.startsWith("http://") || rawCover.startsWith("https://")) {
            return;
        }
        try {
            String signedCoverUrl = chiguaUrlService.generateUrl(rawCover, ChiguaUrlService.ResourceType.COVER);
            if (signedCoverUrl != null) {
                item.setCoverImage(sanitizeSignedUrl(signedCoverUrl));
            }
        } catch (Exception e) {
            logger.warn("❌ 为合集{}生成Web封面URL失败: {}", item.getId(), e.getMessage());
        }
    }

    private Collection cloneCollection(Collection src) {
        Collection dst = new Collection();
        dst.setId(src.getId());
        dst.setTitle(src.getTitle());
        dst.setDescription(src.getDescription());
        dst.setCoverImage(src.getCoverImage());
        dst.setAuthor(src.getAuthor());
        dst.setCategoryId(src.getCategoryId());
        dst.setStatus(src.getStatus());
        dst.setSortOrder(src.getSortOrder());
        dst.setVideoCount(src.getVideoCount());
        dst.setViewCount(src.getViewCount());
        dst.setCreatedAt(src.getCreatedAt());
        dst.setUpdatedAt(src.getUpdatedAt());
        dst.setCategories(src.getCategories());
        dst.setCategoryIds(src.getCategoryIds());
        return dst;
    }

    /**
     * 合集详情基础数据缓存（不含签名URL，仅DB实体）
     */
    @Override
    @Cacheable(value = "collectionDetail", key = "'detail_basic_' + #collectionId", unless = "#result == null")
    public Collection getCachedCollectionBaseDetail(Long collectionId) {
        Collection collection = collectionMapper.selectCollectionById(collectionId);
        if (collection != null && Integer.valueOf(1).equals(collection.getStatus())) {
            return collection;
        }
        return null;
    }

    /**
     * 合集视频列表基础数据缓存（不含签名URL，仅DB实体）
     */
    @Override
    @Cacheable(value = "collectionDetail", key = "'videos_basic_' + #collectionId", unless = "#result == null")
    public List<Video> getCachedCollectionBaseVideos(Long collectionId) {
        List<Video> videos = collectionMapper.selectVideosByCollectionId(collectionId);
        return videos == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(videos);
    }

    /**
     * 根据合集ID查询Web合集详情
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 Collection（含签名URL），
     * 改为：基础数据走 getCachedCollectionBaseDetail 缓存，封面签名URL实时按 IP 生成。
     */
    @Override
    public Collection selectWebCollectionById(Long collectionId)
    {
        logger.info("🔍 查询Web合集详情: collectionId={}", collectionId);

        Collection raw = self.getCachedCollectionBaseDetail(collectionId);
        if (raw == null) {
            logger.warn("⚠️ 合集不存在或未发布: collectionId={}", collectionId);
            return null;
        }
        // 克隆出新对象，避免污染缓存内的实体
        Collection collection = cloneCollection(raw);
        signCollectionCover(collection);
        logger.info("✅ 查询Web合集详情成功: title={}", collection.getTitle());
        return collection;
    }

    /**
     * 查询合集中的视频列表
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 List&lt;Video&gt;（含签名URL），
     * 改为：基础数据走 getCachedCollectionBaseVideos 缓存，封面签名URL通过
     * getCachedCoverSignedUrl 走 region 区分缓存实时获取。
     */
    @Override
    public List<Video> selectVideosByCollectionId(Long collectionId)
    {
        logger.info("🔍 查询合集视频列表: collectionId={}", collectionId);

        List<Video> rawVideos = self.getCachedCollectionBaseVideos(collectionId);
        if (rawVideos == null || rawVideos.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 克隆 + 出口实时签 URL（封面通过 getCachedCoverSignedUrl 走 region 缓存）
        List<Video> videos = new java.util.ArrayList<>(rawVideos.size());
        for (Video raw : rawVideos) {
            if (raw == null) continue;
            Video v = cloneVideoForCollection(raw);
            decorateCollectionVideoCover(v);
            videos.add(v);
        }
        populateCollectionVideoCategories(videos);

        logger.info("✅ 查询合集视频列表成功: 共{}个视频", videos.size());
        return videos;
    }

    /**
     * 浅克隆 Video 关键字段（用于合集视频列表，避免修改缓存内实体）
     */
    private Video cloneVideoForCollection(Video src) {
        Video dst = new Video();
        dst.setId(src.getId());
        dst.setTitle(src.getTitle());
        dst.setDescription(src.getDescription());
        dst.setAuthor(src.getAuthor());
        dst.setDuration(src.getDuration());
        dst.setViewCount(src.getViewCount());
        dst.setLikeCount(src.getLikeCount());
        dst.setCategoryId(src.getCategoryId());
        dst.setCategoryName(src.getCategoryName());
        dst.setCoverImage(src.getCoverImage());
        dst.setCoverUrl(src.getCoverUrl());
        dst.setStatus(src.getStatus());
        dst.setPublishedAt(src.getPublishedAt());
        dst.setCreatedAt(src.getCreatedAt());
        dst.setUpdatedAt(src.getUpdatedAt());
        dst.setIsHot(src.getIsHot());
        dst.setIsRecommended(src.getIsRecommended());
        return dst;
    }

    private void decorateCollectionVideoCover(Video video) {
        if (video == null) {
            return;
        }
        String coverPath = video.getCoverUrl();
        if (coverPath == null || coverPath.trim().isEmpty()) {
            coverPath = video.getCoverImage();
        }
        if (coverPath == null || coverPath.trim().isEmpty()) {
            logger.warn("⚠️ 视频{}没有封面图片", video.getId());
            return;
        }
        try {
            String signedCover = videoService.getCachedCoverSignedUrl(video.getId(), coverPath);
            if (signedCover != null) {
                video.setCoverUrl(sanitizeSignedUrl(signedCover));
            }
        } catch (Exception e) {
            logger.error("❌ 为视频{}生成Web封面URL失败: {}", video.getId(), e.getMessage(), e);
        }
    }

    private String sanitizeSignedUrl(String signedUrl) {
        if (signedUrl == null) {
            return null;
        }
        return signedUrl.replace("&decrypt=true", "");
    }

    private void populateCollectionVideoCategories(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        List<Long> videoIds = videos.stream()
                .filter(video -> video != null && video.getId() != null)
                .map(Video::getId)
                .collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            return;
        }

        Map<Long, List<Category>> categoriesByVideo = videoService.selectCategoriesByVideoIdsBatch(videoIds);
        if (categoriesByVideo == null || categoriesByVideo.isEmpty()) {
            return;
        }

        for (Video video : videos) {
            if (video == null) {
                continue;
            }
            List<Category> categories = categoriesByVideo.get(video.getId());
            if (categories != null && !categories.isEmpty()) {
                video.setCategories(categories);
                if (StringUtils.isBlank(video.getCategoryName())) {
                    video.setCategoryName(categories.get(0).getName());
                }
            }
        }
    }
}
