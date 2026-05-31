package com.ruoyi.chigua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Video;
import java.util.*;

/**
 * 缓存刷新服务 - 支持多分类智能缓存失效
 * 
 * 主要功能：
 * - 支持一个视频属于多个分类的缓存失效场景
 * - 智能识别视频的所有相关分类并失效对应缓存
 * - 确保数据一致性，避免缓存不同步问题
 * - 提供降级策略，保证系统稳定性
 */
@Service
public class CacheRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(CacheRefreshService.class);

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 清理指定分类的视频列表缓存（Redis精确清理策略）
     * 
     * @param categoryId 分类ID，null表示全部视频
     */
    public void clearCategoryVideoCache(Long categoryId) {
        try {
            String categoryKey = categoryId != null ? categoryId.toString() : "all";
            
            // 🎯 使用RedisTemplate精确清理特定分类的缓存键（精确匹配categoryId位置）
            // videoList缓存键格式: list_3_1_all_all, basic_3_1_all_all, paged_3_1_1_20
            clearRedisCacheByPattern("videoList", "list_" + categoryKey + "_*");
            clearRedisCacheByPattern("videoList", "basic_" + categoryKey + "_*");  
            clearRedisCacheByPattern("videoList", "paged_" + categoryKey + "_*");
            clearRedisCacheByPattern("videoList", "basic_paged_" + categoryKey + "_*");
            
            // videoListOptimized缓存键格式: optimized_3_1_0_20, basic_data_3_0_20, count_only_3_all
            clearRedisCacheByPattern("videoListOptimized", "optimized_" + categoryKey + "_*");
            clearRedisCacheByPattern("videoListOptimized", "basic_data_" + categoryKey + "_*");
            clearRedisCacheByPattern("videoListOptimized", "count_only_" + categoryKey + "_*");
            
            // categoryVideoSort缓存键格式: mixed_sort_3_0_20, category_mixed_3_0_20
            clearRedisCacheByPattern("categoryVideoSort", "mixed_sort_" + categoryKey + "_*");
            clearRedisCacheByPattern("categoryVideoSort", "category_mixed_" + categoryKey + "_*");
            
            logger.info("📂 已精确清理分类{}的视频缓存", categoryKey);
        } catch (Exception e) {
            logger.warn("⚠️ 精确清理失败，降级到全量清理: {}", e.getMessage());
            // 降级策略：清理所有相关缓存
            clearCacheAll("videoList");
            clearCacheAll("videoListOptimized");
            clearCacheAll("categoryVideoSort");
            logger.info("✅ 降级成功：已清理所有视频缓存");
        }
    }
    
    /**
     * 智能视频缓存失效（固定键策略）
     * 支持一个视频属于多个分类的场景，直接清理相关缓存
     * 
     * @param videoId 视频ID
     * @param primaryCategoryId 主分类ID
     * @param globalImpact 是否全局影响
     */
    public void smartRefreshVideoCache(Long videoId, Long primaryCategoryId, boolean globalImpact) {
        try {
            // 🔍 步骤1：获取视频的所有相关分类
            List<Long> allRelatedCategoryIds = getAllRelatedCategoryIds(videoId, primaryCategoryId);
            logger.info("🎯 视频{}影响的分类数量: {}, 分类列表: {}", videoId, allRelatedCategoryIds.size(), allRelatedCategoryIds);
            
            // 🔄 步骤2：清理每个相关分类的缓存
            for (Long categoryId : allRelatedCategoryIds) {
                clearCategoryVideoCache(categoryId);
                logger.debug("✅ 已清理分类{}的缓存", categoryId == null ? "null(全部)" : categoryId);
            }
            
            logger.info("✅ 固定键缓存清理完成，共处理{}个分类", allRelatedCategoryIds.size());
            
            // 🔧 步骤3：处理其他类型的缓存
            // 注意：webVideoDetail 已按"方案 B"改造为不再缓存（详情接口不加 @Cacheable），
            // 这里保留 clearCache 调用是无害 no-op，仅为兼容历史 cache 残留
            if (videoId != null) {
                clearCache("webVideoDetail", "detail_" + videoId);
                clearCache("videoRelations", "categories_" + videoId);

                // 签名URL缓存（按 cover_{videoId}_*_{REGION} 模式批量删，CN/OS 都覆盖）
                clearRedisCacheByPattern("signedUrlCache", "cover_" + videoId + "_*");
                clearCache("videoRelations", "tags_" + videoId);
            }
            
            // 🌐 步骤4：处理全局缓存（保持原有逻辑）
            // searchResult 已改为TTL自动过期，不需要主动清理
            clearCacheAll("tagVideoList");
            clearCacheAll("archivesList");
            clearCacheAll("adjacentVideos");
            
            // 🗂️ 步骤5：刷新合集独立缓存（如果视频属于合集分类）
            if (videoId != null) {
                try {
                    refreshCollectionCacheIfNeeded(videoId);
                } catch (Exception e) {
                    logger.warn("⚠️ 刷新合集缓存失败: videoId={}, error={}", videoId, e.getMessage());
                }
            }
            
            logger.info("✅ 多分类视频缓存失效完成: videoId={}, primaryCategoryId={}, globalImpact={}", 
                       videoId, primaryCategoryId, globalImpact);
            
        } catch (Exception e) {
            logger.error("❌ 固定键缓存清理失败: videoId={}, primaryCategoryId={}, error={}", 
                        videoId, primaryCategoryId, e.getMessage(), e);
            
            // 降级策略：全量清理相关缓存
            logger.warn("🔄 降级到全量缓存清理");
            try {
                // 清理所有视频相关缓存
                clearCacheAll("videoList");
                clearCacheAll("videoListOptimized");
                clearCacheAll("categoryVideoSort");
                
                if (videoId != null) {
                    clearCache("webVideoDetail", "detail_" + videoId);
                    clearCache("videoRelations", "categories_" + videoId);
                    clearCache("videoRelations", "tags_" + videoId);
                }
                
                // 清理其他相关缓存
                clearCacheAll("tagVideoList");
                clearCacheAll("archivesList");
                clearCacheAll("adjacentVideos");
                logger.info("✅ 降级处理完成");
            } catch (Exception fallbackEx) {
                logger.error("❌ 降级处理也失败: {}", fallbackEx.getMessage(), fallbackEx);
                throw new RuntimeException("缓存失效完全失败", fallbackEx);
            }
        }
    }

    /**
     * 合集编辑后刷新相关缓存（独立缓存策略）
     * 合集使用独立的缓存策略，不依赖版本控制
     */
    public void refreshCollectionCache(Long collectionId, Long categoryId) {
        try {
            logger.info("🔄 开始刷新合集缓存: collectionId={}, categoryId={}", collectionId, categoryId);
            
            // 🗂️ 清理合集分页列表缓存（所有分类的合集列表）
            clearCacheAll("webCollectionList");
            logger.debug("✅ 已清理Web合集分页列表缓存");
            
            // 📋 清理合集中视频列表缓存
            if (collectionId != null) {
                clearCache("collectionDetail", "videos_" + collectionId);
                logger.debug("✅ 已清理合集{}的视频列表缓存", collectionId);
            } else {
                // 如果没有指定合集ID，清理所有合集视频列表缓存
                clearCacheAll("collectionDetail");
                logger.debug("✅ 已清理所有合集视频列表缓存");
            }
            
            // 🔍 搜索缓存已改为TTL自动过期，不需要主动清理
            // cacheVersionService.incrementVersion("searchResult");
            // logger.debug("✅ 已递增搜索结果缓存版本");
            
            logger.info("✅ 合集独立缓存刷新完成: collectionId={}, categoryId={}", collectionId, categoryId);
        } catch (Exception e) {
            logger.error("❌ 合集缓存刷新失败: collectionId={}, categoryId={}, 错误: {}", collectionId, categoryId, e.getMessage(), e);
        }
    }

    /**
     * 分类编辑后刷新相关缓存（全局影响）
     */
    public void refreshCategoryCache() {
        try {
            // 清理分类缓存
            clearCacheAll("categoryList");
            clearCacheAll("categoryDetail");
            
            // 🚀 分类变更是全局影响，清理所有视频缓存
            clearCacheAll("videoList");
            clearCacheAll("videoListOptimized");
            clearCacheAll("categoryVideoSort");
            
            // 清理合集列表缓存
            clearCacheAll("collectionList");
            clearCacheAll("webCollectionList");
            
            // 清理广告缓存（分类相关广告）
            clearCacheAll("categoryAds");
            
            // 清理往期内容缓存
            clearCacheAll("archivesList");
            
            // 🔧 新增：清理相邻视频缓存（分类变更会影响基于分类的相邻视频关系）
            clearCacheAll("adjacentVideos");
            
            // 🔥 清理热门推荐缓存（使用固定键策略）
            clearCacheAll("hotRecommendedVideosBasic");
            
            logger.info("✅ 分类缓存清理完成（全局影响）");
        } catch (Exception e) {
            logger.error("❌ 分类缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 页面配置编辑后刷新相关缓存
     */
    public void refreshPageConfigCache() {
        try {
            // 直接清理页面配置缓存（使用固定键策略）
            clearCache("pageConfig", "all_active_configs");
            clearCache("pageConfigVersion", "current_version");
            // 已签名富文本缓存
            clearCache("pageConfigSigned", "all_active_configs_signed");
            // 新增：清理按类型查询的页面配置缓存，确保例如 category_more 立即生效
            clearCacheAll("pageConfigList");
            
            logger.info("✅ 页面配置缓存直接清理完成");
        } catch (Exception e) {
            logger.error("❌ 页面配置缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 搜索缓存刷新
     */
    public void refreshSearchCache() {
        try {
            // 搜索缓存已改为TTL自动过期策略，直接清理所有搜索缓存
            clearCacheAll("searchResult");
            logger.info("✅ 搜索缓存直接清理完成");
        } catch (Exception e) {
            logger.error("❌ 搜索缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 评论编辑后刷新相关缓存
     */
    public void refreshCommentCache() {
        try {
            // 直接清理评论相关缓存
            clearCacheAll("commentList");
            clearCacheAll("videoComments");
            
            logger.info("✅ 评论缓存直接清理完成");
        } catch (Exception e) {
            logger.error("❌ 评论缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 标签编辑后刷新相关缓存
     */
    public void refreshTagCache() {
        try {
            // 清理标签列表缓存
            clearCacheAll("tagsList");
            
            // 清理视频关联关系缓存（标签变更会影响视频-标签关系）
            clearCacheAll("videoRelations");
            
            // 清理标签视频列表缓存（标签变更会影响标签页面的视频列表）
            clearCacheAll("tagVideoList");
            
            // 清理往期内容缓存（标签变更会影响归档页面的标签列表）
            clearCacheAll("archivesList");
            
            logger.info("✅ 标签缓存清理完成");
        } catch (Exception e) {
            logger.error("❌ 标签缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 广告编辑后刷新相关缓存
     */
    public void refreshAdvertisementCache() {
        try {
            // 清理广告缓存
            clearCacheAll("categoryAds");
            
            logger.info("✅ 广告缓存清理完成");
        } catch (Exception e) {
            logger.error("❌ 广告缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 评论编辑后刷新相关缓存
     */
    public void refreshCommentCache(Long videoId) {
        try {
            if (videoId != null) {
                // 清理特定视频的评论缓存
                clearCache("videoComments", videoId.toString());
            } else {
                // 清理所有评论缓存
                clearCacheAll("videoComments");
            }
            
            // 清理评论列表缓存（包括投稿评论等）
            clearCacheAll("commentList");
            
            logger.info("✅ 评论缓存清理完成: videoId={}", videoId);
        } catch (Exception e) {
            logger.error("❌ 评论缓存清理失败: videoId={}, 错误: {}", videoId, e.getMessage(), e);
        }
    }

    /**
     * 公告编辑后刷新相关缓存
     */
    public void refreshNoticeCache() {
        try {
            // 清理公告详情缓存
            clearCacheAll("sysNotice");
            
            // 清理公告列表缓存
            clearCacheAll("sysNoticeList");
            
            logger.info("✅ 公告缓存清理完成");
        } catch (Exception e) {
            logger.error("❌ 公告缓存清理失败, 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理指定缓存的特定键
     */
    private void clearCache(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("清理缓存: {} - {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.error("清理缓存失败: {} - {}, 错误: {}", cacheName, key, e.getMessage());
        }
    }

    /**
     * 清理指定缓存的所有内容
     */
    public void clearCacheAll(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("清理全部缓存: {}", cacheName);
            }
        } catch (Exception e) {
            logger.error("清理全部缓存失败: {}, 错误: {}", cacheName, e.getMessage());
        }
    }
    
    // ==================== 多分类缓存失效支持 ====================
    
    /**
     * 获取视频的所有相关分类ID（支持多分类）
     * 
     * @param videoId 视频ID
     * @param primaryCategoryId 主分类ID（来自videos表的category_id字段）
     * @return 所有相关分类ID列表
     */
    private List<Long> getAllRelatedCategoryIds(Long videoId, Long primaryCategoryId) {
        Set<Long> categoryIds = new LinkedHashSet<>(); // 保持顺序，避免重复
        
        try {
            // 🔍 步骤1：添加主分类（来自videos表的category_id）
            if (primaryCategoryId != null) {
                categoryIds.add(primaryCategoryId);
                logger.debug("📂 添加主分类: {}", primaryCategoryId);
            }
            
            // 🔍 步骤2：查询并添加关联分类（来自video_category_tag_relations表）
            // 只处理普通分类，合集分类使用独立缓存策略
            if (videoId != null) {
                try {
                    // 通过ApplicationContext获取VideoService，避免循环依赖
                    IVideoService videoService = applicationContext.getBean(IVideoService.class);
                    List<Category> relatedCategories = videoService.selectCategoriesByVideoId(videoId);
                    
                    if (relatedCategories != null && !relatedCategories.isEmpty()) {
                        for (Category category : relatedCategories) {
                            // 🔍 只处理普通分类（非合集分类）
                            if (category.getIsCollection() == null || category.getIsCollection() == 0) {
                                categoryIds.add(category.getId());
                                logger.debug("🔗 添加普通分类: {} ({})", category.getId(), category.getName());
                            } else {
                                logger.debug("⏭️ 跳过合集分类: {} ({}) - 使用独立缓存策略", category.getId(), category.getName());
                            }
                        }
                        logger.info("🎯 视频{}找到{}个关联分类，已过滤合集分类", videoId, relatedCategories.size());
                    } else {
                        logger.debug("📋 视频{}没有关联分类", videoId);
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ 查询视频{}的关联分类失败: {}", videoId, e.getMessage());
                }
            }
            
            // 🔍 步骤3：智能处理全部视频分类
            // 只有在确实需要影响全部视频列表时才添加null分类
            boolean shouldAffectAllVideos = primaryCategoryId == null || 
                                          (videoId != null && isVideoVisibleInAllList(videoId));
            
            if (shouldAffectAllVideos) {
                categoryIds.add(null);
                logger.debug("📋 添加全部视频分类: null（视频在全部列表中可见）");
            } else {
                logger.debug("⏭️ 跳过全部视频分类: null（视频不在全部列表中或不影响全部列表）");
            }
            
            List<Long> result = new ArrayList<>(categoryIds);
            logger.info("🎯 视频{}的完整分类列表: {}", videoId, result);
            return result;
            
        } catch (Exception e) {
            logger.warn("⚠️ 获取视频{}的关联分类失败，降级到基础失效: {}", videoId, e.getMessage());
            // 降级策略：至少失效主分类和null分类
            List<Long> fallbackCategories = new ArrayList<>();
            if (primaryCategoryId != null) {
                fallbackCategories.add(primaryCategoryId);
            }
            fallbackCategories.add(null);
            return fallbackCategories;
        }
    }
    
    /**
     * 如果视频属于合集分类，刷新合集独立缓存
     * 
     * @param videoId 视频ID
     */
    private void refreshCollectionCacheIfNeeded(Long videoId) {
        try {
            // 查询视频的所有关联分类
            IVideoService videoService = applicationContext.getBean(IVideoService.class);
            List<Category> relatedCategories = videoService.selectCategoriesByVideoId(videoId);
            
            if (relatedCategories != null && !relatedCategories.isEmpty()) {
                boolean hasCollectionCategory = false;
                for (Category category : relatedCategories) {
                    if (category.getIsCollection() != null && category.getIsCollection() == 1) {
                        hasCollectionCategory = true;
                        logger.debug("🗂️ 发现视频{}属于合集分类: {} ({})", videoId, category.getId(), category.getName());
                        break;
                    }
                }
                
                if (hasCollectionCategory) {
                    // 刷新合集独立缓存
                    refreshCollectionCache(null, null); // null表示刷新所有合集缓存
                    logger.info("✅ 已刷新合集独立缓存，因为视频{}属于合集分类", videoId);
                } else {
                    logger.debug("📋 视频{}不属于合集分类，跳过合集缓存刷新", videoId);
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 检查视频{}合集分类关系失败: {}", videoId, e.getMessage());
        }
    }
    
    /**
     * 判断视频是否在"全部视频"列表中可见
     * 只有状态为已发布(status=1)的视频才会在全部列表中显示
     * 
     * @param videoId 视频ID
     * @return 是否在全部列表中可见
     */
    private boolean isVideoVisibleInAllList(Long videoId) {
        try {
            IVideoService videoService = applicationContext.getBean(IVideoService.class);
            Video video = videoService.selectVideoById(videoId);
            
            // 只有已发布的视频才在全部列表中可见
            boolean isVisible = video != null && video.getStatus() != null && video.getStatus() == 1;
            logger.debug("🔍 检查视频{}在全部列表中的可见性: {}", videoId, isVisible);
            return isVisible;
            
        } catch (Exception e) {
            logger.warn("⚠️ 检查视频{}可见性失败，默认认为可见: {}", videoId, e.getMessage());
            // 出错时保守处理，认为可见（确保缓存一致性）
            return true;
        }
    }
    

    
    /**
     * 精确清理特定分类的视频排序缓存
     * 
     * @param categoryId 分类ID
     */
    public void clearSpecificCategoryVideoSortCache(Long categoryId) {
        try {
            String categoryKey = categoryId != null ? categoryId.toString() : "all";
            
            // 🎯 精确清理分类排序缓存
            clearRedisCacheByPattern("categoryVideoSort", "hasSort_" + categoryKey);
            clearRedisCacheByPattern("categoryVideoSort", "mixed_sort_" + categoryKey + "_*");
            clearRedisCacheByPattern("categoryVideoSort", "category_mixed_" + categoryKey + "_*");
            
            logger.info("📂 已精确清理分类{}的视频排序缓存", categoryKey);
        } catch (Exception e) {
            logger.warn("⚠️ 精确清理排序缓存失败，降级到全量清理: {}", e.getMessage());
            clearCacheAll("categoryVideoSort");
            logger.info("✅ 降级成功：已清理所有视频排序缓存");
        }
    }

    /**
     * 使用Redis模式匹配精确清理缓存
     * 
     * @param cacheName 缓存名称
     * @param pattern 匹配模式
     */
    private void clearRedisCacheByPattern(String cacheName, String pattern) {
        try {
            // 构造Redis键模式：缓存名称::键模式
            String redisKeyPattern = cacheName + "::" + pattern;

            // 使用 SCAN 游标迭代（非阻塞），避免 KEYS 命令在大量键时阻塞 Redis
            List<Object> keysToDelete = new ArrayList<>();
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(redisKeyPattern).count(100).build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        byte[] keyBytes = cursor.next();
                        if (keyBytes != null) {
                            keysToDelete.add(new String(keyBytes, StandardCharsets.UTF_8));
                        }
                    }
                } catch (Exception scanEx) {
                    logger.warn("⚠️ SCAN 游标迭代失败: pattern={}, error={}", redisKeyPattern, scanEx.getMessage());
                }
                return null;
            });

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                logger.debug("🎯 精确删除Redis缓存键: {} 个键匹配模式 {}", keysToDelete.size(), redisKeyPattern);
            } else {
                logger.debug("🔍 没有找到匹配模式的缓存键: {}", redisKeyPattern);
            }

        } catch (Exception e) {
            logger.warn("⚠️ Redis精确清理失败: cacheName={}, pattern={}, error={}", cacheName, pattern, e.getMessage());
            // 降级：清理整个缓存
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.warn("🔄 降级清理整个缓存: {}", cacheName);
                }
            } catch (Exception fallbackEx) {
                logger.error("❌ 降级清理也失败: {}", cacheName, fallbackEx);
            }
        }
    }

    /**
     * 智能清理短视频相关缓存
     * 当视频或标签发生变化时调用
     * 
     * @param videoId 视频ID（可选）
     * @param categoryId 分类ID（可选）
     * @param tagId 标签ID（可选）
     * @param globalClear 是否全局清理
     */
    public void invalidateShortVideoCache(Long videoId, Long categoryId, Long tagId, boolean globalClear) {
        try {
            logger.info("🎬 开始清理短视频缓存: videoId={}, categoryId={}, tagId={}, globalClear={}", 
                       videoId, categoryId, tagId, globalClear);
            
            if (globalClear) {
                // 全局清理所有短视频缓存
                clearAllShortVideoCache();
            } else {
                // 精确清理特定条件的缓存
                clearSpecificShortVideoCache(categoryId, tagId);
            }
            
            // 清理批量查询缓存（当视频标签关系发生变化时）
            if (videoId != null || globalClear) {
                clearBatchQueryCache();
            }
            
            logger.info("✅ 短视频缓存清理完成");
        } catch (Exception e) {
            logger.error("❌ 清理短视频缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理所有短视频相关缓存
     */
    private void clearAllShortVideoCache() {
        try {
            // 清理短视频列表缓存
            Cache shortVideoListCache = cacheManager.getCache("shortVideoList");
            if (shortVideoListCache != null) {
                shortVideoListCache.clear();
                logger.info("🧹 已清理所有短视频列表缓存");
            }
            
            // 清理短视频计数缓存
            Cache shortVideoCountCache = cacheManager.getCache("shortVideoCount");
            if (shortVideoCountCache != null) {
                shortVideoCountCache.clear();
                logger.info("🧹 已清理所有短视频计数缓存");
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ 清理所有短视频缓存时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 精确清理特定条件的短视频缓存
     */
    private void clearSpecificShortVideoCache(Long categoryId, Long tagId) {
        try {
            // 构建需要清理的缓存键模式
            List<String> patternsToClean = new ArrayList<>();
            
            if (categoryId != null) {
                // 清理特定分类的短视频缓存
                patternsToClean.add("short_" + categoryId + "_*");
                logger.info("🎯 将清理分类{}的短视频缓存", categoryId);
            }
            
            if (tagId != null) {
                // 清理特定标签的短视频缓存
                patternsToClean.add("short_*_" + tagId + "_*");
                logger.info("🏷️ 将清理标签{}的短视频缓存", tagId);
            }
            
            // 如果没有指定具体条件，清理全部缓存
            if (patternsToClean.isEmpty()) {
                patternsToClean.add("short_*");
                patternsToClean.add("count_*");
                logger.info("🌐 将清理所有短视频缓存");
            }
            
            // 执行缓存清理
            for (String pattern : patternsToClean) {
                clearRedisCacheByPattern("shortVideoList", pattern);
                clearRedisCacheByPattern("shortVideoCount", pattern.replace("short_", "count_"));
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ 精确清理短视频缓存时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 清理批量查询缓存
     */
    private void clearBatchQueryCache() {
        try {
            // 清理批量标签查询缓存
            Cache videoTagsBatchCache = cacheManager.getCache("videoTagsBatch");
            if (videoTagsBatchCache != null) {
                videoTagsBatchCache.clear();
                logger.info("🧹 已清理批量标签查询缓存");
            }
            
            // 清理批量分类查询缓存
            Cache videoCategoriesBatchCache = cacheManager.getCache("videoCategoriesBatch");
            if (videoCategoriesBatchCache != null) {
                videoCategoriesBatchCache.clear();
                logger.info("🧹 已清理批量分类查询缓存");
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ 清理批量查询缓存时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 视频更新时的缓存失效（扩展原有方法以支持短视频）
     */
    public void invalidateVideoRelatedCache(Long videoId, Long categoryId, Long tagId) {
        try {
            // 调用原有的智能缓存失效
            smartRefreshVideoCache(videoId, categoryId, false);
            
            // 额外清理短视频缓存
            invalidateShortVideoCache(videoId, categoryId, tagId, false);
            
        } catch (Exception e) {
            logger.error("❌ 视频相关缓存失效失败: videoId={}, error={}", videoId, e.getMessage(), e);
        }
    }

}