package com.ruoyi.chigua.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 */
@Configuration
public class CacheConfig {

    @Autowired
    private ChiguaProperties chiguaProperties;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 基础缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 获取签名有效期（配置文件中的值是小时，需要转换为秒）
        // 注意：图片签名有效期是2小时（硬编码），M3U8是6小时，VIDEO/MP4是1小时（defaultExpires）
        // 为了安全，使用最短的签名有效期来计算缓存时间
        int imageSignatureExpiresHours = 2; // 图片签名有效期：2小时
        int videoSignatureExpiresHours = 1; // VIDEO/MP4签名有效期：1小时（与ChiguaUrlService.defaultExpires一致）
        int imageSignatureExpiresSeconds = imageSignatureExpiresHours * 3600;
        int videoSignatureExpiresSeconds = videoSignatureExpiresHours * 3600;
        // 缓存时间 = 签名有效期 - 5分钟安全边界
        // 重要：所有包含图片URL的缓存都不能超过图片签名有效期（2小时），否则图片链接会失效
        long cacheTTL = Math.max(120, imageSignatureExpiresSeconds - 300); // 2小时 - 5分钟 = 6900秒（1小时55分钟）
        // 包含VIDEO/MP4签名URL的缓存必须短于1小时，否则视频播放会失败
        long videoUrlCacheTTL = Math.max(120, videoSignatureExpiresSeconds - 600); // 1小时 - 10分钟 = 3000秒（50分钟）

        // 不同缓存类型的配置
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        
        // 视频相关缓存（基于签名时效）
        cacheConfigs.put("videoList", defaultConfig.entryTtl(Duration.ofHours(6))); // 新版统一使用长期缓存+版本控制
        cacheConfigs.put("videoListOptimized", defaultConfig.entryTtl(Duration.ofMinutes(30))); // 优化版视频列表缓存：30分钟
        
        // 签名URL缓存（基于图片签名有效期设置TTL，因为图片签名最短）
        // 图片签名2小时 - 10分钟安全边界 = 1小时50分钟（6600秒），最少5分钟
        long signedUrlCacheTTL = Math.max(300, cacheTTL - 600); // 6900 - 600 = 6300秒（1小时45分钟）
        cacheConfigs.put("signedUrlCache", defaultConfig.entryTtl(Duration.ofSeconds(signedUrlCacheTTL)));
        cacheConfigs.put("videoDetail", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        cacheConfigs.put("hotRecommendedVideosBasic", defaultConfig.entryTtl(Duration.ofHours(6))); // 热门推荐基础数据缓存（新版）
        cacheConfigs.put("collectionList", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        cacheConfigs.put("collectionDetail", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // 静态数据缓存（较长时间）
        cacheConfigs.put("categoryList", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("categoryDetail", defaultConfig.entryTtl(Duration.ofHours(2))); // 单个分类详情缓存2小时
        // tagList 已更名为 tagsList，此行已废弃
        cacheConfigs.put("pageConfigList", defaultConfig.entryTtl(Duration.ofHours(2))); // 页面配置缓存2小时
        
        // 富文本处理缓存（基于签名时效，稍短一些确保URL有效）
        long richTextCacheTTL = Math.max(60, cacheTTL - 600); // 比签名少10分钟，最少1分钟
        cacheConfigs.put("richTextProcessed", defaultConfig.entryTtl(Duration.ofSeconds(richTextCacheTTL)));
        
        // URL生成缓存（基于签名时效，稍短一些确保URL有效）
        long urlGenerationCacheTTL = Math.max(60, cacheTTL - 300); // 比签名少5分钟，最少1分钟
        cacheConfigs.put("urlGeneration", defaultConfig.entryTtl(Duration.ofSeconds(urlGenerationCacheTTL)));
        
        // 搜索缓存（TTL自动过期策略，15分钟）
        cacheConfigs.put("searchResult", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 测试缓存（短时间）
        cacheConfigs.put("testCache", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 广告缓存（基于签名时效，因为广告可能包含图片）
        cacheConfigs.put("categoryAds", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // 视频关联关系缓存（1小时）
        cacheConfigs.put("videoRelations", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Web合集列表缓存（基于签名时效，因为包含视频封面图片）
        cacheConfigs.put("webCollectionList", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // Web视频详情缓存（videoContent含VIDEO/MP4签名URL，必须短于VIDEO签名1小时）
        // ⚠️ 重要：前端 VIDEO_DETAIL 内存缓存最多10分钟，两层叠加最坏 = 后端TTL + 10min
        // 为保证签名始终有效，后端TTL设为40分钟（40+10=50 < 60min签名有效期，留10min安全边界）
        long webVideoDetailTTL = Math.max(120, videoSignatureExpiresSeconds - 1200); // 1小时 - 20分钟 = 40分钟
        cacheConfigs.put("webVideoDetail", defaultConfig.entryTtl(Duration.ofSeconds(webVideoDetailTTL)));
        
        // 相邻视频缓存（2小时，相对稳定的关系数据）
        cacheConfigs.put("adjacentVideos", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // 视频评论缓存（1小时）
        cacheConfigs.put("videoComments", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 评论列表缓存（1小时）
        cacheConfigs.put("commentList", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 往期内容归档缓存（基于签名时效，因为包含视频封面图片）
        cacheConfigs.put("archivesList", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // 标签列表缓存（基于签名时效，因为可能包含图片信息）
        cacheConfigs.put("tagsList", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // 标签视频列表缓存（基于签名时效，因为包含视频封面图片）
        cacheConfigs.put("tagVideoList", defaultConfig.entryTtl(Duration.ofSeconds(cacheTTL)));
        
        // 系统公告缓存（2小时）
        cacheConfigs.put("sysNotice", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("sysNoticeList", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // 页面配置缓存（长期缓存+版本控制）
        cacheConfigs.put("pageConfig", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 页面配置签名缓存（基于图片签名有效期，确保签名URL不会过期）
        // 图片签名有效期2小时，缓存时间设置为1小时45分钟（6300秒），确保在签名过期前刷新
        cacheConfigs.put("pageConfigSigned", defaultConfig.entryTtl(Duration.ofSeconds(richTextCacheTTL)));
        
        // 分类视频排序缓存（6小时，固定键策略）
        cacheConfigs.put("categoryVideoSort", defaultConfig.entryTtl(Duration.ofHours(6)));
        
        // 页面配置版本缓存（长期缓存+版本控制）
        cacheConfigs.put("pageConfigVersion", defaultConfig.entryTtl(Duration.ofHours(24)));

        // 短视频相关缓存配置（优化PC端短视频查询性能）
        cacheConfigs.put("shortVideoList", defaultConfig.entryTtl(Duration.ofMinutes(30))); // 短视频列表缓存：30分钟
        cacheConfigs.put("shortVideoCount", defaultConfig.entryTtl(Duration.ofHours(1))); // 短视频计数缓存：1小时
        cacheConfigs.put("videoTagsBatch", defaultConfig.entryTtl(Duration.ofHours(2))); // 批量视频标签缓存：2小时
        cacheConfigs.put("videoCategoriesBatch", defaultConfig.entryTtl(Duration.ofHours(2))); // 批量视频分类缓存：2小时

        // Redgifs用户缓存
        cacheConfigs.put("redgifsUserDetail", defaultConfig.entryTtl(Duration.ofHours(1)));   // 用户详情：1小时
        cacheConfigs.put("redgifsUserList", defaultConfig.entryTtl(Duration.ofMinutes(10)));   // 用户分页列表：10分钟，写操作自动清除

        // 站点地图缓存（24小时，自动过期后重新生成）
        cacheConfigs.put("videoSitemap", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("tgSitemap",    defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("seoKeywordSitemap", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}