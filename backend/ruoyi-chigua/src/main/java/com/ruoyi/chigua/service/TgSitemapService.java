package com.ruoyi.chigua.service;

import com.ruoyi.chigua.config.ChiguaProperties;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.TgPost;
import com.ruoyi.chigua.mapper.TgPostMapper;
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
 * Telegram 帖子站点地图生成服务
 * <p>
 * 输出结构：
 *   tg-sitemap-index.xml          ── 索引，列出全部 tg-sitemap-N.xml
 *   tg-sitemap-{n}.xml            ── 每个文件最多 BATCH_SIZE 条 URL
 *
 * 站点地图包含：
 *   1. 所有启用的 Telegram 分类落地页：/category/{categoryId}
 *   2. 所有 status=1 的 Telegram 帖子详情页：/tg/post/{postId}
 *
 * 与 {@link VideoSitemapService} 共用域名读取逻辑，缓存独立（tgSitemap）
 */
@Service
public class TgSitemapService {

    private static final Logger logger = LoggerFactory.getLogger(TgSitemapService.class);

    /** 单个 sitemap 文件最多 URL 数量（Google 上限 50000） */
    private static final int BATCH_SIZE = 10000;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private TgPostMapper tgPostMapper;

    @Autowired
    private ChiguaProperties chiguaProperties;

    /**
     * 归一化 baseUrl：去掉末尾斜杠，确保以 http(s):// 开头
     * 泛域名环境下 baseUrl 由请求 Host 动态生成，不读 chigua.domains.site
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            if (chiguaProperties != null && chiguaProperties.getDomains() != null) {
                String s = chiguaProperties.getDomains().getSite();
                if (StringUtils.hasText(s)) baseUrl = s;
            }
        }
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "https://tycg8.com";
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    /**
     * 生成 Telegram 站点地图索引（按 host 维度 24 小时缓存）
     *
     * @param baseUrl 站点根 URL，如 https://search.cqqvl.cc
     *                泛域名环境下由 Controller 从请求 Host 提取传入
     */
    @Cacheable(value = "tgSitemap", key = "'index:' + #baseUrl", unless = "#result == null")
    public String generateSitemapIndex(String baseUrl) {
        String siteDomain = normalizeBaseUrl(baseUrl);
        logger.info("🔍 生成 TG 站点地图索引（缓存未命中, host={}）", siteDomain);

        long totalPosts = tgPostMapper.countTgPostsForSitemap();
        int totalSitemaps = Math.max(1, (int) ((totalPosts + BATCH_SIZE - 1) / BATCH_SIZE));

        logger.info("📊 TG 帖子总数: {}, 需生成 {} 个 sitemap 文件", totalPosts, totalSitemaps);

        String apiPrefix = siteDomain.contains("localhost") ? "" : "/prod-api";
        String today     = LocalDate.now().format(DATE_FORMATTER);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (int i = 1; i <= totalSitemaps; i++) {
            xml.append("  <sitemap>\n");
            xml.append("    <loc>").append(siteDomain).append(apiPrefix)
               .append("/web/api/sitemap/tg-sitemap-").append(i).append(".xml</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("  </sitemap>\n");
        }

        xml.append("</sitemapindex>");
        return xml.toString();
    }

    /**
     * 生成第 N 个 Telegram 站点地图（按 host + 批次维度 24 小时缓存）
     */
    @Cacheable(value = "tgSitemap", key = "'sitemap:' + #baseUrl + ':' + #batchNumber", unless = "#result == null")
    public String generateTgSitemap(String baseUrl, int batchNumber) {
        String siteDomain = normalizeBaseUrl(baseUrl);
        logger.info("🔍 生成 TG 站点地图 #{}（缓存未命中, host={}）", batchNumber, siteDomain);

        if (batchNumber < 1) {
            return emptyUrlSet();
        }

        int offset = (batchNumber - 1) * BATCH_SIZE;

        List<TgPost> posts = tgPostMapper.selectTgPostsForSitemap(offset, BATCH_SIZE);

        // 第 1 个 sitemap 文件附带所有分类落地页
        List<Category> categories = batchNumber == 1
                ? tgPostMapper.selectTgCategoriesForSitemap()
                : java.util.Collections.emptyList();

        if ((posts == null || posts.isEmpty()) && categories.isEmpty()) {
            logger.warn("⚠️ TG 站点地图 #{} 为空: offset={}, limit={}", batchNumber, offset, BATCH_SIZE);
            return emptyUrlSet();
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1) Telegram 分类落地页（仅写入第 1 个 sitemap）
        for (Category c : categories) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteDomain).append("/category/").append(c.getId()).append("</loc>\n");
            xml.append("    <lastmod>").append(formatDate(c.getUpdatedAt())).append("</lastmod>\n");
            xml.append("    <changefreq>daily</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        // 2) Telegram 帖子详情页
        if (posts != null) {
            for (TgPost p : posts) {
                xml.append("  <url>\n");
                xml.append("    <loc>").append(siteDomain).append("/tg/post/").append(p.getId()).append("</loc>\n");
                xml.append("    <lastmod>").append(formatDate(p.getUpdatedAt())).append("</lastmod>\n");
                xml.append("    <changefreq>weekly</changefreq>\n");
                xml.append("    <priority>0.6</priority>\n");
                xml.append("  </url>\n");
            }
        }

        xml.append("</urlset>");

        logger.info("✅ TG 站点地图 #{} 生成成功: 分类 {} 个, 帖子 {} 条",
                batchNumber, categories.size(), posts == null ? 0 : posts.size());
        return xml.toString();
    }

    /** 空 urlset，便于客户端解析 */
    private String emptyUrlSet() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
               "</urlset>";
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        try {
            return date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
    }
}
