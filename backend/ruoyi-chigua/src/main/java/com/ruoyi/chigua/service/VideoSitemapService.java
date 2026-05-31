package com.ruoyi.chigua.service;

import com.ruoyi.chigua.config.ChiguaProperties;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.mapper.VideoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 视频站点地图生成服务
 * 
 * @author ruoyi
 * @date 2025-01-29
 */
@Service
public class VideoSitemapService {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoSitemapService.class);
    
    @Autowired
    private VideoMapper videoMapper;
    
    @Autowired
    private ChiguaProperties chiguaProperties;
    
    /**
     * 获取API域名（用于站点地图API访问）
     * 优先使用配置的apiDomain（泛域名，有Nginx代理到后端）
     * 如果没有配置，回退到site域名（但会记录警告日志）
     */
    private String getApiDomain() {
        if (chiguaProperties != null && chiguaProperties.getDomains() != null) {
            String apiDomain = chiguaProperties.getDomains().getApiDomain();
            if (StringUtils.hasText(apiDomain)) {
                return apiDomain;
            }
        }
        // 如果没有配置API域名，回退到site域名（但这不是最佳实践）
        logger.warn("⚠️ 未配置API域名（chigua.domains.api-domain），使用site域名作为回退，可能导致站点地图无法访问后端API");
        return getSiteDomain();
    }
    
    /**
     * 获取网站主域名（用于生成站点地图URL）
     * 从 yml 配置文件的 chigua.domains.site 读取，避免暴露 tycg.com
     * 
     * 说明：
     * - 使用备用域名（tycg1.com 到 tycg6.com）作为站点地图域名
     * - 避免主域名（tycg.com）被举报或被Google黑名单
     * - 优先使用配置的site域名（通常是tycg1.com）
     * - 这些域名指向落地页，用户选择线路后跳转到泛域名
     */
    private String getSiteDomain() {
        if (chiguaProperties != null && chiguaProperties.getDomains() != null) {
            // 优先使用配置的site域名（通常是tycg1.com）
            String site = chiguaProperties.getDomains().getSite();
            if (StringUtils.hasText(site)) {
                return site;
            }
            
            // 如果没有配置site，回退到main
            return chiguaProperties.getDomains().getMain();
        }
        return "https://tycg8.com"; // 默认值（不使用tycg.com）
    }
    
    /**
     * 根据批次号获取域名（用于轮换域名，分散风险）
     * 不同批次使用不同域名，但同一批次使用固定域名
     */
    private String getSiteDomainByBatch(int batchNumber) {
        if (chiguaProperties != null && chiguaProperties.getDomains() != null) {
            List<String> alternatives = chiguaProperties.getDomains().getSiteAlternatives();
            if (alternatives != null && !alternatives.isEmpty()) {
                // 根据批次号选择域名（轮换策略）
                int index = (batchNumber - 1) % alternatives.size();
                String selected = alternatives.get(index);
                logger.info("🎲 批次 #{} 使用域名: {}", batchNumber, selected);
                return selected;
            }
        }
        // 如果没有备用域名列表，使用默认域名
        return getSiteDomain();
    }
    
    private static final int BATCH_SIZE = 10000; // 每个站点地图最多10,000个视频
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+00:00");
    
    /**
     * 生成站点地图索引（使用缓存，24小时自动过期）
     */
    @Cacheable(value = "videoSitemap", key = "'index'", unless = "#result == null")
    public String generateSitemapIndex() {
        logger.info("🔍 生成站点地图索引（缓存未命中）");
        
        try {
            // 查询视频总数（仅普通分类，排除短视频和分页模式分类）
            long totalVideos = videoMapper.countVideoListForSitemap();
            
            int totalSitemaps = (int) ((totalVideos + BATCH_SIZE - 1) / BATCH_SIZE);
            
            logger.info("📊 视频总数: {}, 需要生成 {} 个站点地图文件", totalVideos, totalSitemaps);
            
            // 生成站点地图索引XML
            return generateSitemapIndexXml(totalSitemaps);
        } catch (Exception e) {
            logger.error("❌ 生成站点地图索引失败", e);
            throw e;
        }
    }
    
    /**
     * 生成单个视频站点地图（使用缓存，24小时自动过期）
     */
    @Cacheable(value = "videoSitemap", key = "'sitemap_' + #batchNumber", unless = "#result == null")
    public String generateVideoSitemap(int batchNumber) {
        logger.info("🔍 生成视频站点地图 #{}（缓存未命中）", batchNumber);
        
        try {
            int offset = (batchNumber - 1) * BATCH_SIZE;
            
            // 使用专门的分页查询方法（性能优化：避免一次性加载所有视频）
            List<Video> videos = videoMapper.selectVideoListForSitemap(offset, BATCH_SIZE);
            
            if (videos == null || videos.isEmpty()) {
                logger.warn("⚠️ 站点地图 #{} 查询结果为空: offset={}, pageSize={}", batchNumber, offset, BATCH_SIZE);
                return generateEmptySitemapXml();
            }
            
            logger.info("📊 站点地图 #{} 包含 {} 个视频（范围: {}-{}）", batchNumber, videos.size(), offset, offset + videos.size());
            
            return generateVideoSitemapXml(videos, batchNumber);
        } catch (Exception e) {
            logger.error("❌ 生成视频站点地图 #{} 失败", batchNumber, e);
            throw e;
        }
    }
    
    /**
     * 生成站点地图索引（固定 baseUrl 版本，供 tycg7.com 等独立域名 GSC 提交使用）
     *
     * 与无参版本的区别：传入 baseUrl 后所有 sitemap 子链 + 内部 URL 都强制使用该域名，
     * 而不是从 chigua.domains.site 读取。cache key 加入 baseUrl 避免与无参版本互相覆盖。
     *
     * @param baseUrl     站点根 URL（如 https://tycg7.com），用于 sitemap 子链
     * @param apiBaseUrl  后端 API 根 URL（如 https://tycg7.com），用于拼 /prod-api/...
     */
    /**
     * tycg7 专用分片大小：参考 51cg1.com 的稳健分片（每片 5000 URL，约 400KB）
     * 抓取耗时 < 1s，远低于 GSC 超时阈值，从源头避免"无法抓取"
     */
    public static final int TYCG7_BATCH_SIZE = 5000;

    /**
     * 获取 tycg7 video 分片总数（供 controller 生成索引时使用）
     */
    public int getTycg7VideoSitemapCount() {
        long totalVideos = videoMapper.countVideoListForSitemap();
        return (int) ((totalVideos + TYCG7_BATCH_SIZE - 1) / TYCG7_BATCH_SIZE);
    }

    @Cacheable(value = "videoSitemap", key = "'index:' + #baseUrl + ':' + #apiBaseUrl", unless = "#result == null")
    public String generateSitemapIndex(String baseUrl, String apiBaseUrl) {
        logger.info("🔍 生成站点地图索引（baseUrl={}）", baseUrl);
        try {
            int totalSitemaps = getTycg7VideoSitemapCount();
            logger.info("📊 video 分片数: {} (每片 {} URL)", totalSitemaps, TYCG7_BATCH_SIZE);
            return generateSitemapIndexXmlForBase(totalSitemaps, baseUrl, apiBaseUrl);
        } catch (Exception e) {
            logger.error("❌ 生成站点地图索引失败 baseUrl={}", baseUrl, e);
            throw e;
        }
    }

    /**
     * 生成单个视频站点地图（固定 baseUrl 版本，使用 TYCG7_BATCH_SIZE 分片）
     */
    @Cacheable(value = "videoSitemap", key = "'sitemap:' + #baseUrl + ':' + #batchNumber", unless = "#result == null")
    public String generateVideoSitemap(String baseUrl, int batchNumber) {
        logger.info("🔍 生成视频站点地图 #{}（baseUrl={}, batchSize={}）", batchNumber, baseUrl, TYCG7_BATCH_SIZE);
        try {
            int offset = (batchNumber - 1) * TYCG7_BATCH_SIZE;
            List<Video> videos = videoMapper.selectVideoListForSitemap(offset, TYCG7_BATCH_SIZE);
            if (videos == null || videos.isEmpty()) {
                return generateEmptySitemapXml();
            }
            return generateVideoSitemapXmlForBase(videos, baseUrl);
        } catch (Exception e) {
            logger.error("❌ 生成视频站点地图 #{} 失败 baseUrl={}", batchNumber, baseUrl, e);
            throw e;
        }
    }

    /** 固定 baseUrl 版本的索引 XML 生成（仅 video 子 sitemap，keyword 由 controller 追加） */
    private String generateSitemapIndexXmlForBase(int totalSitemaps, String baseUrl, String apiBaseUrl) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        String today = LocalDate.now().format(DATE_FORMATTER);
        String apiPrefix = apiBaseUrl.contains("localhost") ? "" : "/prod-api";
        for (int i = 1; i <= totalSitemaps; i++) {
            xml.append("  <sitemap>\n");
            xml.append("    <loc>").append(apiBaseUrl).append(apiPrefix)
                    .append("/web/api/sitemap/tycg7-video-sitemap-").append(i).append(".xml</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("  </sitemap>\n");
        }
        xml.append("</sitemapindex>");
        return xml.toString();
    }

    /**
     * 固定 baseUrl 版本的 video sitemap XML 生成
     * 参考 51cg1.com 的精简风格：只保留 <loc> + <lastmod>，放弃视频富卡片以换取抓取稳定性。
     * 视频元数据由前端页面的 JSON-LD VideoObject 提供。
     */
    private String generateVideoSitemapXmlForBase(List<Video> videos, String baseUrl) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (Video video : videos) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/video/").append(video.getId()).append("</loc>\n");
            xml.append("    <lastmod>").append(formatDate(video.getUpdatedAt())).append("</lastmod>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * 生成空的站点地图XML（当查询范围无效时）
     */
    private String generateEmptySitemapXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
               "        xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\">\n" +
               "</urlset>";
    }
    
    /**
     * 生成站点地图索引XML
     */
    private String generateSitemapIndexXml(int totalSitemaps) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        // 使用站点域名（tycg8.com），站点地图索引中的URL应该使用站点域名
        String siteDomain = getSiteDomain();
        // 判断是否需要API前缀：如果使用localhost，则不需要/prod-api前缀；否则需要
        String apiPrefix = siteDomain.contains("localhost") ? "" : "/prod-api";
        
        // 添加视频sitemap（仅普通分类视频）
        for (int i = 1; i <= totalSitemaps; i++) {
            xml.append("  <sitemap>\n");
            xml.append("    <loc>").append(siteDomain).append(apiPrefix).append("/web/api/sitemap/video-sitemap-").append(i).append(".xml</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("  </sitemap>\n");
        }
        
        xml.append("</sitemapindex>");
        return xml.toString();
    }
    
    /**
     * 生成视频站点地图XML
     */
    private String generateVideoSitemapXml(List<Video> videos, int batchNumber) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\">\n");
        
        // 根据批次号选择域名（轮换策略，分散风险）
        String siteDomain = getSiteDomainByBatch(batchNumber);
        for (Video video : videos) {
            xml.append("  <url>\n");
            // 搜索结果指向实际视频详情页面，使用/video/ID格式
            // 格式：tycg8.com/video/123（站点地图使用站点域名tycg8.com）
            xml.append("    <loc>").append(siteDomain).append("/video/").append(video.getId()).append("</loc>\n");
            xml.append("    <lastmod>").append(formatDate(video.getUpdatedAt())).append("</lastmod>\n");
            xml.append("    <video:video>\n");
            
            // 视频标题
            if (StringUtils.hasText(video.getTitle())) {
                xml.append("      <video:title>").append(escapeXml(video.getTitle())).append("</video:title>\n");
            }
            
            // 视频描述（必须字段，添加回退逻辑以提升SEO效果）
            String description = video.getDescription();
            if (!StringUtils.hasText(description)) {
                // 回退到标题+默认描述
                description = video.getTitle() + " - 吃瓜 - 天涯吃瓜网";
            }
            xml.append("      <video:description>").append(escapeXml(description)).append("</video:description>\n");
            
            // 视频时长
            if (video.getDuration() != null && video.getDuration() > 0) {
                xml.append("      <video:duration>").append(video.getDuration()).append("</video:duration>\n");
            }
            
            // 发布时间
            if (video.getPublishedAt() != null) {
                xml.append("      <video:publication_date>").append(formatISO8601(video.getPublishedAt())).append("</video:publication_date>\n");
            }
            
            xml.append("    </video:video>\n");
            xml.append("  </url>\n");
        }
        
        xml.append("</urlset>");
        return xml.toString();
    }
    
    /**
     * 格式化日期（yyyy-MM-dd）
     */
    private String formatDate(java.util.Date date) {
        if (date == null) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        try {
            java.time.LocalDate localDate = date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            return localDate.format(DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
    }
    
    /**
     * 格式化日期（ISO 8601格式：yyyy-MM-ddTHH:mm:ss+00:00）
     */
    private String formatISO8601(java.util.Date date) {
        if (date == null) {
            return java.time.LocalDateTime.now().atZone(java.time.ZoneId.of("UTC")).format(ISO8601_FORMATTER);
        }
        try {
            java.time.ZonedDateTime zonedDateTime = date.toInstant()
                .atZone(java.time.ZoneId.of("UTC"));
            return zonedDateTime.format(ISO8601_FORMATTER);
        } catch (Exception e) {
            return java.time.LocalDateTime.now().atZone(java.time.ZoneId.of("UTC")).format(ISO8601_FORMATTER);
        }
    }
    
    /**
     * 转义XML特殊字符并过滤无效控制字符
     * XML规范只允许以下控制字符：制表符(0x09)、换行符(0x0A)、回车符(0x0D)
     * 其他控制字符(0x00-0x08, 0x0B-0x0C, 0x0E-0x1F)都是无效的，需要过滤掉
     * 这会导致 XML 解析错误：PCDATA invalid Char value X
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        
        // 先过滤无效的控制字符（保留制表符、换行符、回车符）
        StringBuilder cleaned = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            int codePoint = (int) c;
            // 允许的控制字符：制表符(9)、换行符(10)、回车符(13)
            // 允许其他所有字符（包括 Unicode 字符）
            if (codePoint == 0x09 || codePoint == 0x0A || codePoint == 0x0D || 
                (codePoint >= 0x20 && codePoint <= 0xD7FF) || 
                (codePoint >= 0xE000 && codePoint <= 0xFFFD) ||
                (codePoint >= 0x10000 && codePoint <= 0x10FFFF)) {
                cleaned.append(c);
            }
            // 其他控制字符（0x00-0x08, 0x0B-0x0C, 0x0E-0x1F）被过滤掉
        }
        
        // 转义XML特殊字符
        return cleaned.toString()
                   .replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}

