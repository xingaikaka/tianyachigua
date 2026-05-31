package com.ruoyi.chigua.service;

import com.ruoyi.chigua.config.ChiguaProperties;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.service.ISysDictTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Chigua URL生成服务
 * 类似pornhub项目的R2SignatureService设计，简化为统一域名
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@Service
public class ChiguaUrlService {

    private static final Logger logger = LoggerFactory.getLogger(ChiguaUrlService.class);

    @Autowired
    private ChiguaProperties chiguaProperties;

    @Autowired
    private IpRegionService ipRegionService;

    @Autowired
    private ISysDictTypeService sysDictTypeService;

    /** 字典类型：CDN 域名配置 */
    private static final String CDN_DICT_TYPE = "cdntype";
    /** 字典标签：国内 CDN */
    private static final String LABEL_DOMESTIC = "国内";
    /** 字典标签：国外 CDN */
    private static final String LABEL_OVERSEAS = "国外";
    /** 字典类型：是否开启 IP 归属地过滤 */
    private static final String IP_FILTER_DICT_TYPE = "iplocal";
    /** IP 过滤开关值：开启 */
    private static final String IP_FILTER_YES = "yes";

    /**
     * 资源类型枚举
     * 虽然使用统一域名，但保留类型区分便于将来扩展（如不同签名策略）
     */
    public enum ResourceType {
        VIDEO,      // 视频文件（MP4）
        STREAM,     // 流媒体（M3U8）
        IMAGE,      // 普通图片
        THUMBNAIL,  // 缩略图
        COVER       // 封面图片
    }

    /**
     * 生成安全的资源URL - 统一接口
     * 
     * @param resourcePath 资源路径（不包含域名）
     * @param resourceType 资源类型
     * @return 完整的URL
     */
    public String generateUrl(String resourcePath, ResourceType resourceType) {
        // chigua-web 前端自己客户端解密，不需要 decrypt=true（只有 Worker 管理端才需要）
        return generateUrl(resourcePath, resourceType, false);
    }

    /**
     * 始终使用 Worker（tycgimage1.org）域名生成签名 URL，忽略系统参数中的 CDN 配置。
     * 适用于管理后台：Worker 负责服务端解密，前端无需客户端解密即可直接显示图片/视频。
     *
     * @param resourcePath 资源路径
     * @param resourceType 资源类型
     * @return Worker 签名 URL
     */
    public String generateWorkerUrl(String resourcePath, ResourceType resourceType) {
        if (!StringUtils.hasText(resourcePath)) {
            return null;
        }
        try {
            String domain = chiguaProperties.getDomains().getMain(); // 始终使用 Worker 域名
            String fullUrl = buildFullUrl(domain, resourcePath);
            fullUrl = stripSignatureParams(fullUrl);
            if (shouldProtectResource(resourcePath) && chiguaProperties.getUrl().isSignatureEnabled()) {
                // Worker 图片类资源需要 decrypt=true，触发服务端解密
                fullUrl = addSignature(fullUrl, resourceType, true);
            }
            return fullUrl;
        } catch (Exception e) {
            logger.warn("generateWorkerUrl 失败: {}, 错误: {}", resourcePath, e.getMessage());
            return buildFullUrl(chiguaProperties.getDomains().getMain(), resourcePath);
        }
    }

    /**
     * 生成安全的资源URL - 可控制是否添加解密参数
     * 
     * @param resourcePath 资源路径（不包含域名）
     * @param resourceType 资源类型
     * @param addDecryptParam 是否添加解密参数（Web前端设为false，后台管理设为true）
     * @return 完整的URL
     */
    public String generateUrl(String resourcePath, ResourceType resourceType, boolean addDecryptParam) {
        if (!StringUtils.hasText(resourcePath)) {
            return null;
        }

        try {
            // 1. 域名优先级：系统参数 > yml cdn > yml main（旧 Worker）
            String domain = getActiveCdnDomain();
            
            // 2. 构建完整URL（剥离已有签名参数，避免二次签名）
            String fullUrl = buildFullUrl(domain, resourcePath);
            fullUrl = stripSignatureParams(fullUrl);
            
            // 3. 判断是否需要签名保护
            boolean needsProtection = shouldProtectResource(resourcePath);
            if (needsProtection && chiguaProperties.getUrl().isSignatureEnabled()) {
                fullUrl = addSignature(fullUrl, resourceType, addDecryptParam);
            }
            
            return fullUrl;
            
        } catch (Exception e) {
            logger.warn("生成URL失败: {}, 错误: {}", resourcePath, e.getMessage());
            
            // 回退机制：使用当前生效域名生成基础URL
            if (chiguaProperties.getUrl().isFallbackEnabled()) {
                return buildFullUrl(getActiveCdnDomain(), resourcePath);
            }
            
            return null;
        }
    }

    /**
     * 为视频对象生成所有相关URL
     * 
     * @param video 视频对象
     * @return VideoUrls对象，包含所有URL
     */
    public VideoUrls generateVideoUrls(Video video) {
        if (video == null) {
            return new VideoUrls();
        }

        VideoUrls urls = new VideoUrls();
        
        try {
            // 视频播放URL（MP4）
            String videoPath = video.getVideoUrl();
            if (StringUtils.hasText(videoPath)) {
                urls.setVideoUrl(generateUrl(videoPath, ResourceType.VIDEO));
            }
            
            // M3U8播放URL
            String m3u8Path = video.getM3u8Url();
            if (StringUtils.hasText(m3u8Path)) {
                urls.setM3u8Url(generateUrl(m3u8Path, ResourceType.STREAM));
            }
            
            // 封面图片URL
            String coverPath = video.getCoverUrl();
            if (StringUtils.hasText(coverPath)) {
                urls.setCoverUrl(generateUrl(coverPath, ResourceType.COVER));
            }
            
            // 缩略图URL列表
            List<String> thumbnailPaths = video.getThumbnailUrls();
            List<String> thumbnailUrls = new ArrayList<>();
            for (String thumbPath : thumbnailPaths) {
                String thumbUrl = generateUrl(thumbPath, ResourceType.THUMBNAIL);
                if (StringUtils.hasText(thumbUrl)) {
                    thumbnailUrls.add(thumbUrl);
                }
            }
            urls.setThumbnailUrls(thumbnailUrls);
            
        } catch (Exception e) {
            logger.error("生成视频URL失败: videoId={}, 错误: {}", video.getId(), e.getMessage(), e);
        }
        
        return urls;
    }

    /**
     * 为转码记录生成所有相关URL - 基于数据库字段拼接
     * 
     * @param transcode 转码记录对象
     * @return VideoUrls对象，包含所有URL
     */
    public VideoUrls generateTranscodeUrls(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        VideoUrls urls = new VideoUrls();
        
        if (transcode == null) {
            return urls;
        }

        try {
            // 1. 视频播放URL（MP4）- 基于数据库字段拼接
            String videoPath = transcode.getVideoUrl();
            if (StringUtils.hasText(videoPath)) {
                urls.setVideoUrl(generateUrl(videoPath, ResourceType.VIDEO));
                logger.debug("生成视频URL: {} -> {}", videoPath, urls.getVideoUrl());
            }
            
            // 2. M3U8播放URL - 基于rpath拼接
            String m3u8Path = transcode.getM3u8Url();
            if (StringUtils.hasText(m3u8Path)) {
                urls.setM3u8Url(generateUrl(m3u8Path, ResourceType.STREAM));
                logger.debug("生成M3U8 URL: {} -> {}", m3u8Path, urls.getM3u8Url());
            }
            
            // 3. 封面图片URL - 基于coverImage字段
            String coverPath = transcode.getCoverUrl();
            if (StringUtils.hasText(coverPath)) {
                urls.setCoverUrl(generateUrl(coverPath, ResourceType.COVER));
                logger.debug("生成封面URL: {} -> {}", coverPath, urls.getCoverUrl());
            }
            
            // 4. 缩略图URL列表 - 基于thumbnails字段
            List<String> thumbnailUrls = new ArrayList<>();
            List<String> thumbnailPaths = transcode.getThumbnailUrls();
            if (thumbnailPaths != null && !thumbnailPaths.isEmpty()) {
                for (String thumbPath : thumbnailPaths) {
                    if (StringUtils.hasText(thumbPath)) {
                        String thumbUrl = generateUrl(thumbPath, ResourceType.THUMBNAIL);
                        if (StringUtils.hasText(thumbUrl)) {
                            thumbnailUrls.add(thumbUrl);
                        }
                    }
                }
            }
            urls.setThumbnailUrls(thumbnailUrls);
            
            // 5. 设置优先播放URL（M3U8优先，MP4备用）
            if (StringUtils.hasText(urls.getM3u8Url())) {
                urls.setPreferredPlayUrl(urls.getM3u8Url());
            } else if (StringUtils.hasText(urls.getVideoUrl())) {
                urls.setPreferredPlayUrl(urls.getVideoUrl());
            }
            
            logger.info("为转码记录{}生成URL完成: video={}, m3u8={}, cover={}, thumbnails={}", 
                transcode.getId(), 
                urls.hasVideoUrl(), 
                urls.hasM3u8Url(), 
                urls.hasCoverUrl(), 
                thumbnailUrls.size());
                
        } catch (Exception e) {
            logger.error("生成转码记录URL失败: transcodeId={}, 错误: {}", 
                transcode.getId(), e.getMessage(), e);
        }
        
        return urls;
    }

    /**
     * 生成单个视频URL - 基于转码记录
     */
    public String generateVideoUrl(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        if (transcode == null) {
            return null;
        }
        
        String videoPath = transcode.getVideoUrl();
        return StringUtils.hasText(videoPath) ? generateUrl(videoPath, ResourceType.VIDEO) : null;
    }

    /**
     * 生成M3U8 URL - 基于转码记录
     */
    public String generateM3u8Url(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        if (transcode == null) {
            return null;
        }
        
        String m3u8Path = transcode.getM3u8Url();
        return StringUtils.hasText(m3u8Path) ? generateUrl(m3u8Path, ResourceType.STREAM) : null;
    }

    /**
     * 生成封面URL - 基于转码记录
     */
    public String generateCoverUrl(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        if (transcode == null) {
            return null;
        }
        
        String coverPath = transcode.getCoverUrl();
        return StringUtils.hasText(coverPath) ? generateUrl(coverPath, ResourceType.COVER) : null;
    }

    /**
     * 根据资源类型选择域名 - 简化为统一域名
     */
    private String selectDomainByType(ResourceType resourceType) {
        return getActiveCdnDomain();
    }

    /**
     * 返回当前请求对应的 CDN 区域标识，用于缓存 key 分区。
     * "CN" = 国内 CDN（腾讯等），"OS" = 国外 CDN（Cloudflare Worker 等）
     * 任何异常均默认返回 "CN"。
     */
    public String getCurrentCdnRegionKey() {
        try {
            return resolveActiveCdnLabel();
        } catch (Exception e) {
            return "CN";
        }
    }

    /**
     * 解析当前请求应使用的 CDN 标签（内部复用）
     * 字典缓存（DictUtils）只缓存 status=0 的条目，故"国外"域名可能缺失；
     * 此时回退到 yml 中 chigua.domains.main（Cloudflare Worker 域名）作为兜底。
     */
    private String resolveActiveCdnLabel() {
        boolean ipFilterEnabled = isIpFilterEnabled();

        List<SysDictData> dictList = sysDictTypeService.selectDictDataByType(CDN_DICT_TYPE);
        if (dictList == null || dictList.isEmpty()) {
            return "CN";
        }

        // 国外域名先用 yml 配置兜底，再被字典覆盖（若字典中有启用的国外条目）
        String overseasDomain = chiguaProperties.getDomains().getMain();
        String activeDomain   = null;
        String activeLabel    = null;

        for (SysDictData dict : dictList) {
            String label = dict.getDictLabel();
            String value = dict.getDictValue();
            if (!StringUtils.hasText(value)) continue;
            if (LABEL_OVERSEAS.equals(label)) {
                overseasDomain = value.trim();
            }
            if ("0".equals(dict.getStatus()) && activeLabel == null) {
                activeDomain = value.trim();
                activeLabel  = label;
            }
        }

        if (activeDomain == null) return "CN";
        if (!ipFilterEnabled)    return LABEL_DOMESTIC.equals(activeLabel) ? "CN" : "OS";
        if (LABEL_OVERSEAS.equals(activeLabel)) return "OS";

        if (LABEL_DOMESTIC.equals(activeLabel) && StringUtils.hasText(overseasDomain)) {
            String clientIp = getRequestClientIp();
            if (clientIp != null && !ipRegionService.isChina(clientIp)) {
                return "OS";
            }
        }
        return "CN";
    }

    /**
     * 获取当前生效的 CDN 域名
     *
     * 逻辑：
     * 1. 读取字典 iplocal，判断是否开启 IP 归属地过滤（启用值=yes 则过滤，否则不过滤）
     * 2. 读取字典 cdntype，找到 status=0(正常) 的那条作为主 CDN
     * 3. 若未开启 IP 过滤 → 直接返回主 CDN
     * 4. 若已开启 IP 过滤：
     *      - 主 CDN 是"国外" → 直接返回，无需 IP 判断
     *      - 主 CDN 是"国内" → 检测请求 IP，国外 IP 自动切换国外 CDN
     * 5. 任何异常/无法判断 → 回退到 yml 配置
     */
    private String getActiveCdnDomain() {
        try {
            // ── 第一步：判断是否开启 IP 过滤 ──────────────────────────
            boolean ipFilterEnabled = isIpFilterEnabled();

            // ── 第二步：读取 CDN 字典（优先 Redis 缓存，缓存为空自动回退 DB 并写回缓存）
            List<SysDictData> dictList = sysDictTypeService.selectDictDataByType(CDN_DICT_TYPE);
            if (dictList == null || dictList.isEmpty()) {
                logger.warn("[CDN路由] cdntype 字典为空，使用 yml 配置");
                return chiguaProperties.getDomains().getMain();
            }

            String domesticDomain = null;
            // 国外域名先用 yml 配置兜底（字典缓存只缓存 status=0 条目，国外可能不在缓存中）
            String overseasDomain = chiguaProperties.getDomains().getMain();
            String activeDomain   = null;
            String activeLabel    = null;

            for (SysDictData dict : dictList) {
                String label = dict.getDictLabel();
                String value = dict.getDictValue();
                if (!StringUtils.hasText(value)) continue;

                if (LABEL_DOMESTIC.equals(label)) {
                    domesticDomain = value.trim();
                } else if (LABEL_OVERSEAS.equals(label)) {
                    overseasDomain = value.trim();
                }
                if ("0".equals(dict.getStatus()) && activeLabel == null) {
                    activeDomain = value.trim();
                    activeLabel  = label;
                }
            }

            if (activeDomain == null) {
                logger.warn("[CDN路由] 无启用的 CDN 条目，使用 yml 配置");
                return chiguaProperties.getDomains().getMain();
            }

            // ── 第三步：未开启 IP 过滤 → 直接返回字典启用的 CDN ────────
            if (!ipFilterEnabled) {
                return activeDomain;
            }

            // ── 第四步：已开启 IP 过滤 ──────────────────────────────────
            // 若启用的是国外 CDN，直接返回，无需 IP 判断
            if (LABEL_OVERSEAS.equals(activeLabel)) {
                return activeDomain;
            }

            // 启用的是国内 CDN：国外访客切换到国外 CDN
            if (LABEL_DOMESTIC.equals(activeLabel) && StringUtils.hasText(overseasDomain)) {
                String clientIp = getRequestClientIp();
                if (clientIp != null && !ipRegionService.isChina(clientIp)) {
                    return overseasDomain;
                }
            }

            return activeDomain;

        } catch (Exception e) {
            logger.warn("[CDN路由] 异常，使用 yml 配置: {}", e.getMessage());
        }
        return chiguaProperties.getDomains().getMain();
    }

    /**
     * 读取字典 iplocal，判断 IP 归属地过滤开关是否为开启状态
     * 启用条目的 dictValue = "yes" → 开启；其他情况（no/无配置）→ 不过滤
     */
    private boolean isIpFilterEnabled() {
        try {
            List<SysDictData> list = sysDictTypeService.selectDictDataByType(IP_FILTER_DICT_TYPE);
            if (list == null) return false;
            for (SysDictData dict : list) {
                if ("0".equals(dict.getStatus())) {
                    return IP_FILTER_YES.equalsIgnoreCase(
                            dict.getDictValue() == null ? "" : dict.getDictValue().trim());
                }
            }
        } catch (Exception e) {
            logger.debug("读取字典 {} 失败，默认不过滤: {}", IP_FILTER_DICT_TYPE, e.getMessage());
        }
        return false;
    }

    /**
     * 从当前 HTTP 请求上下文中获取客户端 IP
     * 在异步线程或非 HTTP 上下文中返回 null
     */
    private String getRequestClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return ipRegionService.getClientIp(request);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建完整URL
     */
    private String buildFullUrl(String domain, String resourcePath) {
        if (!StringUtils.hasText(domain) || !StringUtils.hasText(resourcePath)) {
            return null;
        }
        
        // 🚨 重要修复：如果已经是完整URL，则剥离签名参数后返回（防止重复签名）
        if (resourcePath.startsWith("http://") || resourcePath.startsWith("https://")) {
            String cleaned = stripSignatureParams(resourcePath);
            logger.debug("ResourcePath是完整URL，已剥离签名参数: {}", cleaned);
            return cleaned;
        }
        
        // 确保域名末尾没有斜杠
        String cleanDomain = domain.replaceAll("/$", "");
        
        // 处理资源路径，避免重复的files前缀
        String cleanPath = resourcePath;
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        
        // 检查是否已经包含files前缀，避免重复添加
        if (cleanPath.startsWith("files/")) {
            // 如果已经有files前缀，直接拼接
            return cleanDomain + "/" + cleanPath;
        } else {
            // 如果没有files前缀，添加/files/前缀
            return cleanDomain + "/files/" + cleanPath;
        }
    }

    /**
     * 去除 URL 中的签名相关参数（key/expires/downloads/signature/decrypt）
     * 其余查询参数原样保留，防止重复签名。
     */
    private String stripSignatureParams(String url) {
        if (url == null) return null;
        int qIdx = url.indexOf('?');
        if (qIdx < 0) return url;
        String base = url.substring(0, qIdx);
        String query = url.substring(qIdx + 1);
        if (query.isEmpty()) return base;

        String[] parts = query.split("&");
        java.util.List<String> kept = new java.util.ArrayList<>();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            int eq = p.indexOf('=');
            String name = eq > 0 ? p.substring(0, eq) : p;
            String lname = name.toLowerCase();
            if (lname.equals("key") || lname.equals("expires") || lname.equals("downloads") || lname.equals("signature") || lname.equals("decrypt")) {
                continue; // 跳过签名相关参数
            }
            kept.add(p);
        }
        return kept.isEmpty() ? base : base + "?" + String.join("&", kept);
    }

    /**
     * 添加与 Worker 兼容的URL签名（默认添加解密参数）
     * 使用 HMAC-SHA256 签名格式匹配 Worker 期望
     */
    private String addSignature(String url, ResourceType resourceType) {
        return addSignature(url, resourceType, true);
    }

    /**
     * 添加与 Worker 兼容的URL签名
     * 使用 HMAC-SHA256 签名格式匹配 Worker 期望
     */
    private String addSignature(String url, ResourceType resourceType, boolean addDecryptParam) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        
        try {
            // 根据资源类型选择签名有效期（配置文件中的值是小时，需要转换为秒）
            int expiresHours;
            if (resourceType == ResourceType.IMAGE || 
                resourceType == ResourceType.COVER || 
                resourceType == ResourceType.THUMBNAIL) {
                // 图片类型使用2小时签名有效期
                expiresHours = 2;
            } else if (resourceType == ResourceType.STREAM) {
                // M3U8流媒体使用6小时签名有效期
                expiresHours = 6;
            } else {
                // 其他资源类型使用默认有效期
                expiresHours = chiguaProperties.getUrl().getExpires().getDefaultExpires();
            }
            
            // 计算过期时间（秒级时间戳）
            long expiresTimestamp = System.currentTimeMillis() / 1000 + (expiresHours * 3600);
            
            // 提取资源路径作为key
            String resourceKey = extractResourceKey(url);
            
            // 设置下载次数（与Worker期望格式一致）
            String downloadsStr = getDownloadsStringForResourceType(resourceType);
            
            // 按照Worker格式生成签名数据：{key}:{expires}:{downloads}
            String signData = resourceKey + ":" + expiresTimestamp + ":" + downloadsStr;
            
            // 使用HMAC-SHA256生成签名
            String fullSignature = generateHmacSha256(signData, chiguaProperties.getUrl().getSignatureSecret());
            
            // 只取前16位，与Worker保持一致
            String signature = fullSignature.substring(0, 16);
            
            // 构建Worker格式的查询参数
            // 先剥离已存在的签名相关参数，避免重复
            String baseUrl = stripSignatureParams(url);
            String separator = baseUrl.contains("?") ? "&" : "?";
            String signedUrl = baseUrl + separator + 
                "key=" + resourceKey + 
                "&expires=" + expiresTimestamp + 
                "&downloads=" + downloadsStr + 
                "&signature=" + signature;
            
            // 🔐 根据参数决定是否添加解密参数
            if (addDecryptParam && (resourceType == ResourceType.IMAGE || 
                resourceType == ResourceType.COVER || 
                resourceType == ResourceType.THUMBNAIL)) {
                signedUrl += "&decrypt=true";
            }
            

            
            return signedUrl;
            
        } catch (Exception e) {
            logger.warn("添加URL签名失败: {}, 错误: {}", url, e.getMessage());
            return url; // 签名失败时返回原URL
        }
    }
    
    /**
     * 提取资源路径作为key
     * 重要：确保提取的key与R2存储中的实际key完全一致（不带前导斜杠和files前缀）
     */
    private String extractResourceKey(String url) {
        try {
            String resourceKey;
            
            // 从完整URL中提取路径部分
            if (url.startsWith("http")) {
                resourceKey = new java.net.URL(url).getPath();
            } else {
                // 如果已经是路径，直接使用
                resourceKey = url;
            }
            
            // 去掉前导斜杠
            if (resourceKey.startsWith("/")) {
                resourceKey = resourceKey.substring(1);
            }
            
            // 去掉可能存在的files前缀，避免重复
            if (resourceKey.startsWith("files/")) {
                resourceKey = resourceKey.substring(6);
            }
            
            return resourceKey;
            
        } catch (Exception e) {
            logger.warn("提取资源key失败: {}, 错误: {}", url, e.getMessage());
            // 解析失败时返回去掉前导斜杠的原始字符串
            String fallback = url.startsWith("/") ? url.substring(1) : url;
            // 也要去掉可能的files前缀
            if (fallback.startsWith("files/")) {
                fallback = fallback.substring(6);
            }
            return fallback;
        }
    }
    


    /**
     * 根据资源类型获取下载次数字符串
     * 与Worker保持一致：可能为空字符串
     */
    private String getDownloadsStringForResourceType(ResourceType resourceType) {
        switch (resourceType) {
            case VIDEO:
                return "3";   // 视频文件最多3次
            case STREAM:
                return "";    // M3U8流媒体不限制下载次数（与Worker一致）
            case IMAGE:
            case THUMBNAIL:
            case COVER:
                return "5";   // 图片默认5次
            default:
                return "5";   // 默认5次
        }
    }
    
    /**
     * 生成HMAC-SHA256签名
     */
    private String generateHmacSha256(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
            
            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("生成HMAC-SHA256签名失败: {}", e.getMessage());
            throw new RuntimeException("签名生成失败", e);
        }
    }

    /**
     * 验证URL签名（兼容Worker HMAC格式）
     */
    public boolean verifySignature(String resourceKey, String signature, long expires, String downloads) {
        // 检查是否过期
        if (expires < System.currentTimeMillis() / 1000) {
            return false;
        }
        
        try {
            // 按照Worker格式构建签名数据
            String signData = resourceKey + ":" + expires + ":" + (downloads != null ? downloads : "");
            String fullSignature = generateHmacSha256(signData, chiguaProperties.getUrl().getSignatureSecret());
            String expectedSignature = fullSignature.substring(0, 16);
            
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            logger.warn("验证URL签名失败: resourceKey={}, 错误: {}", resourceKey, e.getMessage());
            return false;
        }
    }



    /**
     * 判断资源是否需要保护
     * 参考pornhub R2SignatureService的shouldProtectResource逻辑
     * 修改：封面图片也需要签名保护，确保与pornhub项目一致
     */
    private boolean shouldProtectResource(String resourcePath) {
        if (!StringUtils.hasText(resourcePath)) {
            return false;
        }
        
        String path = resourcePath.toLowerCase();
        
        // 视频文件需要保护
        if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".avi") || 
            path.endsWith(".mov") || path.endsWith(".m3u8") || path.endsWith(".ts") || 
            path.endsWith(".mkv") || path.endsWith(".flv")) {
            return true;
        }
        
        // 图片文件也需要保护（与pornhub项目保持一致）
        // 参考pornhub R2SignatureService，封面图片和缩略图都需要签名保护
        if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || 
            path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
            path.endsWith(".ico") || path.endsWith(".bmp")) {
            return true; // 修改：图片也需要签名保护
        }
        
        // 默认保护未知类型
        return true;
    }

    /**
     * 为转码记录生成封面图片URL
     * 参考pornhub项目，确保封面图片URL有签名保护
     * 
     * @param transcode 转码记录
     * @return 封面图片URL
     */
    public String generatePosterUrl(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        if (transcode == null) {
            return null;
        }

        try {
            // 优先使用coverImage字段
            if (StringUtils.hasText(transcode.getCoverImage())) {
                String coverPath = buildCoverImagePath(transcode, transcode.getCoverImage());
                // 使用COVER类型，确保有签名保护
                return generateUrl(coverPath, ResourceType.COVER);
            }
            
            // 否则从thumbnails JSON中提取第一个缩略图路径
            if (transcode.getThumbnails() != null) {
                String thumbnailPath = extractFirstThumbnailPath(transcode);
                if (StringUtils.hasText(thumbnailPath)) {
                    // 使用THUMBNAIL类型，确保有签名保护
                    return generateUrl(thumbnailPath, ResourceType.THUMBNAIL);
                }
            }
        } catch (Exception e) {
            logger.error("生成转码封面URL失败: transcodeId={}, 错误: {}", transcode.getTranscodeId(), e.getMessage(), e);
        }
        
        return null;
    }



    /**
     * 构建转码视频的完整路径
     */
    private String buildVideoPath(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        // 根据转码记录构建视频路径
        String rpath = transcode.getRpath();
        String path = transcode.getPath();
        String suffix = transcode.getSuffix();
        
        // 构建完整的视频文件路径
        StringBuilder videoPath = new StringBuilder();
        
        if (StringUtils.hasText(rpath)) {
            if (!rpath.startsWith("/")) {
                videoPath.append("/");
            }
            videoPath.append(rpath);
        }
        
        if (StringUtils.hasText(path)) {
            if (!videoPath.toString().endsWith("/") && !path.startsWith("/")) {
                videoPath.append("/");
            }
            videoPath.append(path);
        }
        
        // 添加文件扩展名
        if (StringUtils.hasText(suffix)) {
            if (!videoPath.toString().endsWith("." + suffix)) {
                videoPath.append(".").append(suffix);
            }
        }
        
        return videoPath.toString();
    }

    /**
     * 从thumbnails JSON中提取第一个缩略图路径
     */
    private String extractFirstThumbnailPath(com.ruoyi.chigua.domain.VideoTranscode transcode) {
        try {
            String thumbnails = transcode.getThumbnails();
            if (!StringUtils.hasText(thumbnails)) {
                return null;
            }

            // 如果是JSON数组格式 ["1.jpg", "2.jpg"]
            if (thumbnails.startsWith("[")) {
                com.alibaba.fastjson2.JSONArray thumbArray = com.alibaba.fastjson2.JSON.parseArray(thumbnails);
                if (thumbArray != null && !thumbArray.isEmpty()) {
                    String firstThumb = thumbArray.getString(0);
                    return buildThumbnailPath(transcode, firstThumb);
                }
            }
            // 如果是JSON对象格式 {"pic1": "/path/1.jpg"}
            else if (thumbnails.startsWith("{")) {
                com.alibaba.fastjson2.JSONObject thumbObj = com.alibaba.fastjson2.JSON.parseObject(thumbnails);
                if (thumbObj != null && thumbObj.containsKey("pic1")) {
                    return thumbObj.getString("pic1");
                }
            }
            // 如果是简单字符串
            else {
                return buildThumbnailPath(transcode, thumbnails);
            }
        } catch (Exception e) {
            logger.warn("解析缩略图路径失败: {}", transcode.getThumbnails(), e);
        }
        
        return null;
    }

    /**
     * 构建缩略图的完整路径
     * 修复：避免rpath和path重复拼接导致路径错误
     */
    private String buildThumbnailPath(com.ruoyi.chigua.domain.VideoTranscode transcode, String thumbFile) {
        if (!StringUtils.hasText(thumbFile)) {
            return null;
        }

        // 如果已经是完整路径，直接返回（去掉前导斜杠）
        if (thumbFile.startsWith("/")) {
            return thumbFile.substring(1);
        }
        if (thumbFile.startsWith("http")) {
            return thumbFile;
        }

        // 重要修复：只使用rpath，不要重复拼接path
        // 根据实际存储结构，rpath已经包含了完整的目录路径
        String rpath = transcode.getRpath();
        
        if (StringUtils.hasText(rpath)) {
            // 确保rpath格式正确（不以斜杠开头和结尾）
            String cleanRpath = rpath;
            if (cleanRpath.startsWith("/")) {
                cleanRpath = cleanRpath.substring(1);
            }
            if (cleanRpath.endsWith("/")) {
                cleanRpath = cleanRpath.substring(0, cleanRpath.length() - 1);
            }
            
            return cleanRpath + "/" + thumbFile;
        }
        
        // 如果没有rpath，直接返回文件名
        return thumbFile;
    }

    /**
     * 构建封面图片路径
     * 修复：确保路径格式与R2存储一致
     */
    private String buildCoverImagePath(com.ruoyi.chigua.domain.VideoTranscode transcode, String coverImage) {
        // 如果封面图片已经是绝对路径，去掉前导斜杠后返回
        if (coverImage.startsWith("/")) {
            return coverImage.substring(1);
        }
        
        // 否则拼接rpath
        if (transcode.getRpath() != null) {
            String cleanRpath = transcode.getRpath();
            if (cleanRpath.startsWith("/")) {
                cleanRpath = cleanRpath.substring(1);
            }
            if (cleanRpath.endsWith("/")) {
                cleanRpath = cleanRpath.substring(0, cleanRpath.length() - 1);
            }
            return cleanRpath + "/" + coverImage;
        }
        
        return coverImage;
    }

    /**
     * 视频URL集合类
     */
    public static class VideoUrls {
        private String videoUrl;        // MP4播放URL
        private String m3u8Url;         // M3U8播放URL
        private String coverUrl;        // 封面图片URL
        private List<String> thumbnailUrls = new ArrayList<>(); // 缩略图URL列表
        private String preferredPlayUrl; // 优先播放URL

        // Getter and Setter
        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getM3u8Url() {
            return m3u8Url;
        }

        public void setM3u8Url(String m3u8Url) {
            this.m3u8Url = m3u8Url;
        }

        public String getCoverUrl() {
            return coverUrl;
        }

        public void setCoverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
        }

        public List<String> getThumbnailUrls() {
            return thumbnailUrls;
        }

        public void setThumbnailUrls(List<String> thumbnailUrls) {
            this.thumbnailUrls = thumbnailUrls;
        }
        
        /**
         * 获取优先播放URL（M3U8优先，其次MP4）
         */
        public String getPreferredPlayUrl() {
            if (preferredPlayUrl != null) {
                return preferredPlayUrl;
            }
            return StringUtils.hasText(m3u8Url) ? m3u8Url : videoUrl;
        }
        
        /**
         * 设置优先播放URL
         */
        public void setPreferredPlayUrl(String preferredPlayUrl) {
            this.preferredPlayUrl = preferredPlayUrl;
        }
        
        /**
         * 检查是否有有效的播放URL
         */
        public boolean hasPlayUrl() {
            return StringUtils.hasText(videoUrl) || StringUtils.hasText(m3u8Url);
        }
        
        /**
         * 检查是否有视频URL
         */
        public boolean hasVideoUrl() {
            return StringUtils.hasText(videoUrl);
        }
        
        /**
         * 检查是否有M3U8 URL
         */
        public boolean hasM3u8Url() {
            return StringUtils.hasText(m3u8Url);
        }
        
        /**
         * 检查是否有封面图片
         */
        public boolean hasCover() {
            return StringUtils.hasText(coverUrl);
        }
        
        /**
         * 检查是否有封面图片 - 别名方法
         */
        public boolean hasCoverUrl() {
            return hasCover();
        }
    }

    // ===== 视频 Key 路径生成（用于 /open/key/{videoId} 接口）=====

    /**
     * 为 videoId 生成 key 访问路径（不含域名，无签名）
     * 返回格式：/open/key/{videoId}
     */
    public String generateKeyPath(Long videoId) {
        return "/open/key/" + videoId;
    }
} 