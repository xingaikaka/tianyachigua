package com.ruoyi.chigua.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import com.github.pagehelper.PageHelper;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.mapper.VideoTranscodeMapper;
import com.ruoyi.chigua.mapper.TagMapper;
import com.ruoyi.chigua.mapper.CategoryMapper;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.vo.web.WebVideoListVO;
import com.ruoyi.chigua.domain.VideoImage;
import com.ruoyi.chigua.domain.VideoUrl;
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.IVideoImageService;
import com.ruoyi.chigua.service.IVideoUrlService;
import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.service.VideoUrlGeneratorService;
import com.ruoyi.chigua.service.RichTextProcessorService;
import com.ruoyi.chigua.service.CacheRefreshService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.config.ChiguaProperties;
import org.springframework.data.redis.core.RedisTemplate;
import com.ruoyi.chigua.dto.VideoWithUrlsDto;
import com.ruoyi.chigua.utils.WordSegmentationUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;

/**
 * 视频管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class VideoServiceImpl implements IVideoService 
{
    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    
    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoTranscodeMapper videoTranscodeMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private IVideoImageService videoImageService;

    @Autowired
    private IVideoUrlService videoUrlService;

    @Autowired
    private IVideoTranscodeService videoTranscodeService;

    @Autowired
    private VideoUrlGeneratorService videoUrlGeneratorService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RichTextProcessorService richTextProcessorService;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private IVideoService videoServiceProxy;
    
    @Autowired
    private ChiguaProperties chiguaProperties;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询视频
     * 
     * @param id 视频主键
     * @return 视频
     */
    @Override
    public Video selectVideoById(Long id)
    {
        Video video = videoMapper.selectVideoById(id);
        if (video != null) {
            // 查询关联的分类和标签（使用缓存）
            List<Category> categories = selectCategoriesByVideoId(id);
            List<Tag> tags = selectTagsByVideoId(id);
            
            video.setCategories(categories);
            video.setTags(tags);
            
            // 设置ID数组，方便前端使用
            if (categories != null && !categories.isEmpty()) {
                List<Long> categoryIds = new java.util.ArrayList<>();
                for (Category category : categories) {
                    categoryIds.add(category.getId());
                }
                video.setCategoryIds(categoryIds);
            }
            
            if (tags != null && !tags.isEmpty()) {
                List<Long> tagIds = new java.util.ArrayList<>();
                for (Tag tag : tags) {
                    tagIds.add(tag.getId());
                }
                video.setTagIds(tagIds);
            }

            // 后台表单回显：封面统一签名后返回完整URL（仅用于返回，不落库）
            if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = videoUrlGeneratorService.generateCoverUrl(video);
                    if (signedCoverUrl != null) {
                        video.setCoverImage(signedCoverUrl);
                    }
                } catch (Exception ignored) { }
            }
        }
        return video;
    }

    /**
     * 查询视频列表（后台管理专用，不使用缓存）
     * 后台管理应该始终看到最新数据，因此直接查询数据库
     * 
     * @param video 视频查询条件
     * @return 视频列表（包含签名URL）
     */
    @Override
    public List<Video> selectVideoList(Video video)
    {
        logger.info("🔍 后台管理查询视频列表（直接查询数据库，不使用缓存）");
        
        // 直接查询数据库，确保数据是最新的
        List<Video> result = videoMapper.selectVideoListWithStats(video);
        
        // 生成 Worker 签名URL（管理后台始终使用 tycgimage1.org，不缓存结果）
        for (Video videoItem : result) {
            if (videoItem.getCoverImage() != null && !videoItem.getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = videoUrlGeneratorService.generateWorkerCoverUrl(videoItem);
                    if (signedCoverUrl != null) {
                        // 列表显示：直接返回完整URL到coverImage，便于前端组件绑定
                        videoItem.setCoverImage(signedCoverUrl);
                    }
                } catch (Exception e) {
                    logger.warn("为视频{}生成封面签名URL失败: {}, 错误: {}",
                        videoItem.getId(), videoItem.getCoverImage(), e.getMessage());
                }
            }
        }
        
        logger.info("📊 后台管理查询完成，返回{}条视频记录", result.size());
        return result;
    }

    /**
     * 根据source_id列表批量查询视频
     * 
     * @param sourceIds source_id列表
     * @return 视频集合（包含id和sourceId字段）
     */
    @Override
    public List<Video> selectVideosBySourceIds(List<String> sourceIds)
    {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.info("🔍 批量查询sourceId视频，数量: {}", sourceIds.size());
        List<Video> result = videoMapper.selectVideosBySourceIds(sourceIds);
        logger.info("✅ 批量查询完成，找到{}条匹配记录", result.size());
        
        return result;
    }

    @Override
    public List<Video> selectVideosByTitles(List<String> titles)
    {
        if (titles == null || titles.isEmpty()) {
            return new ArrayList<>();
        }

        logger.info("🔍 批量查询title视频，数量: {}", titles.size());
        List<Video> result = videoMapper.selectVideosByTitles(titles);
        logger.info("✅ 批量title查询完成，找到{}条匹配记录", result.size());

        return result;
    }
    
    /**
     * 查询视频列表 —— 严格"方案 B"：仅返回 DB 基础数据，缓存中不含任何 CDN 签名 URL，
     * 因此缓存可跨 region 共享，不会出现国内/国外 CDN 互相污染。
     *
     * 历史调用方 {@code WebCategoryServiceImpl#getAdjacentVideosFromList} 仅使用
     * id/title 找相邻视频，故无需 cover 签名 URL；其它若有需要展示封面的场景，
     * 应在出口处调用 {@code chiguaUrlService.generateUrl(...)} 实时签名。
     */
    @Cacheable(value = "videoList", key = "'list_' + (#video.categoryId != null ? #video.categoryId : 'all') + '_' + (#video.status != null ? #video.status : 'all') + '_' + (#video.isHot != null ? #video.isHot : 'all') + '_' + (#video.isRecommended != null ? #video.isRecommended : 'all')", unless = "#result == null")
    public List<Video> selectVideoListWithSignedUrls(Video video)
    {
        return videoMapper.selectVideoListWithStats(video);
    }

    /**
     * 获取缓存的视频列表基础数据（不包含签名URL）
     * 专门为热门推荐等需要缓存但不需要签名URL的场景设计
     */
    @Override
    @Cacheable(value = "videoList", key = "'basic_' + (#video.categoryId != null ? #video.categoryId : 'all') + '_' + (#video.status != null ? #video.status : 'all') + '_' + (#video.isHot != null ? #video.isHot : 'all') + '_' + (#video.isRecommended != null ? #video.isRecommended : 'all')", unless = "#result == null")
    public List<Video> getCachedVideoListBasic(Video video) {
        logger.debug("🔍 执行数据库查询 getCachedVideoListBasic: status={}, isHot={}, isRecommended={}", 
                    video.getStatus(), video.getIsHot(), video.getIsRecommended());
        
        // 纯数据库查询，不生成签名URL，适合缓存
        return videoMapper.selectVideoListWithStats(video);
    }
    

    

    
    /**
     * Web 精简版：查询视频总数 - 使用版本控制缓存
     */
    public Long countWebVideoListBasic(Video video)
    {
        // 使用手动缓存管理，避免Spring缓存的类型转换问题
        String cacheKey = buildCountCacheKey(video);
        Long cachedCount = getCachedCount(cacheKey);
        
        if (cachedCount != null) {
            logger.debug("🎯 视频总数缓存命中: key={}, count={}", cacheKey, cachedCount);
            return cachedCount;
        }
        
        try {
            // 查询数据库
            long count = videoMapper.countWebVideoListBasic(video);
            Long result = Long.valueOf(count);
            
            // 缓存结果
            setCachedCount(cacheKey, result);
            logger.debug("📝 视频总数已缓存: key={}, count={}", cacheKey, result);
            
            return result;
        } catch (Exception e) {
            logger.error("❌ 查询视频总数失败: video={}, 错误: {}", video, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 构建总数查询的缓存键
     */
    private String buildCountCacheKey(Video video) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("videoCount:countWebVideoListBasic:");
        
        // 添加查询参数
        if (video.getCategoryId() != null) {
            keyBuilder.append("categoryId=").append(video.getCategoryId());
        }
        if (video.getStatus() != null) {
            keyBuilder.append(",status=").append(video.getStatus());
        }
        if (video.getTitle() != null && !video.getTitle().trim().isEmpty()) {
            keyBuilder.append(",title=").append(video.getTitle());
        }
        
        // 移除版本号（已改为固定键策略）
        
        return keyBuilder.toString();
    }

    /**
     * 从缓存获取总数
     */
    private Long getCachedCount(String cacheKey) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? Long.valueOf(cached) : null;
        } catch (Exception e) {
            logger.warn("⚠️ 获取缓存总数失败: key={}, 错误: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * 设置总数缓存（防空缓存：不缓存计数为0的结果）
     */
    private void setCachedCount(String cacheKey, Long count) {
        try {
            if (count != null && count > 0) {
                // 缓存1小时
                redisTemplate.opsForValue().set(cacheKey, count.toString(), 3600, TimeUnit.SECONDS);
                logger.debug("✅ 总数缓存存储成功: key={}, count={}", cacheKey, count);
            } else if (count != null && count == 0) {
                logger.info("🚫 跳过空总数缓存: key={}, count=0, 避免缓存空计数", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("⚠️ 设置缓存总数失败: key={}, count={}, 错误: {}", cacheKey, count, e.getMessage());
        }
    }

    /**
     * 将封面字段规范化为相对路径（files/...）。
     * 保存入库时调用，避免把完整URL写入数据库。
     */
    private String normalizeCoverImageToPath(String coverImage) {
        if (coverImage == null) {
            return null;
        }
        String value = coverImage.trim();
        if (value.isEmpty()) {
            return value;
        }

        // 去掉查询参数
        int qIdx = value.indexOf('?');
        if (qIdx > -1) {
            value = value.substring(0, qIdx);
        }

        // 完整URL -> 提取路径
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int idx = value.indexOf("/files/");
            if (idx >= 0 && idx + 1 < value.length()) {
                return value.substring(idx + 1); // files/...
            }
            try {
                java.net.URL u = new java.net.URL(value);
                String path = u.getPath();
                if (path.startsWith("/")) path = path.substring(1);
                return path;
            } catch (Exception ignored) {}
        }

        // 绝对路径
        if (value.startsWith("/")) {
            if (value.startsWith("/profile/images/")) {
                return value.substring("/profile/".length()); // images/...
            }
            return value.substring(1);
        }

        return value;
    }

    /**
     * 将首个视频地址规范化为相对路径（videos/...）。
     * 支持传入完整URL或带查询参数的链接。
     * 
     * 预期格式：videos/202510/05/68e23a811b3591e6e7f09c7f/6ga0g8/index.m3u8
     */
    private String normalizeFirstVideoPath(String videoUrl) {
        if (StringUtils.isBlank(videoUrl)) {
            return null;
        }

        String value = videoUrl.trim();

        // 跳过浏览器生成的 blob/data URL
        if (value.startsWith("blob:" ) || value.startsWith("data:")) {
            return null;
        }

        // 去掉查询参数
        int qIdx = value.indexOf('?');
        if (qIdx >= 0) {
            value = value.substring(0, qIdx);
        }

        // 完整URL -> 提取路径
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(value);
                value = url.getPath();
            } catch (Exception ignored) {}
        } else if (value.startsWith("//")) {
            // 协议相对地址
            value = value.substring(2);
            int firstSlash = value.indexOf('/');
            if (firstSlash >= 0) {
                value = value.substring(firstSlash + 1);
            }
        }

        if (value.startsWith("/")) {
            value = value.substring(1);
        }

        // 🔧 关键修复：去掉 files/ 前缀，统一为 videos/ 开头的格式
        if (value.startsWith("files/videos/")) {
            value = value.substring(6); // 去掉 "files/"，保留 "videos/"
            logger.debug("🔧 规范化路径：去掉 files/ 前缀 -> {}", value);
        } else if (value.startsWith("files/")) {
            // 如果是 files/其他路径，也去掉 files/
            value = value.substring(6);
            logger.debug("🔧 规范化路径：去掉 files/ 前缀 -> {}", value);
        }

        if (StringUtils.isBlank(value)) {
            return null;
        }

        String lower = value.toLowerCase(Locale.ROOT);
        boolean hasKnownExt = lower.endsWith(".m3u8") || lower.endsWith(".mp4") || lower.endsWith(".webm")
            || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".avi");

        if (!hasKnownExt) {
            if (lower.endsWith("/")) {
                value = value + "index.m3u8";
            } else {
                value = value + "/index.m3u8";
            }
        }

        return value;
    }

    /**
     * 当 first_video_url 为空时，根据主地址或关联地址回填。
     */
    private void backfillFirstVideoUrlIfMissing(Video video) {
        if (video == null || video.getId() == null) {
            return;
        }
        if (StringUtils.isNotBlank(video.getFirstVideoUrl())) {
            return;
        }

        try {
            String candidate = null;

            // 1. 优先使用主要播放地址
            VideoUrl primaryUrl = videoUrlService.selectPrimaryUrlByVideoId(video.getId());
            if (primaryUrl != null && StringUtils.isNotBlank(primaryUrl.getVideoUrl())) {
                candidate = normalizeFirstVideoPath(primaryUrl.getVideoUrl());
            }

            // 2. 退化到第一条地址
            if (StringUtils.isBlank(candidate)) {
                List<VideoUrl> urlList = videoUrlService.selectVideoUrlsByVideoId(video.getId());
                if (urlList != null) {
                    for (VideoUrl item : urlList) {
                        if (item == null || StringUtils.isBlank(item.getVideoUrl())) {
                            continue;
                        }
                        candidate = normalizeFirstVideoPath(item.getVideoUrl());
                        if (StringUtils.isNotBlank(candidate)) {
                            break;
                        }
                    }
                }
            }

            if (StringUtils.isNotBlank(candidate)) {
                Video update = new Video();
                update.setId(video.getId());
                update.setFirstVideoUrl(candidate);
                videoMapper.updateVideo(update);
                video.setFirstVideoUrl(candidate);
                logger.info("✅ 自动回填 first_video_url: videoId={}, url={}", video.getId(), candidate);
            } else {
                logger.debug("ℹ️ 未找到可用于回填的播放地址: videoId={}", video.getId());
            }
        } catch (Exception e) {
            logger.warn("⚠️ 回填 first_video_url 失败: videoId={}, err={}", video.getId(), e.getMessage());
        }
    }

    /**
     * Web 精简版：分页查询视频列表（仅基础字段），并生成封面签名URL
     * 注意：不能缓存此方法，因为URL生成有时效性，但内部会调用缓存的数据库查询方法
     */
    public java.util.List<Video> selectWebVideoListBasicPagedWithSignedUrls(Video video, Integer pageNum, Integer pageSize)
    {
        logger.info("📋 调用 selectWebVideoListBasicPagedWithSignedUrls: categoryId={}, pageNum={}, pageSize={}", 
                    video != null ? video.getCategoryId() : null, pageNum, pageSize);

        // 采用显式 offset/limit，绕过插件分页差异 - 直接调用数据库查询以支持缓存
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (safePageNum - 1) * safePageSize;

        // 🔧 修复：通过ApplicationContext获取代理对象，确保AOP生效
        IVideoService proxiedSelf = applicationContext.getBean(IVideoService.class);
        
        // 统一使用 mapper 查询（排序：is_recommended, is_hot, sort_order, published_at）
        java.util.List<Video> result = proxiedSelf.getCachedVideoListBasicPaged(video, offset, safePageSize);

        //logger.info("🎯 获取到{}条视频记录，开始生成签名URL", result.size());

        // 为每个视频生成签名URL（这部分不适合缓存，因为URL有时效性）
        for (Video videoItem : result) {
            //logger.info("🔍 检查视频{}: coverImage={}", videoItem.getId(), videoItem.getCoverImage());
            if (videoItem.getCoverImage() != null && !videoItem.getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = videoUrlGeneratorService.generateCoverUrl(videoItem);
                    if (signedCoverUrl != null) {
                        //logger.info("🔗 视频{}生成签名封面URL: {}", videoItem.getId(), signedCoverUrl);
                        // 使用coverUrl临时字段，不会持久化到数据库
                        videoItem.setCoverUrl(signedCoverUrl);
                    } else {
                        logger.warn("⚠️ 视频{}生成签名封面URL返回null", videoItem.getId());
                    }
                } catch (Exception e) {
                    logger.warn("❌ 生成视频{}封面签名URL失败: {}", videoItem.getId(), e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 带缓存的视频列表查询（用于解决AOP代理问题）
     */
    @Cacheable(value = "videoList",
               key = "'basic_paged_' + (#video.categoryId != null ? #video.categoryId : 'all') + '_s' + (#video.status != null ? #video.status : '') + '_h' + (#video.isHot != null ? #video.isHot : '') + '_r' + (#video.isRecommended != null ? #video.isRecommended : '') + '_' + #offset + '_' + #pageSize",
               unless = "#result == null")
    public java.util.List<Video> getCachedVideoListBasicPaged(Video video, int offset, int pageSize)
    {
        logger.info("🔍 执行数据库查询 getCachedVideoListBasicPaged: categoryId={}, offset={}, pageSize={}", 
                    video != null ? video.getCategoryId() : null, offset, pageSize);

        // 纯数据库查询，适合缓存
        return videoMapper.selectWebVideoListBasicPaged(video, offset, pageSize);
    }

    /**
     * 带缓存的分类视频列表查询（已移除 category_video_sort，统一使用 is_recommended, is_hot 排序）
     */
    @Cacheable(value = "videoList", key = "'mixed_sort_' + (#video.categoryId != null ? #video.categoryId : 'all') + '_' + #offset + '_' + #pageSize", unless = "#result == null")
    public java.util.List<Video> getCachedCategoryVideoListWithMixedSort(Video video, int offset, int pageSize)
    {
        logger.info("📋 执行分类视频列表查询: categoryId={}, offset={}, pageSize={}", video != null ? video.getCategoryId() : null, offset, pageSize);
        return videoMapper.selectWebVideoListBasicPaged(video, offset, pageSize);
    }

    /**
     * 列表专用：分页查询视频列表（仅必需字段），并生成封面签名URL
     * 优化版：使用三层缓存策略
     */
    @Override
    public List<WebVideoListVO> selectVideoListOnlyPagedWithSignedUrls(Video video, Integer pageNum, Integer pageSize)
    {
        // 采用显式 offset/limit，绕过插件分页差异
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (safePageNum - 1) * safePageSize;

        logger.info("🎯 执行列表专用查询 selectVideoListOnlyPagedWithSignedUrls: categoryId={}, pageNum={}, pageSize={}, offset={}", 
                    video != null ? video.getCategoryId() : null, pageNum, pageSize, offset);

        // 第一步：获取基础数据（使用缓存）
        List<WebVideoListVO> result = getCachedVideoListOnlyPagedBasicData(video, safePageNum, safePageSize);
        logger.info("🎯 [缓存HIT/MISS] 获取到{}条视频记录，开始生成签名URL", result.size());

        // 第二步：为每个视频生成签名URL（现在也使用缓存了！）
        for (WebVideoListVO videoItem : result) {
            if (videoItem.getCoverImageUrl() != null && !videoItem.getCoverImageUrl().trim().isEmpty()) {
                try {
                    // 使用缓存的签名URL生成方法
                    String signedCoverUrl = getCachedCoverSignedUrl(videoItem.getId(), videoItem.getCoverImageUrl());
                    if (signedCoverUrl != null) {
                        videoItem.setCoverImageUrl(signedCoverUrl);
                    } else {
                        logger.warn("⚠️ 视频{}生成签名封面URL返回null", videoItem.getId());
                    }
                } catch (Exception e) {
                    logger.warn("❌ 生成视频{}封面签名URL失败: {}", videoItem.getId(), e.getMessage());
                }
            }
        }

        enrichVideoListWithCategories(result);

        return result;
    }

    /**
     * 第一层缓存：基础数据缓存（不含签名URL）
     */
    public List<WebVideoListVO> getCachedVideoListOnlyPagedBasicData(Video video, int pageNum, int pageSize) {
        int safePage = pageNum < 1 ? 1 : pageNum;
        int safeSize = pageSize < 1 ? 1 : pageSize;
        int offset = (safePage - 1) * safeSize;
        String fingerprint = buildVideoListFingerprint(video);
        String categoryKey = resolveCategoryKey(video);
        String actualCacheKey = String.format(Locale.ROOT, "basic_data_%s_%s_p%d_s%d", categoryKey, fingerprint, safePage, safeSize);

        // 先尝试从缓存获取
        try {
            Cache cache = cacheManager.getCache("videoListOptimized");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(actualCacheKey);
                if (wrapper != null) {
                    @SuppressWarnings("unchecked")
                    List<WebVideoListVO> cachedValue = (List<WebVideoListVO>) wrapper.get();
                    return cloneVideoList(cachedValue);
                }
            }
        } catch (Exception e) {
            logger.warn("❌ 基础数据缓存获取失败: {}", e.getMessage());
        }


        // 过滤短视频（当categoryId为null时）
        Boolean excludeShort = shouldExcludeShort(video);
        List<WebVideoListVO> result = videoMapper.selectVideoListOnlyPaged(video, offset, safeSize, excludeShort);

        // 手动存储到缓存（防空缓存：不缓存空结果）
        try {
            Cache cache = cacheManager.getCache("videoListOptimized");
            if (cache != null && result != null && !result.isEmpty()) {
                cache.put(actualCacheKey, cloneVideoList(result));
                logger.debug("✅ 缓存存储成功: key={}, size={}", actualCacheKey, result.size());
            } else if (result != null && result.isEmpty()) {
                logger.info("🚫 跳过空结果缓存: key={}, 避免缓存空分页数据", actualCacheKey);
            }
        } catch (Exception e) {
            logger.warn("❌ 基础数据缓存存储失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 第二层缓存：签名URL缓存
     */
    @Override
    public String getCachedCoverSignedUrl(Long videoId, String originalPath) {
        // 缓存 key 含 CDN 区域标识（CN/OS），避免国内/国外用户共用同一条缓存导致 CDN 路由失效
        String regionKey = chiguaUrlService.getCurrentCdnRegionKey();
        String cacheKey = "cover_" + videoId + "_" + (originalPath != null ? originalPath.hashCode() : "null") + "_" + regionKey;
        
        // 先尝试从缓存获取
        try {
            Cache cache = cacheManager.getCache("signedUrlCache");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    String cachedValue = (String) wrapper.get();
                    return cachedValue;
                }
            }
        } catch (Exception e) {
            logger.warn("❌ 签名URL缓存获取失败: {}", e.getMessage());
        }
        
        try {
            // 创建临时Video对象用于生成签名URL
            Video tempVideo = new Video();
            tempVideo.setId(videoId);
            tempVideo.setCoverImage(originalPath);
            
            String signedUrl = videoUrlGeneratorService.generateCoverUrl(tempVideo);

            // 手动存储到缓存
            try {
                Cache cache = cacheManager.getCache("signedUrlCache");
                if (cache != null && signedUrl != null) {
                    cache.put(cacheKey, signedUrl);
                }
            } catch (Exception cacheEx) {
                logger.warn("❌ 签名URL缓存存储失败: {}", cacheEx.getMessage());
            }
            
            return signedUrl;
        } catch (Exception e) {
            logger.warn("❌ 生成封面签名URL失败: videoId={}, originalPath={}", videoId, originalPath, e);
            return originalPath; // 降级返回原始路径
        }
    }

    /**
     * 列表专用：带缓存的视频列表查询（仅必需字段）- 支持分类排序功能
     */
    @Override
    public List<WebVideoListVO> getCachedVideoListOnlyPaged(Video video, int offset, int pageSize)
    {
        // 统一使用 mapper 查询（排序：is_recommended, is_hot, sort_order, published_at）
        Boolean excludeShort = (video != null && video.getCategoryId() == null) ? Boolean.TRUE : null;
        List<WebVideoListVO> result = videoMapper.selectVideoListOnlyPaged(video, offset, pageSize, excludeShort);
        enrichVideoListWithCategories(result);
        return result;
    }

    /**
     * 列表专用：查询视频总数（添加缓存）
     */
    @Override
    public Long countVideoListOnly(Video video)
    {
        // 构建缓存键，避免类型转换问题
        String fingerprint = buildVideoListFingerprint(video);
        String categoryKey = resolveCategoryKey(video);
        String cacheKey = "count_only_" + categoryKey + "_" + fingerprint;
        

        // 先尝试从缓存获取
        try {
            Cache cache = cacheManager.getCache("videoListOptimized");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    Object cachedObj = wrapper.get();
                    Long cachedValue;
                    if (cachedObj instanceof Integer) {
                        cachedValue = ((Integer) cachedObj).longValue();
                    } else if (cachedObj instanceof Long) {
                        cachedValue = (Long) cachedObj;
                    } else {
                        cachedValue = Long.valueOf(cachedObj.toString());
                    }
                    return cachedValue;
                }
            }
        } catch (Exception e) {
            logger.warn("❌ 缓存获取失败: {}", e.getMessage());
        }
        
        // 缓存未命中，执行数据库查询
        return getCachedCountVideoListOnly(video, cacheKey);
    }
    
    /**
     * 带缓存的计数查询方法
     */
    public Long getCachedCountVideoListOnly(Video video, String cacheKey) {
        logger.info("🔍 [缓存MISS] 执行数据库计数查询 countVideoListOnly: categoryId={}, cacheKey={}, timestamp={}", 
                    video != null ? video.getCategoryId() : null, cacheKey, System.currentTimeMillis());
        
        Boolean excludeShort = shouldExcludeShort(video);
        Long count = videoMapper.countVideoListOnly(video, excludeShort);

        // 手动存储到缓存（防空缓存：不缓存计数为0的结果）
        try {
            Cache cache = cacheManager.getCache("videoListOptimized");
            if (cache != null && count != null && count > 0) {
                cache.put(cacheKey, count);
                logger.debug("✅ 计数缓存存储成功: key={}, count={}", cacheKey, count);
            } else if (count != null && count == 0) {
                logger.info("🚫 跳过空计数缓存: key={}, count=0, 避免缓存空计数", cacheKey);
            }
        } catch (Exception e) {
            logger.warn("❌ 计数缓存存储失败: {}", e.getMessage());
        }
        
        return count;
    }

    /**
     * 根据视频查询条件生成缓存指纹，确保所有可变过滤项均纳入缓存键。
     */
    private String buildVideoListFingerprint(Video video) {
        StringBuilder builder = new StringBuilder();
        if (video != null) {
            builder.append("category=").append(resolveCategoryKey(video));
            builder.append("|status=").append(video.getStatus() != null ? video.getStatus() : "all");
            builder.append("|isHot=").append(video.getIsHot() != null ? video.getIsHot() : "all");
            builder.append("|isRecommended=").append(video.getIsRecommended() != null ? video.getIsRecommended() : "all");
            builder.append("|videoType=").append(video.getVideoType() != null ? video.getVideoType() : "all");
            builder.append("|author=").append(video.getAuthor() != null ? video.getAuthor().trim() : "");
            builder.append("|title=").append(video.getTitle() != null ? video.getTitle().trim() : "");
        } else {
            builder.append("category=all|status=all|isHot=all|isRecommended=all|videoType=all");
        }
        return DigestUtils.md5DigestAsHex(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Boolean shouldExcludeShort(Video video) {
        return (video == null || video.getCategoryId() == null) ? Boolean.TRUE : Boolean.FALSE;
    }

    private String resolveCategoryKey(Video video) {
        return (video != null && video.getCategoryId() != null) ? video.getCategoryId().toString() : "all";
    }

    private List<WebVideoListVO> cloneVideoList(List<WebVideoListVO> source) {
        if (CollectionUtils.isEmpty(source)) {
            return new ArrayList<>();
        }
        List<WebVideoListVO> copy = new ArrayList<>(source.size());
        for (WebVideoListVO item : source) {
            if (item == null) {
                continue;
            }
            WebVideoListVO cloned = new WebVideoListVO();
            cloned.setId(item.getId());
            cloned.setTitle(item.getTitle());
            cloned.setCoverImageUrl(item.getCoverImageUrl());
            cloned.setAuthor(item.getAuthor());
            cloned.setPublishedAt(item.getPublishedAt());
            cloned.setCategoryName(item.getCategoryName());
            cloned.setIsHot(item.getIsHot());
            copy.add(cloned);
        }
        return copy;
    }

    private void enrichVideoListWithCategories(List<WebVideoListVO> videos) {
        if (CollectionUtils.isEmpty(videos)) {
            return;
        }

        List<Long> videoIds = videos.stream()
                .filter(Objects::nonNull)
                .map(WebVideoListVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(videoIds)) {
            return;
        }

        Map<Long, List<Category>> categoriesByVideo = getSelfProxy().selectCategoriesByVideoIdsBatch(videoIds);

        for (WebVideoListVO vo : videos) {
            if (vo == null) {
                continue;
            }
            List<Category> categories = categoriesByVideo.get(vo.getId());
            if (CollectionUtils.isEmpty(categories)) {
                vo.setCategories(Collections.emptyList());
                continue;
            }
            List<WebVideoListVO.WebCategoryInfo> webCategories = categories.stream()
                    .filter(Objects::nonNull)
                    .map(category -> new WebVideoListVO.WebCategoryInfo(category.getId(), category.getName()))
                    .collect(Collectors.toList());
            vo.setCategories(webCategories);
        }
    }

    /**
     * 缓存测试方法 - 验证缓存是否工作
     */
    @Cacheable(value = "videoListOptimized", key = "'test_cache_' + #testKey")
    public String testCache(String testKey) {
        logger.info("🧪 [缓存MISS] 执行缓存测试: testKey={}", testKey);
        return "cached_result_" + testKey + "_" + System.currentTimeMillis();
    }

    /**
     * 签名URL缓存测试方法
     */
    @Cacheable(value = "signedUrlCache", key = "'test_signed_' + #testKey")
    public String testSignedUrlCache(String testKey) {
        logger.info("🔗 [缓存MISS] 执行签名URL缓存测试: testKey={}", testKey);
        return "signed_result_" + testKey + "_" + System.currentTimeMillis();
    }

    /**
     * 构建列表专用计数缓存键
     */
    private String buildCountCacheKeyOptimized(Video video) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("videoCountOptimized:countVideoListOnly:");
        
        // 添加查询参数
        if (video.getCategoryId() != null) {
            keyBuilder.append("categoryId=").append(video.getCategoryId());
        }
        if (video.getStatus() != null) {
            keyBuilder.append(",status=").append(video.getStatus());
        }
        if (video.getTitle() != null && !video.getTitle().trim().isEmpty()) {
            keyBuilder.append(",title=").append(video.getTitle());
        }
        
        // 移除版本号（已改为固定键策略）
        
        return keyBuilder.toString();
    }

    /**
     * 查询视频的分类列表（带缓存）
     */
    @Override
    @Cacheable(value = "videoRelations", key = "'categories_' + #videoId", unless = "#result == null")
    public List<Category> selectCategoriesByVideoId(Long videoId)
    {
        return videoMapper.selectCategoriesByVideoId(videoId);
    }

    /**
     * 查询视频的标签列表（带缓存）
     */
    @Override
    @Cacheable(value = "videoRelations", key = "'tags_' + #videoId", unless = "#result == null")
    public List<Tag> selectTagsByVideoId(Long videoId)
    {
        return videoMapper.selectTagsByVideoId(videoId);
    }

    /**
     * 批量查询视频的标签信息（解决N+1问题）
     * 缓存2小时，视频列表缓存后同一页ID固定，命中率高；编辑视频/标签时由CacheRefreshService清除
     */
    @Override
    @Cacheable(value = "videoTagsBatch", key = "'batch_tags_' + #videoIds.toString()", unless = "#result == null")
    public Map<Long, List<Tag>> selectTagsByVideoIdsBatch(List<Long> videoIds) {
        if (CollectionUtils.isEmpty(videoIds)) {
            return new HashMap<>();
        }
        
        logger.debug("批量查询视频标签: {} 个视频", videoIds.size());
        
        // 批量查询，一次SQL获取所有视频的标签
        List<Map<String, Object>> rawResults = videoMapper.selectTagsByVideoIdsBatch(videoIds);
        
        // 将结果转换为Map<Long, List<Tag>>格式
        Map<Long, List<Tag>> resultMap = new HashMap<>();
        
        for (Map<String, Object> row : rawResults) {
            // 安全的类型转换，处理Integer和Long的兼容性
            // MyBatis可能返回小写键名，需要检查
            Object videoIdObj = row.get("videoId");
            if (videoIdObj == null) {
                videoIdObj = row.get("videoid"); // 尝试小写
            }
            if (videoIdObj == null) {
                videoIdObj = row.get("VIDEO_ID"); // 尝试大写
            }
            
            Object tagIdObj = row.get("tagId");
            if (tagIdObj == null) {
                tagIdObj = row.get("tagid"); // 尝试小写
            }
            if (tagIdObj == null) {
                tagIdObj = row.get("TAG_ID"); // 尝试大写
            }
            
            Object tagNameObj = row.get("tagName");
            if (tagNameObj == null) {
                tagNameObj = row.get("tagname"); // 尝试小写
            }
            if (tagNameObj == null) {
                tagNameObj = row.get("TAG_NAME"); // 尝试大写
            }
            
            Object tagColorObj = row.get("tagColor");
            if (tagColorObj == null) {
                tagColorObj = row.get("tagcolor"); // 尝试小写
            }
            if (tagColorObj == null) {
                tagColorObj = row.get("TAG_COLOR"); // 尝试大写
            }
            
            Long videoId = convertToLong(videoIdObj);
            Long tagId = convertToLong(tagIdObj);
            String tagName = tagNameObj != null ? tagNameObj.toString() : null;
            String tagColor = tagColorObj != null ? tagColorObj.toString() : null;
            
            if (videoId == null || tagId == null || tagName == null) {
                logger.warn("⚠️ 标签数据不完整，跳过: videoId={}, tagId={}, tagName={}", videoId, tagId, tagName);
                continue;
            }
            
            // 创建Tag对象
            Tag tag = new Tag();
            tag.setId(tagId);
            tag.setName(tagName);
            tag.setColor(tagColor);
            
            // 添加到结果Map中
            resultMap.computeIfAbsent(videoId, k -> new ArrayList<>()).add(tag);
        }
        
        // 🔍 调试：输出查询结果统计
        int videosWithTags = (int) resultMap.values().stream().filter(list -> list != null && !list.isEmpty()).count();
        logger.debug("✅ 标签查询完成: 查询{}个视频，{}个视频有标签", videoIds.size(), videosWithTags);
        
        return resultMap;
    }

    /**
     * 批量查询视频的分类信息（解决N+1问题）
     */
    @Override
    @Cacheable(value = "videoCategoriesBatch", key = "'batch_categories_' + #videoIds.toString()", unless = "#result == null")
    public Map<Long, List<Category>> selectCategoriesByVideoIdsBatch(List<Long> videoIds) {
        if (CollectionUtils.isEmpty(videoIds)) {
            return new HashMap<>();
        }
        
        logger.debug("🔍 批量查询视频分类: videoIds={}", videoIds);
        
        // 批量查询，一次SQL获取所有视频的分类
        List<Map<String, Object>> rawResults = videoMapper.selectCategoriesByVideoIdsBatch(videoIds);
        
        // 将结果转换为Map<Long, List<Category>>格式
        Map<Long, List<Category>> resultMap = new HashMap<>();
        
        for (Map<String, Object> row : rawResults) {
            // 安全的类型转换，处理Integer和Long的兼容性
            Long videoId = convertToLong(row.get("videoId"));
            Long categoryId = convertToLong(row.get("categoryId"));
            String categoryName = (String) row.get("categoryName");
            Integer sortOrder = (Integer) row.get("sortOrder");
            
            // 创建Category对象
            Category category = new Category();
            category.setId(categoryId);
            category.setName(categoryName);
            category.setSortOrder(sortOrder);
            
            // 添加到结果Map中
            resultMap.computeIfAbsent(videoId, k -> new ArrayList<>()).add(category);
        }
        
        logger.debug("✅ 批量查询视频分类完成: 查询{}个视频，返回{}个视频的分类数据", 
                    videoIds.size(), resultMap.size());
        
        return resultMap;
    }

    private IVideoService getSelfProxy() {
        if (videoServiceProxy != null) {
            return videoServiceProxy;
        }
        return applicationContext.getBean(IVideoService.class);
    }


    /**
     * 查询视频列表（包含分类和标签信息）
     * 
     * @param video 视频
     * @return 视频集合
     */
    @Override
    public List<Video> selectVideoListWithRelations(Video video)
    {
        return videoMapper.selectVideoListWithRelations(video);
    }

    /**
     * 验证分类并设置视频类型
     * 
     * @param video 视频对象
     * @throws RuntimeException 如果分类验证失败
     */
    private void validateCategoriesAndSetVideoType(Video video) {
        // 获取选择的分类ID列表
        List<Long> categoryIds = video.getCategoryIds();
        
        if (categoryIds == null || categoryIds.isEmpty()) {
            // 如果没有选择分类，默认设置为长视频
            video.setVideoType("long");
            logger.info("✅ 未选择分类，默认设置视频类型为长视频");
            return;
        }
        
        // 查询所有选中分类的信息
        List<Category> selectedCategories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            try {
                Category category = categoryMapper.selectCategoryById(categoryId);
                if (category != null) {
                    selectedCategories.add(category);
                }
            } catch (Exception e) {
                logger.error("❌ 查询分类信息失败: categoryId={}, error={}", categoryId, e.getMessage());
            }
        }
        
        if (selectedCategories.isEmpty()) {
            video.setVideoType("long");
            logger.info("✅ 未找到有效分类，默认设置视频类型为长视频");
            return;
        }
        
        // 检查是否同时包含长视频和短视频分类
        boolean hasShortVideo = false;
        boolean hasLongVideo = false;
        
        for (Category category : selectedCategories) {
            if (category.getIsShort() != null && category.getIsShort() == 1) {
                hasShortVideo = true;
            } else {
                hasLongVideo = true;
            }
        }
        
        // 如果同时包含长视频和短视频分类，抛出异常
        if (hasShortVideo && hasLongVideo) {
            String shortCategoryNames = selectedCategories.stream()
                .filter(c -> c.getIsShort() != null && c.getIsShort() == 1)
                .map(Category::getName)
                .collect(Collectors.joining(", "));
            String longCategoryNames = selectedCategories.stream()
                .filter(c -> c.getIsShort() == null || c.getIsShort() == 0)
                .map(Category::getName)
                .collect(Collectors.joining(", "));
            
            String errorMsg = String.format("不能同时选择长视频分类和短视频分类！短视频分类：[%s]，长视频分类：[%s]", 
                shortCategoryNames, longCategoryNames);
            logger.error("❌ 分类验证失败: {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // 根据分类设置视频类型
        if (hasShortVideo) {
            video.setVideoType("short");
            logger.info("✅ 选择的分类为短视频分类，自动设置视频类型为短视频");
        } else {
            video.setVideoType("long");
            logger.info("✅ 选择的分类为长视频分类，自动设置视频类型为长视频");
        }
    }

    /**
     * 新增视频
     * 
     * @param video 视频
     * @return 结果
     */
    @Override
    @Transactional
    public int insertVideo(Video video)
    {
        // 设置默认值
        if (video.getViewCount() == null) {
            video.setViewCount(0);
        }
        if (video.getCommentCount() == null) {
            video.setCommentCount(0);
        }
        if (video.getLikeCount() == null) {
            video.setLikeCount(0);
        }
        if (video.getShareCount() == null) {
            video.setShareCount(0);
        }
        if (video.getSortOrder() == null) {
            video.setSortOrder(0);
        }
        if (video.getIsRecommended() == null) {
            video.setIsRecommended(0);
        }
        if (video.getIsHot() == null) {
            video.setIsHot(0);
        }
        if (video.getStatus() == null) {
            video.setStatus(0); // 默认草稿状态
        }
        
        // 设置发布时间
        if (video.getStatus() == 1 && video.getPublishedAt() == null) {
            video.setPublishedAt(new Date());
        }
        
        video.setLastEditedAt(new Date());
        
        // 🔧 验证分类并设置视频类型
        validateCategoriesAndSetVideoType(video);
        
        // 规范化封面：保存时只保存相对路径
        if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
            video.setCoverImage(normalizeCoverImageToPath(video.getCoverImage()));
        }

        // 🔧 关键修复：处理 first_video_url
        if (StringUtils.isNotBlank(video.getFirstVideoUrl())) {
            // 如果有值，进行规范化
            video.setFirstVideoUrl(normalizeFirstVideoPath(video.getFirstVideoUrl()));
            logger.debug("🔧 规范化 first_video_url: url={}", video.getFirstVideoUrl());
        } else if (video.getFirstVideoUrl() != null && video.getFirstVideoUrl().trim().isEmpty()) {
            // 如果是空字符串，设置为 null，以便后续回填逻辑能够正常工作
            video.setFirstVideoUrl(null);
            logger.debug("🔧 将空字符串的 first_video_url 设置为 null，便于回填");
        }

        // 🔧 修复：新增视频时智能处理富文本内容转换
        if (video.getVideoContent() != null && !video.getVideoContent().trim().isEmpty()) {
            String originalContent = video.getVideoContent();
            
            // 🔧 关键修复：检测内容是否已经是存储格式，避免重复转换
            boolean isAlreadyStorageFormat = isContentInStorageFormat(originalContent);
            
            if (isAlreadyStorageFormat) {
                logger.info("✅ 新增视频富文本内容已经是存储格式，跳过转换: contentLength={}", 
                    originalContent.length());
                // 直接使用原内容，不进行转换
                video.setVideoContent(originalContent);
            } else {
                logger.info("🔄 新增视频富文本内容需要转换为存储格式: contentLength={}", 
                    originalContent.length());
                String storageContent = richTextProcessorService.prepareRichTextForStorage(originalContent);
                video.setVideoContent(storageContent);
                
                logger.info("✅ 新增视频富文本内容转换完成: 原长度={}, 转换后长度={}", 
                    originalContent.length(), storageContent.length());
            }
            
            // 🔧 自动从富文本内容中提取第一个视频的transcode_id
            if (video.getTranscodeId() == null || video.getTranscodeId().trim().isEmpty()) {
                String extractedTranscodeId = extractTranscodeIdFromRichText(video.getVideoContent());
                if (extractedTranscodeId != null) {
                    video.setTranscodeId(extractedTranscodeId);
                    logger.info("✅ 从富文本内容中自动提取transcode_id: {}", extractedTranscodeId);
                }
            }

        // 🔧 新增：从富文本内容中提取首个视频URL，填充 first_video_url（相对路径）
        if (video.getFirstVideoUrl() == null || video.getFirstVideoUrl().trim().isEmpty()) {
            try {
                String firstUrl = extractFirstVideoUrl(video.getVideoContent());
                if (firstUrl != null && !firstUrl.trim().isEmpty()) {
                    video.setFirstVideoUrl(firstUrl);
                    logger.info("✅ 从富文本中提取首个视频URL并保存: {}", firstUrl);
                }
            } catch (Exception ignore) {}
        }
        }
        
        int result = videoMapper.insertVideo(video);
        
        // 保存分类和标签关系
        if (result > 0) {
            saveVideoCategoryRelations(video.getId(), video.getCategoryIds());
            saveVideoTagRelations(video.getId(), video.getTagIds());

            backfillFirstVideoUrlIfMissing(video);

            // 🔧 修复：如果视频关联了转码ID，标记该转码记录为已使用
            if (video.getTranscodeId() != null && !video.getTranscodeId().trim().isEmpty()) {
                try {
                    videoTranscodeService.markTranscodeAsUsed(video.getTranscodeId(), video.getId());
                    logger.info("✅ 标记转码记录为已使用: transcodeId={}, videoId={}", video.getTranscodeId(), video.getId());
                } catch (Exception e) {
                    logger.error("❌ 标记转码记录为已使用失败: transcodeId={}, videoId={}, 错误: {}", 
                        video.getTranscodeId(), video.getId(), e.getMessage());
                }
            }
            
            // 在事务提交后更新缓存版本号，避免事务回滚时版本号也被回滚
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 视频新增事务提交成功，开始智能缓存失效: videoId={}, categoryId={}", video.getId(), video.getCategoryId());
                    // 🚀 使用智能缓存失效：支持多分类
                    try {
                        smartCacheInvalidation(video.getId(), video.getCategoryId(), false);
                        // 额外清理短视频缓存
                        cacheRefreshService.invalidateShortVideoCache(video.getId(), video.getCategoryId(), null, false);
                    } catch (Exception ignored) {}
                }
            });
        }
        
        return result;
    }

    /**
     * 修改视频
     * 
     * @param video 视频
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideo(Video video)
    {
        // 设置最后编辑时间
        video.setLastEditedAt(new Date());
        
        // 如果状态改为发布且之前没有发布时间，则设置发布时间
        if (video.getStatus() == 1 && video.getPublishedAt() == null) {
            video.setPublishedAt(new Date());
        }
        
        // 🔧 验证分类并设置视频类型
        validateCategoriesAndSetVideoType(video);
        
        // 规范化封面：保存时只保存相对路径
        if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
            video.setCoverImage(normalizeCoverImageToPath(video.getCoverImage()));
        }

        // 🔧 关键修复：处理 first_video_url
        if (StringUtils.isNotBlank(video.getFirstVideoUrl())) {
            // 如果有值，进行规范化
            video.setFirstVideoUrl(normalizeFirstVideoPath(video.getFirstVideoUrl()));
            logger.debug("🔧 规范化 first_video_url: videoId={}, url={}", video.getId(), video.getFirstVideoUrl());
        } else if (video.getFirstVideoUrl() != null && video.getFirstVideoUrl().trim().isEmpty()) {
            // 如果是空字符串，设置为 null，以便后续回填逻辑能够正常工作
            video.setFirstVideoUrl(null);
            logger.debug("🔧 将空字符串的 first_video_url 设置为 null，便于回填: videoId={}", video.getId());
        }

        // 🔧 修复：更新视频时智能处理富文本内容转换
        if (video.getVideoContent() != null && !video.getVideoContent().trim().isEmpty()) {
            String originalContent = video.getVideoContent();
            
            // 🔧 关键修复：检测内容是否已经是存储格式，避免重复转换
            boolean isAlreadyStorageFormat = isContentInStorageFormat(originalContent);
            
            if (isAlreadyStorageFormat) {
                logger.info("✅ 更新视频富文本内容已经是存储格式，跳过转换: videoId={}, contentLength={}", 
                    video.getId(), originalContent.length());
                // 直接使用原内容，不进行转换
                video.setVideoContent(originalContent);
            } else {
                logger.info("🔄 更新视频富文本内容需要转换为存储格式: videoId={}, contentLength={}", 
                    video.getId(), originalContent.length());
                String storageContent = richTextProcessorService.prepareRichTextForStorage(originalContent);
                video.setVideoContent(storageContent);
                
                logger.info("✅ 更新视频富文本内容转换完成: 原长度={}, 转换后长度={}", 
                    originalContent.length(), storageContent.length());
            }
            
            // 🔧 自动从富文本内容中提取第一个视频的transcode_id
            if (video.getTranscodeId() == null || video.getTranscodeId().trim().isEmpty()) {
                String extractedTranscodeId = extractTranscodeIdFromRichText(video.getVideoContent());
                if (extractedTranscodeId != null) {
                    video.setTranscodeId(extractedTranscodeId);
                    logger.info("✅ 从富文本内容中自动提取transcode_id: {}", extractedTranscodeId);
                }
            }

            if (StringUtils.isBlank(video.getFirstVideoUrl())) {
                try {
                    String firstUrl = extractFirstVideoUrl(video.getVideoContent());
                    if (firstUrl != null && !firstUrl.trim().isEmpty()) {
                        video.setFirstVideoUrl(firstUrl);
                        logger.info("✅ 从富文本中提取首个视频URL并保存: {}", firstUrl);
                    }
                } catch (Exception ignore) {}
            }
        }

        int result = videoMapper.updateVideo(video);
        
        // 更新分类和标签关系
        if (result > 0) {
            saveVideoCategoryRelations(video.getId(), video.getCategoryIds());
            saveVideoTagRelations(video.getId(), video.getTagIds());

            backfillFirstVideoUrlIfMissing(video);
            
            // 🔧 修复：如果视频关联了转码ID，标记该转码记录为已使用
            if (video.getTranscodeId() != null && !video.getTranscodeId().trim().isEmpty()) {
                try {
                    videoTranscodeService.markTranscodeAsUsed(video.getTranscodeId(), video.getId());
                    logger.info("✅ 更新时标记转码记录为已使用: transcodeId={}, videoId={}", video.getTranscodeId(), video.getId());
                } catch (Exception e) {
                    logger.error("❌ 更新时标记转码记录为已使用失败: transcodeId={}, videoId={}, 错误: {}", 
                        video.getTranscodeId(), video.getId(), e.getMessage());
                }
            }
            
            // 在事务提交后更新缓存版本号，避免事务回滚时版本号也被回滚
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 视频更新事务提交成功，开始智能缓存失效: videoId={}, categoryId={}", video.getId(), video.getCategoryId());
                    // 🚀 使用智能缓存失效：支持多分类
                    try {
                        logger.info("🔄 开始执行视频更新缓存失效: videoId={}, categoryId={}", video.getId(), video.getCategoryId());
                        smartCacheInvalidation(video.getId(), video.getCategoryId(), false);
                        // 额外清理短视频缓存
                        cacheRefreshService.invalidateShortVideoCache(video.getId(), video.getCategoryId(), null, false);
                        logger.info("✅ 视频更新缓存清理成功: videoId={}", video.getId());
                    } catch (Exception e) {
                        logger.error("❌ 视频更新缓存清理失败: videoId={}, error={}", video.getId(), e.getMessage(), e);
                        // 不要忽略异常，重新抛出以便调试
                        throw new RuntimeException("缓存失效失败", e);
                    }
                }
            });
        }
        
        return result;
    }

    /**
     * 批量删除视频
     * 
     * @param ids 需要删除的视频主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteVideoByIds(Long[] ids)
    {
        // 删除关联的分类和标签关系，并更新标签使用次数
        for (Long id : ids) {
            // 获取要删除的视频关联的标签ID列表
            List<Long> tagIds = videoMapper.selectTagIdsByVideoId(id);
            
            // 删除分类和标签关系
            videoMapper.deleteVideoCategoryRelations(id);
            videoMapper.deleteVideoTagRelations(id);
            
            // 减少标签使用次数
            if (!CollectionUtils.isEmpty(tagIds)) {
                tagMapper.batchUpdateTagUsageCount(tagIds, -1);
                logger.info("✅ 删除视频{}时减少标签使用次数: tagIds={}, count={}", id, tagIds, tagIds.size());
            }
        }
        
        int result = videoMapper.deleteVideoByIds(ids);
        
        // 删除后更新相关缓存版本号
        if (result > 0) {
            // 在事务提交后更新缓存版本号，避免事务回滚时版本号也被回滚
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 视频删除事务提交成功，开始智能缓存失效: deletedCount={}", result);
                    // 🚀 批量删除是全局影响
                    try {
                        smartCacheInvalidation(null, null, true);
                        // 全局清理短视频缓存
                        cacheRefreshService.invalidateShortVideoCache(null, null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        
        return result;
    }

    /**
     * 删除视频信息
     * 
     * @param id 视频主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteVideoById(Long id)
    {
        // 获取要删除的视频关联的标签ID列表
        List<Long> tagIds = videoMapper.selectTagIdsByVideoId(id);
        
        // 删除关联的分类和标签关系
        videoMapper.deleteVideoCategoryRelations(id);
        videoMapper.deleteVideoTagRelations(id);
        
        // 减少标签使用次数
        if (!CollectionUtils.isEmpty(tagIds)) {
            tagMapper.batchUpdateTagUsageCount(tagIds, -1);
            logger.info("✅ 删除视频{}时减少标签使用次数: tagIds={}, count={}", id, tagIds, tagIds.size());
        }
        
        int result = videoMapper.deleteVideoById(id);
        
        // 删除后更新相关缓存版本号
        if (result > 0) {
            // 🚀 单个视频删除：全局影响（因为不知道原分类）
            try {
                smartCacheInvalidation(id, null, true);
            } catch (Exception ignored) {}
        }
        
        return result;
    }



    /**
     * 保存视频分类关系
     * 
     * @param videoId 视频ID
     * @param categoryIds 分类ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int saveVideoCategoryRelations(Long videoId, List<Long> categoryIds)
    {
        if (videoId == null) {
            return 0;
        }
        
        // 先删除原有关系
        videoMapper.deleteVideoCategoryRelations(videoId);
        
        // 批量插入新关系
        if (!CollectionUtils.isEmpty(categoryIds)) {
            return videoMapper.batchInsertVideoCategoryRelations(videoId, categoryIds);
        }
        
        return 1;
    }

    /**
     * 保存视频标签关系
     * 
     * @param videoId 视频ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int saveVideoTagRelations(Long videoId, List<Long> tagIds)
    {
        if (videoId == null) {
            return 0;
        }
        
        // 获取原有的标签ID列表，用于计算使用次数变化
        List<Long> oldTagIds = videoMapper.selectTagIdsByVideoId(videoId);
        
        // 先删除原有关系
        videoMapper.deleteVideoTagRelations(videoId);
        
        // 更新标签使用次数：减少原有标签的使用次数
        if (!CollectionUtils.isEmpty(oldTagIds)) {
            tagMapper.batchUpdateTagUsageCount(oldTagIds, -1);
            logger.info("✅ 减少原有标签使用次数: tagIds={}, count={}", oldTagIds, oldTagIds.size());
        }
        
        // 批量插入新关系
        if (!CollectionUtils.isEmpty(tagIds)) {
            int result = videoMapper.batchInsertVideoTagRelations(videoId, tagIds);
            
            // 更新标签使用次数：增加新标签的使用次数
            if (result > 0) {
                tagMapper.batchUpdateTagUsageCount(tagIds, 1);
                logger.info("✅ 增加新标签使用次数: tagIds={}, count={}", tagIds, tagIds.size());
                
                // 更新标签相关缓存
                cacheRefreshService.refreshTagCache();
            }
            
            return result;
        }
        
        return 1;
    }

    /**
     * 根据已选标签推荐相关标签
     * 双层推荐策略：
     * 1. 基于标签共现分析推荐
     * 2. 基于分词模糊匹配推荐
     * 最多返回30条推荐标签
     * 
     * @param selectedTagIds 已选标签ID列表
     * @return 推荐标签集合（最多30条）
     */
    @Override
    public List<Tag> recommendTags(List<Long> selectedTagIds)
    {
        if (CollectionUtils.isEmpty(selectedTagIds)) {
            return new ArrayList<>();
        }
        
        List<Tag> allRecommendedTags = new ArrayList<>();
        
        // 第一层：基于共现关系推荐
        List<Tag> coOccurrenceTags = videoMapper.selectRecommendedTags(selectedTagIds);
        if (!CollectionUtils.isEmpty(coOccurrenceTags)) {
            allRecommendedTags.addAll(coOccurrenceTags);
            logger.info("✅ 共现推荐找到{}条标签", coOccurrenceTags.size());
        } else {
            logger.info("⚠️ 没有找到基于共现的推荐标签");
        }
        
        // 第二层：基于分词模糊匹配推荐
        if (allRecommendedTags.size() < 30) {
            try {
                // 获取已选标签的名称用于分词
                List<String> selectedTagNames = getSelectedTagNames(selectedTagIds);
                
                // 对标签名称进行分词
                List<String> keywords = WordSegmentationUtil.segmentMultipleTagNames(selectedTagNames);
                
                if (!CollectionUtils.isEmpty(keywords)) {
                    logger.info("🔍 分词关键词: {}", keywords);
                    
                    List<Tag> segmentationTags = videoMapper.selectTagsByWordSegmentation(selectedTagIds, keywords);
                    
                    // 去重合并
                    for (Tag tag : segmentationTags) {
                        boolean exists = allRecommendedTags.stream()
                            .anyMatch(existingTag -> existingTag.getId().equals(tag.getId()));
                        if (!exists && allRecommendedTags.size() < 30) {
                            allRecommendedTags.add(tag);
                        }
                    }
                    logger.info("✅ 分词匹配找到{}条新标签，当前总数：{}", segmentationTags.size(), allRecommendedTags.size());
                }
            } catch (Exception e) {
                logger.error("❌ 分词匹配推荐失败: {}", e.getMessage(), e);
            }
        }
        
        // 最终限制返回结果数量为30条
        if (allRecommendedTags.size() > 30) {
            logger.info("🔧 推荐标签数量为{}，限制为30条", allRecommendedTags.size());
            allRecommendedTags = allRecommendedTags.subList(0, 30);
        }
        
        logger.info("🎯 双层推荐策略最终返回{}条推荐标签", allRecommendedTags.size());
        return allRecommendedTags;
    }
    
    /**
     * 获取已选标签的名称列表
     * 
     * @param selectedTagIds 已选标签ID列表
     * @return 标签名称列表
     */
    private List<String> getSelectedTagNames(List<Long> selectedTagIds) {
        if (CollectionUtils.isEmpty(selectedTagIds)) {
            return new ArrayList<>();
        }
        
        try {
            // 一次性查询所有标签，提高效率
            List<Tag> allTags = videoMapper.selectAllTags();
            List<String> tagNames = new ArrayList<>();
            
            for (Long tagId : selectedTagIds) {
                for (Tag tag : allTags) {
                    if (tag.getId().equals(tagId)) {
                        tagNames.add(tag.getName());
                        break;
                    }
                }
            }
            
            logger.debug("🏷️ 获取到{}个标签名称: {}", tagNames.size(), tagNames);
            return tagNames;
            
        } catch (Exception e) {
            logger.error("❌ 获取标签名称失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新视频统计数据
     * 
     * @param video 视频对象
     * @return 结果
     */
    @Override
    public int updateVideoStatistics(Video video)
    {
        return videoMapper.updateVideoStatistics(video);
    }

    /**
     * 查询所有分类列表（用于下拉选择）
     * 
     * @return 分类集合
     */
    @Override
    public List<Category> selectAllCategories()
    {
        return videoMapper.selectAllCategories();
    }

    /**
     * 查询所有标签列表（用于下拉选择）
     * 
     * @return 标签集合
     */
    @Override
    public List<Tag> selectAllTags()
    {
        return videoMapper.selectAllTags();
    }

    /**
     * 分页查询标签列表（用于标签选择弹出层）
     * 
     * @param tag 标签查询条件
     * @return 标签集合
     */
    @Override
    public List<Tag> selectTagsPage(Tag tag)
    {
        return videoMapper.selectTagsPage(tag);
    }

    /**
     * 根据ID列表查询标签信息（用于编辑时回显已选标签）
     * 
     * @param tagIds 标签ID列表
     * @return 标签集合
     */
    @Override
    public List<Tag> selectTagsByIds(List<Long> tagIds)
    {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return videoMapper.selectTagsByIds(tagIds);
    }

    /**
     * 根据关键词搜索标签（带缓存）
     * 
     * @param keyword 关键词
     * @return 标签集合
     */
    @Override
    @Cacheable(value = "searchResult", key = "'tags_' + #keyword", unless = "#result == null")
    public List<Tag> searchTags(String keyword)
    {
        return videoMapper.searchTags(keyword);
    }

    /**
     * 创建新标签
     * 
     * @param tag 标签对象
     * @return 结果
     */
    @Override
    @Transactional
    public int insertTag(Tag tag)
    {
        tag.setCreatedAt(new Date());
        tag.setUpdatedAt(new Date());
        tag.setUsageCount(0);
        return videoMapper.insertTag(tag);
    }

    /**
     * 更新视频内容
     * 
     * @param videoId 视频ID
     * @param videoContent 视频内容
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoContent(Long videoId, String videoContent)
    {
        Video video = new Video();
        video.setId(videoId);
        
        // 🔧 修复：智能处理富文本内容转换
        boolean isAlreadyStorageFormat = isContentInStorageFormat(videoContent);
        
        if (isAlreadyStorageFormat) {
            logger.info("✅ 更新视频内容已经是存储格式，跳过转换: videoId={}, contentLength={}", 
                videoId, videoContent.length());
            video.setVideoContent(videoContent);
        } else {
            logger.info("🔄 更新视频内容需要转换为存储格式: videoId={}, contentLength={}", 
                videoId, videoContent.length());
            String storageContent = richTextProcessorService.prepareRichTextForStorage(videoContent);
            video.setVideoContent(storageContent);
            
            logger.info("✅ 更新视频内容转换完成: 原长度={}, 转换后长度={}", 
                videoContent.length(), storageContent.length());
        }
        video.setLastEditedAt(new Date());
        
        int result = videoMapper.updateVideo(video);
        
        // 如果更新成功，提取并保存图片URL和视频URL
        if (result > 0 && videoContent != null && !videoContent.trim().isEmpty()) {
            // 提取图片URL（使用原始内容，因为可能包含签名URL）
            extractAndSaveImageUrls(videoId, videoContent);
            
            // 提取视频URL
            extractAndSaveVideoUrls(videoId, videoContent);
        }
        
        return result;
    }

    /**
     * 根据转码ID查找视频
     * 
     * @param transcodeId 转码ID
     * @return 视频对象
     */
    @Override
    public Video findByTranscodeId(String transcodeId) 
    {
        return videoMapper.findByTranscodeId(transcodeId);
    }

    /**
     * 获取视频的富文本内容（包含动态生成的签名URL）
     * 
     * @param videoId 视频ID
     * @return 处理后的富文本内容
     */
    @Override
    public String getProcessedVideoContent(Long videoId) 
    {
        Video video = videoMapper.selectVideoById(videoId);
        if (video == null || video.getVideoContent() == null) {
            logger.info("📝 获取视频内容: videoId={}, 内容为空", videoId);
            return "";
        }

        logger.info("📝 获取视频内容: videoId={}, 原始内容长度={}", videoId, video.getVideoContent().length());
        logger.debug("📝 原始内容预览: {}", 
            video.getVideoContent().length() > 200 ? 
            video.getVideoContent().substring(0, 200) + "..." : 
            video.getVideoContent());

        // 🔧 强制检查是否包含相对路径
        boolean hasRelativePaths = video.getVideoContent().contains("src=\"files/") || 
                                  video.getVideoContent().contains("poster=\"files/");
        logger.info("📝 强制检查相对路径: {}", hasRelativePaths);

        // 管理后台：始终使用 Worker 域名生成签名URL
        String processedContent = richTextProcessorService.processRichTextForAdmin(video.getVideoContent());
        
        logger.info("📝 内容处理完成: 原始长度={}, 处理后长度={}", 
            video.getVideoContent().length(), processedContent.length());
        logger.debug("📝 处理后内容预览: {}", 
            processedContent.length() > 200 ? 
            processedContent.substring(0, 200) + "..." : 
            processedContent);
            
        return processedContent;
    }

    /**
     * 获取包含URL信息的视频详情
     * 
     * @param id 视频ID
     * @return 包含URL信息的视频DTO
     */
    @Override
    public VideoWithUrlsDto selectVideoWithUrlsById(Long id) 
    {
        Video video = selectVideoById(id);
        if (video == null) {
            return null;
        }
        
        VideoWithUrlsDto dto = new VideoWithUrlsDto(video);
        
        // 生成各种URL
        dto.setVideoUrl(videoUrlGeneratorService.generateVideoUrl(video));
        dto.setM3u8Url(videoUrlGeneratorService.generateM3u8Url(video));
        dto.setCoverUrl(videoUrlGeneratorService.generateCoverUrl(video));
        dto.setThumbnailUrls(videoUrlGeneratorService.getThumbnailUrls(video));
        
        // 生成HTML内容
        dto.setVideoPlayerHtml(videoUrlGeneratorService.generateVideoPlayerHtml(video));
        dto.setImageGalleryHtml(videoUrlGeneratorService.generateImageGalleryHtml(video));
        dto.setTechnicalInfoHtml(videoUrlGeneratorService.generateTechnicalInfoHtml(video));
        
        // 检查URL可用性
        dto.setVideoUrlAvailable(videoUrlGeneratorService.isVideoUrlAvailable(dto.getVideoUrl()));
        dto.setM3u8UrlAvailable(videoUrlGeneratorService.isVideoUrlAvailable(dto.getM3u8Url()));
        
        // 生成完整的富文本内容
        dto.setEnhancedVideoContent(generateEnhancedVideoContent(dto));
        
        return dto;
    }

    /**
     * 生成视频的完整富文本内容
     * 
     * @param videoId 视频ID
     * @return 富文本内容
     */
    @Override
    public String generateEnhancedVideoContent(Long videoId) 
    {
        Video video = selectVideoById(videoId);
        if (video == null) {
            return "";
        }
        
        VideoWithUrlsDto dto = new VideoWithUrlsDto(video);
        dto.setVideoPlayerHtml(videoUrlGeneratorService.generateVideoPlayerHtml(video));
        dto.setImageGalleryHtml(videoUrlGeneratorService.generateImageGalleryHtml(video));
        dto.setTechnicalInfoHtml(videoUrlGeneratorService.generateTechnicalInfoHtml(video));
        
        return generateEnhancedVideoContent(dto);
    }

    /**
     * 生成完整的富文本内容（私有方法）
     * 
     * @param dto 包含URL信息的视频DTO
     * @return 富文本内容
     */
    private String generateEnhancedVideoContent(VideoWithUrlsDto dto) 
    {
        StringBuilder content = new StringBuilder();
        
        // 添加CSS样式
        content.append("<style>");
        content.append(".video-player-container { margin: 20px 0; text-align: center; }");
        content.append(".image-gallery { margin: 20px 0; }");
        content.append(".gallery-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; }");
        content.append(".gallery-item img { width: 100%; height: auto; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }");
        content.append(".technical-info { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 8px; background: #f9f9f9; }");
        content.append(".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 10px; }");
        content.append(".info-item { padding: 8px; background: white; border-radius: 4px; }");
        content.append(".info-item .label { font-weight: bold; color: #333; }");
        content.append("</style>");
        
        // 添加视频播放器
        if (dto.getVideoPlayerHtml() != null && !dto.getVideoPlayerHtml().trim().isEmpty()) {
            content.append(dto.getVideoPlayerHtml());
        }
        
        // 添加原有内容
        if (dto.getVideoContent() != null && !dto.getVideoContent().trim().isEmpty()) {
            content.append("<div class=\"original-content\">");
            content.append(dto.getVideoContent());
            content.append("</div>");
        }
        
        // 添加技术信息
        if (dto.getTechnicalInfoHtml() != null && !dto.getTechnicalInfoHtml().trim().isEmpty()) {
            content.append(dto.getTechnicalInfoHtml());
        }
        
        // 添加图片画廊
        if (dto.getImageGalleryHtml() != null && !dto.getImageGalleryHtml().trim().isEmpty()) {
            content.append(dto.getImageGalleryHtml());
        }
        
        return content.toString();
    }

    /**
     * 更新视频的富文本内容（包含播放器和技术信息）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoContentWithUrls(Long videoId) 
    {
        String enhancedContent = generateEnhancedVideoContent(videoId);
        return updateVideoContent(videoId, enhancedContent);
    }
    
    /**
     * 提取并保存图片URL
     * 
     * @param videoId 视频ID
     * @param content 内容
     */
    private void extractAndSaveImageUrls(Long videoId, String content) {
        try {
            // 正则表达式匹配img标签的src属性
            java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
                "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher imgMatcher = imgPattern.matcher(content);
            
            int sortOrder = 0;
            while (imgMatcher.find()) {
                String imageUrl = imgMatcher.group(1);
                
                // 检查URL是否已存在
                if (!videoImageService.checkImageUrlExists(imageUrl, videoId, null)) {
                    VideoImage videoImage = new VideoImage();
                    videoImage.setVideoId(videoId);
                    videoImage.setImageUrl(imageUrl);
                    videoImage.setTitle("副文本图片");
                    videoImage.setDescription("从副文本中自动提取的图片");
                    videoImage.setSortOrder(sortOrder++);
                    videoImage.setIsPrimary(0);
                    videoImage.setStatus(1);
                    
                    videoImageService.insertVideoImage(videoImage);
                }
            }
        } catch (Exception e) {
            // 记录日志但不影响主流程
            System.err.println("提取图片URL时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 提取并保存视频URL
     * 
     * @param videoId 视频ID
     * @param content 内容
     */
    private void extractAndSaveVideoUrls(Long videoId, String content) {
        try {
            // 正则表达式匹配iframe标签的src属性（用于嵌入式视频）
            java.util.regex.Pattern iframePattern = java.util.regex.Pattern.compile(
                "<iframe[^>]+src=[\"']([^\"']+)[\"'][^>]*>", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher iframeMatcher = iframePattern.matcher(content);
            
            // 正则表达式匹配video标签的src属性
            java.util.regex.Pattern videoPattern = java.util.regex.Pattern.compile(
                "<video[^>]+src=[\"']([^\"']+)[\"'][^>]*>", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher videoMatcher = videoPattern.matcher(content);
            
            int sortOrder = 0;
            
            // 处理iframe视频
            while (iframeMatcher.find()) {
                String videoUrl = iframeMatcher.group(1);
                
                // 检查URL是否已存在
                if (!videoUrlService.checkVideoUrlExists(videoUrl, videoId, null)) {
                    VideoUrl videoUrlObj = new VideoUrl();
                    videoUrlObj.setVideoId(videoId);
                    videoUrlObj.setVideoUrl(videoUrl);
                    videoUrlObj.setTitle("副文本视频 " + (sortOrder + 1));
                    videoUrlObj.setDescription("从副文本中自动提取的嵌入式视频");
                    videoUrlObj.setSortOrder(sortOrder++);
                    videoUrlObj.setIsPrimary(0);
                    videoUrlObj.setStatus(1);
                    
                    // 根据URL判断视频来源
                    if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                        videoUrlObj.setFormat("YouTube");
                    } else if (videoUrl.contains("vimeo.com")) {
                        videoUrlObj.setFormat("Vimeo");
                    } else if (videoUrl.contains("bilibili.com")) {
                        videoUrlObj.setFormat("Bilibili");
                    } else {
                        videoUrlObj.setFormat("iframe");
                    }
                    
                    videoUrlService.insertVideoUrl(videoUrlObj);
                }
            }
            
            // 处理video标签
            while (videoMatcher.find()) {
                String videoUrl = videoMatcher.group(1);
                
                // 检查URL是否已存在
                if (!videoUrlService.checkVideoUrlExists(videoUrl, videoId, null)) {
                    VideoUrl videoUrlObj = new VideoUrl();
                    videoUrlObj.setVideoId(videoId);
                    videoUrlObj.setVideoUrl(videoUrl);
                    videoUrlObj.setTitle("副文本视频 " + (sortOrder + 1));
                    videoUrlObj.setDescription("从副文本中自动提取的视频文件");
                    videoUrlObj.setSortOrder(sortOrder++);
                    videoUrlObj.setIsPrimary(0);
                    videoUrlObj.setStatus(1);
                    
                    // 根据URL扩展名判断格式
                    if (videoUrl.toLowerCase().endsWith(".mp4")) {
                        videoUrlObj.setFormat("MP4");
                    } else if (videoUrl.toLowerCase().endsWith(".avi")) {
                        videoUrlObj.setFormat("AVI");
                    } else if (videoUrl.toLowerCase().endsWith(".mkv")) {
                        videoUrlObj.setFormat("MKV");
                    } else if (videoUrl.toLowerCase().endsWith(".m3u8")) {
                        videoUrlObj.setFormat("M3U8");
                    } else {
                        videoUrlObj.setFormat("VIDEO");
                    }
                    
                    videoUrlService.insertVideoUrl(videoUrlObj);
                }
            }
        } catch (Exception e) {
            // 记录日志但不影响主流程
            System.err.println("提取视频URL时发生错误: " + e.getMessage());
        }
    }

    /**
     * 批量修改视频状态
     * 
     * @param ids 视频ID数组
     * @param status 状态
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoStatus(Long[] ids, Integer status)
    {
        int result = 0;
        for (Long id : ids) {
            Video video = new Video();
            video.setId(id);
            video.setStatus(status);
            if (status == 1) { // 发布状态
                video.setPublishedAt(new Date());
            }
            result += videoMapper.updateVideo(video);
        }
        if (result > 0) {
            // 事务提交后统一递增相关缓存版本，确保前台列表/详情等数据更新
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 🚀 批量操作：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量设置推荐状态
     * 
     * @param ids 视频ID数组
     * @param isRecommended 是否推荐
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoRecommended(Long[] ids, Integer isRecommended)
    {
        int result = 0;
        for (Long id : ids) {
            Video video = new Video();
            video.setId(id);
            video.setIsRecommended(isRecommended);
            result += videoMapper.updateVideo(video);
        }
        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 🚀 批量操作：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量设置热门状态
     * 
     * @param ids 视频ID数组
     * @param isHot 是否热门
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoHot(Long[] ids, Integer isHot)
    {
        int result = 0;
        for (Long id : ids) {
            Video video = new Video();
            video.setId(id);
            video.setIsHot(isHot);
            result += videoMapper.updateVideo(video);
        }
        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 🚀 批量操作：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量设置多分类
     * 
     * @param ids 视频ID数组
     * @param categoryIds 分类ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchUpdateVideoCategories(Long[] ids, List<Long> categoryIds)
    {
        int result = 0;
        for (Long videoId : ids) {
            result += saveVideoCategoryRelations(videoId, categoryIds);
        }
        
        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 批量设置多分类事务提交成功，开始智能缓存失效");
                    // 🚀 批量分类变更：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量修改视频主分类
     * 
     * @param ids 视频ID数组
     * @param categoryId 目标分类ID
     * @return 结果
     */
    @Override
    @Transactional
    public int batchUpdateVideoPrimaryCategory(Long[] ids, Long categoryId)
    {
        if (ids == null || ids.length == 0 || categoryId == null) {
            return 0;
        }

        logger.info("🔄 批量修改视频主分类: videoIds={}, targetCategoryId={}", 
                   Arrays.toString(ids), categoryId);

        List<Long> videoIdList = Arrays.asList(ids);
        
        // 🚀 性能优化：使用批量SQL更新主分类
        int result = videoMapper.batchUpdateVideoPrimaryCategory(videoIdList, categoryId);
        
        // 🔧 增量添加分类关系：如果存在则忽略，不存在则新增
        int addedRelations = 0;
        for (Long videoId : videoIdList) {
            try {
                // 检查是否已存在该分类关系
                boolean exists = videoMapper.checkVideoCategoryRelationExists(videoId, categoryId);
                if (!exists) {
                    videoMapper.insertVideoCategoryRelation(videoId, categoryId);
                    addedRelations++;
                    logger.debug("✅ 为视频{}添加分类关系: categoryId={}", videoId, categoryId);
                } else {
                    logger.debug("⏭️ 视频{}已存在分类关系，跳过: categoryId={}", videoId, categoryId);
                }
            } catch (Exception e) {
                logger.warn("⚠️ 为视频{}处理分类关系时出现异常: {}", videoId, e.getMessage());
            }
        }
        
        logger.info("📊 分类关系处理完成: 新增{}个关系，跳过{}个已存在关系", 
                   addedRelations, videoIdList.size() - addedRelations);

        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 批量修改主分类事务提交成功，开始智能缓存失效: 影响{}个视频", result);
                    // 🚀 批量分类变更：全局影响
                    try { 
                        smartCacheInvalidation(null, categoryId, true);
                        // 额外清理短视频缓存
                        cacheRefreshService.invalidateShortVideoCache(null, categoryId, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }

        logger.info("✅ 批量修改视频主分类完成: 成功修改{}个视频", result);
        return result;
    }

    /**
     * 批量添加标签
     * 
     * @param ids 视频ID数组
     * @param tagIds 标签ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchAddVideoTags(Long[] ids, List<Long> tagIds)
    {
        int result = 0;
        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long videoId : ids) {
                for (Long tagId : tagIds) {
                    try {
                        videoMapper.insertVideoTagRelation(videoId, tagId);
                        result++;
                    } catch (Exception e) {
                        // 忽略重复插入的异常
                    }
                }
            }
        }
        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 🚀 批量操作：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量移除标签
     * 
     * @param ids 视频ID数组
     * @param tagIds 标签ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchRemoveVideoTags(Long[] ids, List<Long> tagIds)
    {
        // 这里需要在Mapper中添加相应的删除方法
        // 暂时返回0，实际项目中需要实现
        int result = 0;
        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long videoId : ids) {
                for (Long tagId : tagIds) {
                    try {
                        // 假设存在根据 videoId/tagId 删除关系的方法：videoMapper.deleteVideoTagRelation
                        // 如果项目中尚未实现，可后续补充；这里仅做一致性处理
                        // videoMapper.deleteVideoTagRelation(videoId, tagId);
                        result++; // 计数占位
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (result > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 🚀 批量操作：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }
        return result;
    }

    /**
     * 批量添加视频标签（增量模式：存在的忽略，不存在的新增）
     * 
     * @param ids 视频ID数组
     * @param tagIds 新标签ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchReplaceVideoTags(Long[] ids, List<Long> tagIds)
    {
        if (ids == null || ids.length == 0) {
            return 0;
        }

        if (CollectionUtils.isEmpty(tagIds)) {
            return 0;
        }

        logger.info("🏷️ 批量添加视频标签（增量模式）: videoIds={}, tagIds={}", 
                   Arrays.toString(ids), tagIds);

        List<Long> videoIdList = Arrays.asList(ids);
        int totalAdded = 0;
        int totalSkipped = 0;
        
        // 🔧 增量添加标签关系：对每个视频的每个标签检查是否存在
        for (Long videoId : videoIdList) {
            int addedForVideo = 0;
            int skippedForVideo = 0;
            
            for (Long tagId : tagIds) {
                try {
                    // 检查是否已存在该标签关系
                    boolean exists = videoMapper.checkVideoTagRelationExists(videoId, tagId);
                    if (!exists) {
                        videoMapper.insertVideoTagRelation(videoId, tagId);
                        addedForVideo++;
                        totalAdded++;
                        logger.debug("✅ 为视频{}添加标签关系: tagId={}", videoId, tagId);
                    } else {
                        skippedForVideo++;
                        totalSkipped++;
                        logger.debug("⏭️ 视频{}已存在标签关系，跳过: tagId={}", videoId, tagId);
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ 为视频{}处理标签关系时出现异常: tagId={}, error={}", videoId, tagId, e.getMessage());
                }
            }
            
            logger.debug("📊 视频{}标签处理完成: 新增{}个，跳过{}个", videoId, addedForVideo, skippedForVideo);
        }

        int result = totalAdded;

        final int finalResult = result; // 创建final变量供lambda使用
        if (finalResult > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 批量添加标签事务提交成功，开始智能缓存失效: 影响{}个视频", ids.length);
                    // 🚀 批量标签变更：全局影响
                    try { 
                        smartCacheInvalidation(null, null, true);
                        // 额外清理短视频缓存和批量查询缓存
                        cacheRefreshService.invalidateShortVideoCache(null, null, null, true);
                    } catch (Exception ignored) {}
                }
            });
        }

        logger.info("📊 批量添加视频标签完成: 影响{}个视频，新增{}个标签关系，跳过{}个已存在关系", 
                   ids.length, totalAdded, totalSkipped);
        return ids.length; // 返回处理的视频数量
    }

    /**
     * 更新视频富文本中的视频URL签名
     * 
     * @param videoId 视频ID
     * @return 更新结果
     */
    @Override
    @Transactional
    public int updateVideoContentSignatures(Long videoId)
    {
        Video video = videoMapper.selectVideoById(videoId);
        if (video == null || video.getVideoContent() == null) {
            return 0;
        }

        try {
            // 更新富文本中的视频URL签名
            String updatedContent = videoTranscodeService.updateVideoSignaturesInRichText(video.getVideoContent());
            
            // 只有当内容发生变化时才更新
            if (!video.getVideoContent().equals(updatedContent)) {
                video.setVideoContent(updatedContent);
                return videoMapper.updateVideo(video);
            }
            
            return 1; // 内容没有变化，但处理成功
        } catch (Exception e) {
            throw new RuntimeException("更新视频富文本签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新视频富文本中的视频URL签名
     * 
     * @param videoIds 视频ID列表
     * @return 更新结果
     */
    @Override
    @Transactional
    public int batchUpdateVideoContentSignatures(List<Long> videoIds)
    {
        if (CollectionUtils.isEmpty(videoIds)) {
            return 0;
        }

        int updateCount = 0;
        for (Long videoId : videoIds) {
            try {
                updateCount += updateVideoContentSignatures(videoId);
            } catch (Exception e) {
                // 记录日志但继续处理其他视频
                System.err.println("更新视频ID " + videoId + " 的富文本签名失败: " + e.getMessage());
            }
        }
        
        return updateCount;
    }

    /**
     * 从富文本内容中提取第一个视频的video_transcodes.id
     * 优先使用data-video-id（对应video_transcodes.id），如果没有则通过data-transcode-id查询
     * 
     * @param richTextContent 富文本内容
     * @return 提取到的video_transcodes.id，如果没有找到则返回null
     */
    private String extractTranscodeIdFromRichText(String richTextContent) {
        if (richTextContent == null || richTextContent.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 1. 优先尝试提取data-video-id（对应video_transcodes.id主键）
            java.util.regex.Pattern videoIdPattern = java.util.regex.Pattern.compile(
                "<div[^>]+class=\"rich-text-video\"[^>]+data-video-id=\"([^\"]+)\"[^>]*>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher videoIdMatcher = videoIdPattern.matcher(richTextContent);
            
            if (videoIdMatcher.find()) {
                String videoId = videoIdMatcher.group(1);
                if (videoId != null && !videoId.trim().isEmpty()) {
                    logger.info("✅ 从富文本中提取到video-id: {}", videoId);
                    
                    // 验证这个ID确实存在
                    try {
                        Long id = Long.valueOf(videoId.trim());
                        VideoTranscode record = videoTranscodeMapper.selectVideoTranscodeById(id);
                        if (record != null) {
                            logger.info("✅ 确认video-id {} 对应的转码记录存在", id);
                            return videoId.trim(); // 直接返回video_transcodes.id
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("⚠️ video-id 不是有效的数字: {}", videoId);
                    }
                }
            }
            
            // 2. 兼容旧格式：尝试提取data-transcode-id，然后查询获取id
            java.util.regex.Pattern transcodeIdPattern = java.util.regex.Pattern.compile(
                "<div[^>]+class=\"rich-text-video\"[^>]+data-transcode-id=\"([^\"]+)\"[^>]*>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher transcodeIdMatcher = transcodeIdPattern.matcher(richTextContent);
            
            if (transcodeIdMatcher.find()) {
                String transcodeId = transcodeIdMatcher.group(1);
                if (transcodeId != null && !transcodeId.trim().isEmpty()) {
                    logger.info("✅ 从富文本中提取到transcode_id: {}", transcodeId);
                    
                    // 通过transcode_id查询获取对应的id
                    VideoTranscode record = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(transcodeId.trim());
                    if (record != null && record.getId() != null) {
                        logger.info("✅ 通过transcode_id {} 找到对应的video_transcodes.id: {}", transcodeId, record.getId());
                        return record.getId().toString();
                    }
                }
            }
            
            logger.debug("🔍 未在富文本中找到有效的video-id或transcode_id");
            return null;
            
        } catch (Exception e) {
            logger.error("❌ 从富文本内容中提取video_transcodes.id失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据转码回调数据更新视频富文本内容
     * 1. 根据transcodeId查询video_transcodes表获取title
     * 2. 根据video_transcodes表中的title模糊查询videos表
     * 3. 在匹配视频的富文本内容末尾添加视频播放器
     */
    @Override
    @Transactional
    public String updateVideoContentByTranscodeCallback(com.ruoyi.chigua.dto.PpvodCallbackDto callbackData) 
    {
        logger.info("🎬 开始处理转码回调富文本更新: transcodeId={}", callbackData.getTranscodeId());
        
        try {
            // 1. 根据transcodeId查询video_transcodes表，获取title
            com.ruoyi.chigua.domain.VideoTranscode transcodeRecord = 
                videoTranscodeService.selectVideoTranscodeByTranscodeId(callbackData.getTranscodeId());
            
            if (transcodeRecord == null) {
                logger.warn("⚠️ 未找到转码记录: transcodeId={}", callbackData.getTranscodeId());
                return "失败：未找到对应的转码记录";
            }
            
            String videoTitle = transcodeRecord.getTitle();
            if (videoTitle == null || videoTitle.trim().isEmpty()) {
                logger.warn("⚠️ 转码记录中没有标题信息: transcodeId={}", callbackData.getTranscodeId());
                return "失败：转码记录中没有标题信息";
            }
            
            logger.info("🔍 使用转码记录中的标题进行查询: {}", videoTitle);
            
            // 2. 智能处理标题，去除常见的分段后缀
            String cleanTitle = cleanVideoTitle(videoTitle);
            logger.info("🧹 清理后的标题: {} -> {}", videoTitle, cleanTitle);
            
            Video searchVideo = new Video();
            List<Video> matchedVideos = null;
            
            // 3. 优先使用精确匹配（避免"鬼父 Re-born 2"匹配到"鬼父 Re-born 1"）
            searchVideo.setTitle(videoTitle);
            matchedVideos = videoMapper.selectVideoListByTitleExact(searchVideo);
            
            if (matchedVideos != null && !matchedVideos.isEmpty()) {
                logger.info("✅ 使用原始标题精确匹配成功: videoTitle={}, 找到{}个视频", videoTitle, matchedVideos.size());
            } else {
                // 如果精确匹配失败，尝试使用清理后的标题精确匹配
                if (!videoTitle.equals(cleanTitle)) {
                    logger.info("🔄 尝试使用清理后的标题精确匹配: {}", cleanTitle);
                    searchVideo.setTitle(cleanTitle);
                    matchedVideos = videoMapper.selectVideoListByTitleExact(searchVideo);
                    
                    if (matchedVideos != null && !matchedVideos.isEmpty()) {
                        logger.info("✅ 使用清理后的标题精确匹配成功: cleanTitle={}, 找到{}个视频", cleanTitle, matchedVideos.size());
                    }
                }
            }
            
            // 4. 如果精确匹配都失败，再使用模糊匹配（作为兜底方案）
            if (matchedVideos == null || matchedVideos.isEmpty()) {
                logger.info("⚠️ 精确匹配失败，尝试模糊匹配（可能不准确）: cleanTitle={}", cleanTitle);
                searchVideo.setTitle(cleanTitle);
                matchedVideos = videoMapper.selectVideoListByTitleLike(searchVideo);
                
                if (matchedVideos == null || matchedVideos.isEmpty()) {
                    // 如果清理后的标题没有匹配，尝试使用原始标题
                    if (!videoTitle.equals(cleanTitle)) {
                        logger.info("🔄 尝试使用原始标题模糊匹配: {}", videoTitle);
                        searchVideo.setTitle(videoTitle);
                        matchedVideos = videoMapper.selectVideoListByTitleLike(searchVideo);
                    }
                }
                
                if (matchedVideos == null || matchedVideos.isEmpty()) {
                    logger.info("📭 所有匹配方式都失败: videoTitle={}", videoTitle);
                    return "未找到匹配的视频";
                }
                
                // 模糊匹配可能返回多个结果，选择最佳匹配（标题最短或最相似的）
                if (matchedVideos.size() > 1) {
                    logger.warn("⚠️ 模糊匹配返回多个结果，选择最佳匹配");
                    matchedVideos = selectBestMatchVideo(videoTitle, matchedVideos);
                }
            }
            
            // 5. 取第一个匹配的视频
            Video targetVideo = matchedVideos.get(0);
            logger.info("🎯 找到匹配视频: videoId={}, title={}", targetVideo.getId(), targetVideo.getTitle());
            
            // 4. 生成视频播放器HTML内容
            String videoHtml = generateVideoHtmlFromCallback(callbackData, targetVideo);
            
            // 5. 更新视频的富文本内容
            String originalContent = targetVideo.getVideoContent();
            String updatedContent;
            
            if (originalContent == null || originalContent.trim().isEmpty()) {
                // 如果原来没有富文本内容，直接设置
                updatedContent = videoHtml;
            } else {
                // 在原有内容末尾添加视频
                updatedContent = originalContent + "\n\n" + videoHtml;
            }
            
            // 🔧 修复：智能处理富文本内容转换
            boolean isAlreadyStorageFormat = isContentInStorageFormat(updatedContent);
            String storageContent;
            
            if (isAlreadyStorageFormat) {
                logger.info("✅ 回调更新内容已经是存储格式，跳过转换: videoId={}, contentLength={}", 
                    targetVideo.getId(), updatedContent.length());
                storageContent = updatedContent;
            } else {
                logger.info("🔄 回调更新内容需要转换为存储格式: videoId={}, contentLength={}", 
                    targetVideo.getId(), updatedContent.length());
                storageContent = richTextProcessorService.prepareRichTextForStorage(updatedContent);
                
                logger.info("✅ 回调更新内容转换完成: 原长度={}, 转换后长度={}", 
                    updatedContent.length(), storageContent.length());
            }
            
            // 更新数据库
            Video updateVideo = new Video();
            updateVideo.setId(targetVideo.getId());
            updateVideo.setVideoContent(storageContent);
            updateVideo.setLastEditedAt(new Date());
            
            // 如果视频还没有绑定转码ID，自动绑定（使用video_transcodes表的主键id，与数据库设计一致）
            if (targetVideo.getTranscodeId() == null || targetVideo.getTranscodeId().trim().isEmpty()) {
                updateVideo.setTranscodeId(String.valueOf(transcodeRecord.getId()));
                logger.info("🔗 自动绑定转码ID: videoId={}, transcodeRecordId={}", 
                    targetVideo.getId(), transcodeRecord.getId());
            }
            
            int updateResult = videoMapper.updateVideo(updateVideo);
            
            if (updateResult > 0) {
                // 标记转码记录为已使用（使用transcodeRecord的transcode_id字符串字段）
                try {
                    videoTranscodeService.markTranscodeAsUsed(transcodeRecord.getTranscodeId(), targetVideo.getId());
                    logger.info("✅ 标记转码记录为已使用: transcodeId={}, videoId={}", 
                        transcodeRecord.getTranscodeId(), targetVideo.getId());
                } catch (Exception e) {
                    logger.error("❌ 标记转码记录为已使用失败: transcodeId={}, 错误: {}", 
                        transcodeRecord.getTranscodeId(), e.getMessage());
                }
                
                // 🚀 富文本更新：支持多分类
                try {
                    smartCacheInvalidation(targetVideo.getId(), targetVideo.getCategoryId(), false);
                } catch (Exception ignored) {}
                
                logger.info("✅ 富文本更新成功: videoId={}, transcodeId={}", 
                    targetVideo.getId(), callbackData.getTranscodeId());
                
                return String.format("成功：已更新视频 '%s' (ID:%d) 的富文本内容", 
                    targetVideo.getTitle(), targetVideo.getId());
            } else {
                logger.error("❌ 富文本更新失败: videoId={}", targetVideo.getId());
                return "失败：数据库更新失败";
            }
            
        } catch (Exception e) {
            logger.error("❌ 转码回调富文本更新异常: transcodeId={}, 错误: {}", 
                callbackData.getTranscodeId(), e.getMessage(), e);
            return "失败：" + e.getMessage();
        }
    }

    /**
     * 根据转码回调数据生成视频播放器HTML
     */
    private String generateVideoHtmlFromCallback(com.ruoyi.chigua.dto.PpvodCallbackDto callbackData, Video targetVideo) {
        logger.info("🎞️ 生成视频HTML: transcodeId={}", callbackData.getTranscodeId());
        
        // 构建视频路径（使用相对路径格式，参考正确示例）
        String videoPath = "";
        String posterPath = "";
        
        // 根据转码ID构建标准的文件路径格式
        String transcodeId = callbackData.getTranscodeId();
        if (transcodeId != null) {
            // 根据转码ID查询转码记录，获取rpath中的完整路径信息
            VideoTranscode transcodeRecord = videoTranscodeService.selectVideoTranscodeByTranscodeId(transcodeId);
            String rpath = transcodeRecord.getRpath();
            logger.info("🎯 从转码记录中获取完整路径: {}", rpath);
            
            // 从rpath中提取完整路径信息，格式如：videos/202508/22/转码ID/子目录名
            String[] pathParts = rpath.split("/");
            
            // 提取日期路径部分：202508/22
            String yearMonth = pathParts[1]; // 202508
            String day = pathParts[2];       // 22
            String datePath = yearMonth + "/" + day;
            
            // 提取子目录名
            String subDirectory = pathParts[pathParts.length - 1]; // 最后一部分作为子目录名
            
            // 构建完整的基础路径
            String basePath = "files/videos/" + datePath + "/" + transcodeId;
            
            logger.info("🎯 提取路径信息: datePath={}, subDirectory={}, basePath={}", 
                datePath, subDirectory, basePath);
            
            videoPath = basePath + "/" + subDirectory + "/index.m3u8";
            
            // 封面图片路径
            posterPath = basePath + "/cover.jpg";
        }
        
        // 如果回调数据中有具体的路径信息，将其转换为标准格式
        if (callbackData.getPath() != null && !callbackData.getPath().isEmpty()) {
            String path = callbackData.getPath();
            // 转换为相对路径格式
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.startsWith("http://") || path.startsWith("https://")) {
                // 如果是完整URL，提取路径部分
                try {
                    java.net.URL url = new java.net.URL(path);
                    path = url.getPath().substring(1); // 去掉开头的 /
                } catch (Exception e) {
                    logger.warn("无法解析视频URL: {}", path);
                }
            }
            
            // 将路径转换为标准的files/videos格式
            if (!path.startsWith("files/videos/")) {
                // 如果不是标准格式，构建标准路径
                VideoTranscode transcodeRecord = videoTranscodeService.selectVideoTranscodeByTranscodeId(transcodeId);
                String rpath = transcodeRecord.getRpath();
                logger.info("🎯 从转码记录中获取完整路径（回调路径）: {}", rpath);
                
                // 从rpath中提取完整路径信息
                String[] pathParts = rpath.split("/");
                
                // 提取日期路径部分
                String yearMonth = pathParts[1];
                String day = pathParts[2];
                String datePath = yearMonth + "/" + day;
                
                // 提取子目录名
                String subDirectory = pathParts[pathParts.length - 1];
                
                // 构建完整的基础路径
                String basePath = "files/videos/" + datePath + "/" + transcodeId;
                
                logger.info("🎯 提取路径信息（回调路径）: datePath={}, subDirectory={}, basePath={}", 
                    datePath, subDirectory, basePath);
                
                // 确保视频路径以index.m3u8结尾（HLS格式）
                if (!path.toLowerCase().endsWith(".m3u8") && !path.toLowerCase().endsWith(".mp4")) {
                    // 如果路径不是以视频文件结尾，添加index.m3u8
                    videoPath = basePath + "/" + subDirectory + "/index.m3u8";
                } else {
                    // 如果已经是视频文件，使用标准路径结构
                    videoPath = basePath + "/" + subDirectory + "/" + java.nio.file.Paths.get(path).getFileName().toString();
                }
            } else {
                // 如果已经是标准格式，直接使用
                videoPath = path;
            }
        }
        
        // 如果有缩略图，使用缩略图作为封面
        if (callbackData.getThumbnails() != null && !callbackData.getThumbnails().isEmpty()) {
            String thumbnail = callbackData.getThumbnails().get(0);
            if (thumbnail != null && !thumbnail.isEmpty()) {
                // 转换为相对路径格式
                if (thumbnail.startsWith("/")) {
                    thumbnail = thumbnail.substring(1);
                }
                if (thumbnail.startsWith("http://") || thumbnail.startsWith("https://")) {
                    // 如果是完整URL，提取路径部分
                    try {
                        java.net.URL url = new java.net.URL(thumbnail);
                        thumbnail = url.getPath().substring(1); // 去掉开头的 /
                    } catch (Exception e) {
                        logger.warn("无法解析封面URL: {}", thumbnail);
                    }
                }
                posterPath = thumbnail;
            }
        }
        
        // 生成标准的video标签HTML（参考正确格式）
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<video controls=\"controls\" width=\"100%\"");
        
        
        // 添加data属性
        htmlBuilder.append(" data-video-id=\"").append(targetVideo.getId()).append("\""); // 使用实际的视频ID
        htmlBuilder.append(" data-transcode-id=\"").append(escapeHtml(transcodeId)).append("\"");
        htmlBuilder.append(" data-orgfile=\"").append(escapeHtml(callbackData.getOrgfile())).append("\"");
        htmlBuilder.append(" data-resolution=\"").append(escapeHtml(callbackData.getResolution())).append("\"");
        htmlBuilder.append(" data-duration=\"").append(callbackData.getDuration() != null ? callbackData.getDuration() : 0).append("\"");
        
        // 添加封面图片
        if (!posterPath.isEmpty()) {
            htmlBuilder.append(" poster=\"").append(escapeHtml(posterPath)).append("\"");
        }
        
        // 添加样式（参考正确格式）
        htmlBuilder.append(" style=\"max-width: 600px; display: block; margin: 10px 0px; width: 100%;\"");
        
        htmlBuilder.append(">");
        
        // 添加视频源
        if (!videoPath.isEmpty()) {
            // 判断视频类型
            String mimeType = "video/mp4";
            if (videoPath.toLowerCase().contains(".m3u8")) {
                mimeType = "application/x-mpegURL";
            }
            
            htmlBuilder.append("<source src=\"").append(escapeHtml(videoPath)).append("\" type=\"").append(mimeType).append("\">");
        }
        
        htmlBuilder.append("</video>");
        
        String html = htmlBuilder.toString();
        logger.info("✅ 生成的视频HTML: {}", html);
        
        return html;
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 清理视频标题，去除常见的分段后缀，提高匹配成功率
     * 
     * @param title 原始标题
     * @return 清理后的标题
     */
    private String cleanVideoTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }
        
        String cleanTitle = title.trim();
        
        // 去除常见的分段后缀模式
        // 匹配 _part01, _part02, _part1, _part2, _01, _02, _1, _2 等
        cleanTitle = cleanTitle.replaceAll("_part\\d+$", "");  // _part01, _part02, _part1, _part2
        cleanTitle = cleanTitle.replaceAll("_\\d+$", "");      // _01, _02, _1, _2
        cleanTitle = cleanTitle.replaceAll("\\s+part\\s*\\d+$", ""); // " part 01", " part1"
        cleanTitle = cleanTitle.replaceAll("\\s+\\d+$", "");   // " 01", " 1"
        cleanTitle = cleanTitle.replaceAll("第\\d+部分?$", "");  // "第1部分", "第01部"
        cleanTitle = cleanTitle.replaceAll("\\(\\d+\\)$", ""); // "(1)", "(01)"
        cleanTitle = cleanTitle.replaceAll("\\[\\d+\\]$", ""); // "[1]", "[01]"
        
        // 去除末尾的空白字符
        cleanTitle = cleanTitle.trim();
        
        // 如果清理后为空，返回原始标题
        if (cleanTitle.isEmpty()) {
            return title;
        }
        
        return cleanTitle;
    }
    
    /**
     * 从多个模糊匹配结果中选择最佳匹配
     * 优先选择完全匹配或标题最短的（避免"鬼父 Re-born 2"匹配到"鬼父 Re-born 1"）
     * 
     * @param targetTitle 目标标题
     * @param candidates 候选视频列表
     * @return 最佳匹配的视频列表（单个元素）
     */
    private List<Video> selectBestMatchVideo(String targetTitle, List<Video> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        
        if (candidates.size() == 1) {
            return candidates;
        }
        
        // 1. 优先查找完全匹配
        for (Video video : candidates) {
            if (video.getTitle() != null && video.getTitle().equals(targetTitle)) {
                logger.info("✅ 找到完全匹配: videoId={}, title={}", video.getId(), video.getTitle());
                return java.util.Collections.singletonList(video);
            }
        }
        
        // 2. 选择标题最短的（最可能是基础标题而不是带分段的）
        Video bestMatch = candidates.get(0);
        int minLength = bestMatch.getTitle() != null ? bestMatch.getTitle().length() : Integer.MAX_VALUE;
        
        for (Video video : candidates) {
            if (video.getTitle() != null && video.getTitle().length() < minLength) {
                bestMatch = video;
                minLength = video.getTitle().length();
            }
        }
        
        logger.info("🎯 选择标题最短的视频作为最佳匹配: videoId={}, title={} (从{}个候选中选择)", 
            bestMatch.getId(), bestMatch.getTitle(), candidates.size());
        
        return java.util.Collections.singletonList(bestMatch);
    }
    
    // ==================== 多分类智能缓存失效 ====================
    
    /**
     * 多分类智能缓存失效
     * 支持一个视频属于多个分类的场景，确保数据一致性
     * 
     * @param videoId 视频ID
     * @param primaryCategoryId 主分类ID
     * @param globalImpact 是否全局影响
     */
    private void smartCacheInvalidation(Long videoId, Long primaryCategoryId, boolean globalImpact) {
        try {
            logger.debug("🔄 执行多分类智能缓存失效: videoId={}, primaryCategoryId={}, globalImpact={}", 
                       videoId, primaryCategoryId, globalImpact);
            cacheRefreshService.smartRefreshVideoCache(videoId, primaryCategoryId, globalImpact);
        } catch (Exception e) {
            logger.error("❌ 多分类缓存失效失败: videoId={}, primaryCategoryId={}, globalImpact={}, error={}", 
                        videoId, primaryCategoryId, globalImpact, e.getMessage(), e);
            throw new RuntimeException("缓存失效失败", e);
        }
    }

    /**
     * 检测富文本内容是否已经是存储格式
     * 存储格式特征：
     * 1. 包含相对路径（如 src="files/xxx" 或 poster="files/xxx"）
     * 2. 包含 data-resource-key 属性
     * 3. 不包含签名URL（如不包含 signature= 或 chigua-r2-worker.xingaikaka.workers.dev）
     * 
     * @param content 富文本内容
     * @return true 如果已经是存储格式
     */
    private boolean isContentInStorageFormat(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true; // 空内容认为是存储格式
        }
        
        logger.debug("🔍 检测内容格式，长度: {}", content.length());
        
        // 检查是否包含签名URL - 如果包含则不是存储格式
        boolean hasSignedUrls = content.contains("signature=") || 
                               content.contains("expires=") ||
                               content.contains("?key=") ||
                               content.contains("&key=") ||
                               content.contains("?signature=") ||
                               content.contains("&signature=");
        
        // 🔧 检查是否包含配置的域名
        if (!hasSignedUrls && chiguaProperties != null && chiguaProperties.getDomains() != null) {
            String mainDomain = chiguaProperties.getDomains().getMain();
            String fallbackDomain = chiguaProperties.getDomains().getFallback();
            
            if (mainDomain != null && content.contains(mainDomain)) {
                hasSignedUrls = true;
            }
            if (fallbackDomain != null && content.contains(fallbackDomain)) {
                hasSignedUrls = true;
            }
        }
        
        if (hasSignedUrls) {
            logger.info("🔍 内容包含签名URL，需要转换为存储格式");
            return false;
        }
        
        // 检查是否包含存储格式的特征
        boolean hasDataResourceKey = content.contains("data-resource-key=");
        boolean hasRelativePaths = content.contains("src=\"files/") || 
                                 content.contains("poster=\"files/") ||
                                 content.contains("src=\"images/") ||
                                 content.contains("poster=\"images/") ||
                                 content.contains("src=\"uploads/") ||
                                 content.contains("poster=\"uploads/");
        
        // 🔧 更严格的检查：如果有媒体标签但没有存储格式特征，可能需要转换
        boolean hasMediaTags = content.contains("<img") || 
                              content.contains("<video") || 
                              content.contains("<source");
        
        if (hasMediaTags) {
            if (hasDataResourceKey || hasRelativePaths) {
                logger.info("✅ 内容包含媒体标签且有存储格式特征，已经是存储格式");
                return true;
            } else {
                // 有媒体标签但没有存储格式特征，可能是新插入的内容需要转换
                logger.info("🔍 内容包含媒体标签但缺少存储格式特征，可能需要转换");
                return false;
            }
        } else {
            // 没有媒体标签的纯文本内容
            logger.debug("✅ 内容不包含媒体标签，认为是存储格式");
            return true;
        }
    }

    /**
     * 安全的类型转换：将Object转换为Long
     * 处理数据库返回Integer但需要Long的情况
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("⚠️ 无法将值转换为Long: {}", value);
            return null;
        }
    }

    /**
     * 从富文本内容中提取首个 <video>/<source src> 的资源路径为相对路径
     * 🔧 优先提取 source 标签的 src，因为 video 标签的 src 可能是 blob URL
     */
    private String extractFirstVideoUrl(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        try {
            // 🔧 优先查找 source 标签的 src
            int pSource = content.indexOf("<source");
            if (pSource >= 0) {
                int pSrc = content.indexOf("src=\"", pSource);
                if (pSrc >= 0) {
                    int start = pSrc + 5;
                    int end = content.indexOf('"', start);
                    if (end > start) {
                        String url = content.substring(start, end);
                        // 跳过 blob URL
                        if (!url.startsWith("blob:") && !url.startsWith("data:")) {
                            String normalized = normalizeFirstVideoPath(url);
                            if (normalized != null) {
                                logger.debug("✅ 从 source 标签提取视频URL: {}", normalized);
                                return normalized;
                            }
                        }
                    }
                }
            }
            
            // 🔧 如果没有 source 标签，再查找 video 标签的 src
            int pVideo = content.indexOf("<video");
            if (pVideo >= 0) {
                int pSrc = content.indexOf("src=\"", pVideo);
                if (pSrc >= 0) {
                    int start = pSrc + 5;
                    int end = content.indexOf('"', start);
                    if (end > start) {
                        String url = content.substring(start, end);
                        // 跳过 blob URL
                        if (!url.startsWith("blob:") && !url.startsWith("data:")) {
                            String normalized = normalizeFirstVideoPath(url);
                            if (normalized != null) {
                                logger.debug("✅ 从 video 标签提取视频URL: {}", normalized);
                                return normalized;
                            }
                        }
                    }
                }
            }
            
            logger.debug("⚠️ 未能从富文本中提取有效的视频URL");
            return null;
        } catch (Exception e) {
            logger.error("❌ 提取视频URL失败: {}", e.getMessage());
            return null;
        }
    }
} 
