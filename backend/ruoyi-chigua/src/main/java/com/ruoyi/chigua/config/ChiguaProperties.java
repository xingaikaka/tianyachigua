package com.ruoyi.chigua.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

/**
 * Chigua视频系统配置属性
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@Component
@ConfigurationProperties(prefix = "chigua")
public class ChiguaProperties {

    /**
     * R2存储配置
     */
    private R2Config r2 = new R2Config();

    /**
     * 域名配置
     */
    private DomainConfig domains = new DomainConfig();

    /**
     * URL生成配置
     */
    private UrlConfig url = new UrlConfig();

    /**
     * 资源路径配置
     */
    private PathConfig paths = new PathConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    // Getter and Setter
    public R2Config getR2() {
        return r2;
    }

    public void setR2(R2Config r2) {
        this.r2 = r2;
    }

    public DomainConfig getDomains() {
        return domains;
    }

    public void setDomains(DomainConfig domains) {
        this.domains = domains;
    }

    public UrlConfig getUrl() {
        return url;
    }

    public void setUrl(UrlConfig url) {
        this.url = url;
    }

    public PathConfig getPaths() {
        return paths;
    }

    public void setPaths(PathConfig paths) {
        this.paths = paths;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    /**
     * R2存储配置类
     */
    public static class R2Config {
        private String endpoint = "https://your-account-id.r2.cloudflarestorage.com";
        private String accessKeyId = "your-access-key-id";
        private String secretAccessKey = "your-secret-access-key";
        private String bucketName = "51chigua";
        private ImageEncryptionConfig imageEncryption = new ImageEncryptionConfig();

        // Getter and Setter
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public ImageEncryptionConfig getImageEncryption() {
            return imageEncryption;
        }

        public void setImageEncryption(ImageEncryptionConfig imageEncryption) {
            this.imageEncryption = imageEncryption;
        }
    }

    /**
     * 图片加密配置类
     */
    public static class ImageEncryptionConfig {
        private boolean enabled = true;
        private String key = "cYC8lOMnoUnqzeFhYcGCoLqNa44k9RMfmoorxeS7vIo=";

        // Getter and Setter
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    /**
     * 域名配置类 - 支持主域名和备用域名
     */
    public static class DomainConfig {
        /**
         * 站点主域名（用于站点地图和SEO，不暴露tycg.com）
         * 建议使用 tycg1.com 到 tycg6.com，避免主域名被举报或黑名单
         */
        private String site = "https://tycg8.com";
        
        /**
         * 站点备用域名列表（用于站点地图轮换，分散风险）
         * 格式：tycg1.com,tycg2.com,tycg3.com,tycg4.com,tycg5.com,tycg6.com
         */
        private List<String> siteAlternatives;
        
        /**
         * API域名（用于站点地图API访问，指向泛域名，因为泛域名有Nginx代理到后端Java）
         * 例如：https://search.cqqvl.cc 或 https://api.cqqvl.cc
         * 这个域名必须能通过 /prod-api/ 前缀访问到后端Java服务
         * 注意：已更新为search.cqqvl.cc用于Google Search Console配置
         */
        private String apiDomain;
        
        /**
         * 图片资源域名（R2存储）
         */
        private String main = "https://tycgimage1.org";
        
        /**
         * 备用域名
         */
        private String fallback = "https://74dcb623526e1925a35e0991a0940f16.r2.cloudflarestorage.com";

        // Getter and Setter
        public String getSite() {
            return site;
        }

        public void setSite(String site) {
            this.site = site;
        }

        public List<String> getSiteAlternatives() {
            return siteAlternatives;
        }

        public void setSiteAlternatives(List<String> siteAlternatives) {
            this.siteAlternatives = siteAlternatives;
        }

        public String getApiDomain() {
            return apiDomain;
        }

        public void setApiDomain(String apiDomain) {
            this.apiDomain = apiDomain;
        }

        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }

        public String getFallback() {
            return fallback;
        }

        public void setFallback(String fallback) {
            this.fallback = fallback;
        }
    }

    /**
     * 安全配置
     */
    public static class SecurityConfig {
        /**
         * 后台统计接口访问令牌
         */
        private String statsAdminToken = "change-me";

        public String getStatsAdminToken() {
            return statsAdminToken;
        }

        public void setStatsAdminToken(String statsAdminToken) {
            this.statsAdminToken = statsAdminToken;
        }
    }

    /**
     * URL配置类
     */
    public static class UrlConfig {
        private boolean signatureEnabled = true;
        private String signatureSecret = "chigua-r2-signature-key-2025";
        private boolean fallbackEnabled = true;
        private ExpiresConfig expires = new ExpiresConfig();
        private DownloadLimitsConfig downloadLimits = new DownloadLimitsConfig();
        private FileTypesConfig fileTypes = new FileTypesConfig();

        // Getter and Setter
        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
        }

        public String getSignatureSecret() {
            return signatureSecret;
        }

        public void setSignatureSecret(String signatureSecret) {
            this.signatureSecret = signatureSecret;
        }

        public boolean isFallbackEnabled() {
            return fallbackEnabled;
        }

        public void setFallbackEnabled(boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }

        public ExpiresConfig getExpires() {
            return expires;
        }

        public void setExpires(ExpiresConfig expires) {
            this.expires = expires;
        }

        public DownloadLimitsConfig getDownloadLimits() {
            return downloadLimits;
        }

        public void setDownloadLimits(DownloadLimitsConfig downloadLimits) {
            this.downloadLimits = downloadLimits;
        }

        public FileTypesConfig getFileTypes() {
            return fileTypes;
        }

        public void setFileTypes(FileTypesConfig fileTypes) {
            this.fileTypes = fileTypes;
        }
    }

    /**
     * 过期时间配置类
     */
    public static class ExpiresConfig {
        private int defaultExpires = 3600;  // 默认1小时
        private int shortExpires = 1800;    // 短期30分钟
        private int longExpires = 7200;     // 长期2小时

        // Getter and Setter
        public int getDefaultExpires() {
            return defaultExpires;
        }

        public void setDefaultExpires(int defaultExpires) {
            this.defaultExpires = defaultExpires;
        }

        public int getShortExpires() {
            return shortExpires;
        }

        public void setShortExpires(int shortExpires) {
            this.shortExpires = shortExpires;
        }

        public int getLongExpires() {
            return longExpires;
        }

        public void setLongExpires(int longExpires) {
            this.longExpires = longExpires;
        }
    }

    /**
     * 下载限制配置类
     */
    public static class DownloadLimitsConfig {
        private int video = 3;      // 视频文件最多3次
        private int stream = 10;    // 流媒体最多10次
        private int defaultLimit = 5; // 默认5次

        // Getter and Setter
        public int getVideo() {
            return video;
        }

        public void setVideo(int video) {
            this.video = video;
        }

        public int getStream() {
            return stream;
        }

        public void setStream(int stream) {
            this.stream = stream;
        }

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }
    }

    /**
     * 文件类型配置类
     */
    public static class FileTypesConfig {
        private List<String> protectedTypes = Arrays.asList("mp4", "webm", "avi", "mov", "wmv", "flv", "webm", "mkv", "m3u8", "ts");
        private List<String> publicTypes = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg", "ico", "bmp", "tiff");

        // Getter and Setter
        public List<String> getProtectedTypes() {
            return protectedTypes;
        }

        public void setProtectedTypes(List<String> protectedTypes) {
            this.protectedTypes = protectedTypes;
        }

        public List<String> getPublicTypes() {
            return publicTypes;
        }

        public void setPublicTypes(List<String> publicTypes) {
            this.publicTypes = publicTypes;
        }
    }

    /**
     * 路径配置类
     */
    public static class PathConfig {
        private String videoPrefix = "/videos";
        private String imagePrefix = "/images";
        private String thumbnailPrefix = "/thumbnails";

        // Getter and Setter
        public String getVideoPrefix() {
            return videoPrefix;
        }

        public void setVideoPrefix(String videoPrefix) {
            this.videoPrefix = videoPrefix;
        }

        public String getImagePrefix() {
            return imagePrefix;
        }

        public void setImagePrefix(String imagePrefix) {
            this.imagePrefix = imagePrefix;
        }

        public String getThumbnailPrefix() {
            return thumbnailPrefix;
        }

        public void setThumbnailPrefix(String thumbnailPrefix) {
            this.thumbnailPrefix = thumbnailPrefix;
        }
    }
} 
