package com.ruoyi.chigua.controller.web;

import com.ruoyi.chigua.service.VideoSitemapService;
import com.ruoyi.chigua.service.ISeoKeywordService;
import com.ruoyi.chigua.domain.SeoKeyword;
import com.ruoyi.chigua.config.ChiguaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * 站点地图Controller
 * 
 * @author ruoyi
 * @date 2025-01-29
 */
@RestController
@RequestMapping("/web/api/sitemap")
@CrossOrigin(origins = "*")
public class WebSitemapController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSitemapController.class);
    
    @Autowired
    private VideoSitemapService videoSitemapService;

    @Autowired
    private com.ruoyi.chigua.service.TgSitemapService tgSitemapService;

    @Autowired
    private ISeoKeywordService seoKeywordService;
    
    @Autowired
    private ChiguaProperties chiguaProperties;
    
    /**
     * SEO配置开关
     * 默认false（泛域名环境禁用）
     * tycg8.com环境设置为true
     */
    @Value("${chigua.seo.sitemap-enabled:false}")
    private boolean sitemapEnabled;
    
    /**
     * 获取站点地图索引
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping(value = "/sitemap-index.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemapIndex() {
        // 检查是否启用Sitemap功能（泛域名环境返回404）
        if (!sitemapEnabled) {
            logger.warn("⚠️ Sitemap功能未启用（泛域名环境），返回404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        try {
            logger.info("📄 请求站点地图索引");
            String xml = videoSitemapService.generateSitemapIndex();
            
            // 验证 XML 不为空
            if (xml == null || xml.trim().isEmpty()) {
                logger.error("❌ 站点地图索引为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            // 设置正确的 Content-Type（Google 要求）
            headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
            // 允许所有来源访问（包括Google爬虫）
            headers.set("Access-Control-Allow-Origin", "*");
            // 设置缓存控制（24小时，与站点地图缓存一致）
            headers.setCacheControl("public, max-age=300, must-revalidate");
            // 添加 X-Robots-Tag，允许索引
            headers.set("X-Robots-Tag", "index, follow");
            // 设置内容长度
            headers.setContentLength(xml.getBytes(StandardCharsets.UTF_8).length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(xml);
        } catch (Exception e) {
            logger.error("❌ 获取站点地图索引失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取视频站点地图文件
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping(value = "/video-sitemap-{n}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getVideoSitemap(@PathVariable int n) {
        // 检查是否启用Sitemap功能（泛域名环境返回404）
        if (!sitemapEnabled) {
            logger.warn("⚠️ Sitemap功能未启用（泛域名环境），返回404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        try {
            logger.info("📄 请求视频站点地图 #{}", n);
            
            if (n < 1) {
                logger.warn("⚠️ 无效的站点地图编号: {}", n);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            String xml = videoSitemapService.generateVideoSitemap(n);
            
            // 验证 XML 不为空
            if (xml == null || xml.trim().isEmpty()) {
                logger.warn("⚠️ 站点地图 #{} 为空", n);
                // 返回空的站点地图而不是错误（符合 sitemap 协议）
                xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
                      "        xmlns:video=\"http://www.google.com/schemas/sitemap-video/1.1\">\n" +
                      "</urlset>";
            }
            
            HttpHeaders headers = new HttpHeaders();
            // 设置正确的 Content-Type（Google 要求）
            headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
            // 允许所有来源访问（包括Google爬虫）
            headers.set("Access-Control-Allow-Origin", "*");
            // 设置缓存控制（24小时，与站点地图缓存一致）
            headers.setCacheControl("public, max-age=300, must-revalidate");
            // 添加 X-Robots-Tag，允许索引
            headers.set("X-Robots-Tag", "index, follow");
            // 设置内容长度
            headers.setContentLength(xml.getBytes(StandardCharsets.UTF_8).length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(xml);
        } catch (Exception e) {
            logger.error("❌ 获取视频站点地图 #{} 失败", n, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /** TG 站点地图固定域名（用于 GSC 提交，不受泛域名影响） */
    private static final String TG_SITEMAP_BASE_URL = "https://tycg7.com";

    /** tycg7.com 专版 sitemap 固定域名（用于 GSC 提交，强制 URL 与 Property 同源） */
    private static final String TYCG7_BASE_URL = "https://tycg7.com";

    /** keyword sitemap 单文件分片大小（参考 51cg1.com，5000 URL/片 ~ 200KB） */
    private static final int TYCG7_KEYWORD_BATCH = 5000;

    /**
     * tycg7.com 专用 sitemap 总索引
     * 路径：/web/api/sitemap/tycg7-sitemap-index.xml
     *
     * 内容：N 个 video 分片 + M 个 keyword 分片（均 5000/片）
     * 参考 51cg1.com 的稳健分片策略：小文件 + 多分片，抓取耗时 < 1s，零超时。
     * TG sitemap 已有独立索引，不在此处包含。
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping(value = "/tycg7-sitemap-index-v2.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTycg7SitemapIndex() {
        try {
            logger.info("📄 请求 tycg7 站点地图索引");
            int videoCount = videoSitemapService.getTycg7VideoSitemapCount();
            int keywordTotal = seoKeywordService.countActiveKeywords();
            int keywordCount = (keywordTotal + TYCG7_KEYWORD_BATCH - 1) / TYCG7_KEYWORD_BATCH;
            String today = java.time.LocalDate.now().toString();
            String apiPrefix = TYCG7_BASE_URL.contains("localhost") ? "" : "/prod-api";

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            for (int i = 1; i <= videoCount; i++) {
                xml.append("  <sitemap>\n");
                xml.append("    <loc>").append(TYCG7_BASE_URL).append(apiPrefix)
                        .append("/web/api/sitemap/tycg7-video-v2-").append(i).append(".xml</loc>\n");
                xml.append("    <lastmod>").append(today).append("</lastmod>\n");
                xml.append("  </sitemap>\n");
            }
            for (int i = 1; i <= keywordCount; i++) {
                xml.append("  <sitemap>\n");
                xml.append("    <loc>").append(TYCG7_BASE_URL).append(apiPrefix)
                        .append("/web/api/sitemap/tycg7-keywords-v2-").append(i).append(".xml</loc>\n");
                xml.append("    <lastmod>").append(today).append("</lastmod>\n");
                xml.append("  </sitemap>\n");
            }
            xml.append("</sitemapindex>");
            String result = xml.toString();
            logger.info("✅ tycg7 索引: video={} 片, keyword={} 片", videoCount, keywordCount);
            return ResponseEntity.ok().headers(buildXmlHeaders(result)).body(result);
        } catch (Exception e) {
            logger.error("❌ 获取 tycg7 站点地图索引失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * tycg7.com 专用视频站点地图（精简版：只 loc + lastmod）
     * 路径：/web/api/sitemap/tycg7-video-sitemap-{n}.xml
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping(value = "/tycg7-video-v2-{n}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTycg7VideoSitemap(@PathVariable int n) {
        try {
            if (n < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            logger.info("📄 请求 tycg7 视频站点地图 #{}", n);
            String xml = videoSitemapService.generateVideoSitemap(TYCG7_BASE_URL, n);
            if (xml == null || xml.trim().isEmpty()) {
                xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                    + "</urlset>";
            }
            return ResponseEntity.ok().headers(buildXmlHeaders(xml)).body(xml);
        } catch (Exception e) {
            logger.error("❌ 获取 tycg7 视频站点地图 #{} 失败", n, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * tycg7.com 专用 SEO 关键词站点地图（分片版，每片 {@link #TYCG7_KEYWORD_BATCH} 个）
     * 路径：/web/api/sitemap/tycg7-seo-keywords-sitemap-{n}.xml
     *
     * 精简风格（参考 51cg1.com）：只 loc + lastmod，去掉 changefreq/priority。
     * 单文件 ~5000 URL / ~200KB，抓取秒返。
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping(value = "/tycg7-keywords-v2-{n}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTycg7SeoKeywordsSitemapShard(@PathVariable int n) {
        try {
            if (n < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            logger.info("📄 请求 tycg7 SEO 关键词站点地图 #{}", n);
            String result = seoKeywordService.generateKeywordSitemapShard(TYCG7_BASE_URL, n, TYCG7_KEYWORD_BATCH);
            return ResponseEntity.ok().headers(buildXmlHeaders(result)).body(result);
        } catch (Exception e) {
            logger.error("❌ 获取 tycg7 SEO 关键词站点地图 #{} 失败", n, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 兼容旧 URL：tycg7-seo-keywords-sitemap.xml（不带分片号）
     * 返回第 1 片的内容，避免 GSC 历史提交的 URL 持续报"无法抓取"。
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping(value = "/tycg7-seo-keywords-sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTycg7SeoKeywordsSitemapLegacy() {
        return getTycg7SeoKeywordsSitemapShard(1);
    }

    /**
     * Telegram 帖子站点地图索引（固定域名 tycg7.com，始终可用）
     * 路径：/web/api/sitemap/tg-sitemap-index.xml
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping(value = "/tg-sitemap-index.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTgSitemapIndex(HttpServletRequest request) {
        try {
            logger.info("📄 请求 TG 站点地图索引, baseUrl={}", TG_SITEMAP_BASE_URL);
            String xml = tgSitemapService.generateSitemapIndex(TG_SITEMAP_BASE_URL);
            if (xml == null || xml.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            return ResponseEntity.ok().headers(buildXmlHeaders(xml)).body(xml);
        } catch (Exception e) {
            logger.error("❌ 获取 TG 站点地图索引失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 第 N 个 Telegram 站点地图文件（固定域名 tycg7.com，始终可用）
     * 路径：/web/api/sitemap/tg-sitemap-{n}.xml
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @GetMapping(value = "/tg-sitemap-{n}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getTgSitemap(@PathVariable int n, HttpServletRequest request) {
        try {
            if (n < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            logger.info("📄 请求 TG 站点地图 #{}, baseUrl={}", n, TG_SITEMAP_BASE_URL);
            String xml = tgSitemapService.generateTgSitemap(TG_SITEMAP_BASE_URL, n);
            if (xml == null || xml.trim().isEmpty()) {
                xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                      "</urlset>";
            }
            return ResponseEntity.ok().headers(buildXmlHeaders(xml)).body(xml);
        } catch (Exception e) {
            logger.error("❌ 获取 TG 站点地图 #{} 失败", n, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 统一构造 XML 响应头（Google 爬虫友好）
     *
     * Cache-Control 设为 5 分钟 + must-revalidate：
     *   - 旧的 max-age=86400 会让 Googlebot 把响应在自己内部缓存 24h，
     *     即便我们 submit-sitemap "申请重抓"，Google 也可能直接复用旧快照，
     *     造成 GSC 长期显示一个早已不存在的错误。
     *   - 改为 300s 后，Googlebot 5 分钟内会主动带 If-Modified-Since 回源校验，
     *     新内容能在一个抓取周期内即时反映到 GSC 状态。
     *   - 5 分钟仍能挡住短时间内 EdgeOne/Nginx 反复回源，平衡命中率与时效性。
     */
    private HttpHeaders buildXmlHeaders(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
        headers.set("Access-Control-Allow-Origin", "*");
        headers.setCacheControl("public, max-age=300, must-revalidate");
        headers.set("X-Robots-Tag", "index, follow");
        headers.setContentLength(body.getBytes(StandardCharsets.UTF_8).length);
        return headers;
    }

    /**
     * 从请求中推断 baseUrl（泛域名+CDN 友好）
     * 优先级：X-Forwarded-Proto + X-Forwarded-Host > Host > request.scheme+serverName
     *
     * 例：用户访问 https://search.cqqvl.cc/prod-api/web/api/sitemap/tg-sitemap-index.xml
     * 经 Cloudflare 与 Nginx 转发到后端，期望返回的 sitemap 内 URL 也是 https://search.cqqvl.cc/*
     */
    private String resolveBaseUrl(HttpServletRequest request) {
        String proto = firstNonBlank(
                request.getHeader("X-Forwarded-Proto"),
                request.getHeader("CF-Visitor-Proto"),
                request.getScheme()
        );
        // CF-Visitor header 形如 {"scheme":"https"}
        String cfVisitor = request.getHeader("CF-Visitor");
        if (cfVisitor != null && cfVisitor.contains("https")) {
            proto = "https";
        }
        String host = firstNonBlank(
                request.getHeader("X-Forwarded-Host"),
                request.getHeader("Host"),
                request.getServerName()
        );
        if (host == null) host = "tycg8.com";
        // X-Forwarded-Host 可能含端口或多个值（逗号分隔）
        int comma = host.indexOf(',');
        if (comma >= 0) host = host.substring(0, comma).trim();
        // 移除默认端口
        if (host.endsWith(":80") || host.endsWith(":443")) {
            host = host.substring(0, host.lastIndexOf(':'));
        }
        return (proto == null ? "https" : proto) + "://" + host;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    /**
     * 获取SEO关键词站点地图
     * 为50000+关键词生成sitemap，让Google索引所有关键词页面
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @GetMapping(value = "/seo-keywords-sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSeoKeywordsSitemap() {
        // 检查是否启用Sitemap功能（泛域名环境返回404）
        if (!sitemapEnabled) {
            logger.warn("⚠️ Sitemap功能未启用（泛域名环境），返回404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        try {
            logger.info("📄 请求SEO关键词站点地图");
            
            // 获取所有启用的关键词
            List<SeoKeyword> keywords = seoKeywordService.getAllActiveKeywords();
            String baseUrl = chiguaProperties != null && chiguaProperties.getDomains() != null 
                ? chiguaProperties.getDomains().getSite() 
                : "https://tycg8.com";
            
            if (keywords == null || keywords.isEmpty()) {
                logger.warn("⚠️ 没有启用的SEO关键词");
                // 返回空sitemap
                String emptyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
                      "</urlset>";
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                    .body(emptyXml);
            }
            
            // 生成sitemap XML
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            
            for (SeoKeyword keyword : keywords) {
                xml.append("  <url>\n");
                xml.append("    <loc>").append(escapeXml(baseUrl)).append("/keyword/").append(keyword.getId()).append("</loc>\n");
                xml.append("    <changefreq>weekly</changefreq>\n");
                xml.append("    <priority>").append(keyword.getPriority() != null ? keyword.getPriority() : "0.6").append("</priority>\n");
                xml.append("  </url>\n");
            }
            
            xml.append("</urlset>");
            
            String result = xml.toString();
            logger.info("✅ 生成SEO关键词站点地图成功，共 {} 个关键词", keywords.size());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
            headers.set("Access-Control-Allow-Origin", "*");
            headers.setCacheControl("public, max-age=300, must-revalidate"); // 24小时缓存
            headers.set("X-Robots-Tag", "index, follow");
            headers.setContentLength(result.getBytes(StandardCharsets.UTF_8).length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(result);
        } catch (Exception e) {
            logger.error("❌ 获取SEO关键词站点地图失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * XML特殊字符转义
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}

