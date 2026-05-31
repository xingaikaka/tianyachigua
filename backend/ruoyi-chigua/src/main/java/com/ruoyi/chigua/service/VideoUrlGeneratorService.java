package com.ruoyi.chigua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.service.ChiguaUrlService.ResourceType;
import com.ruoyi.chigua.service.ChiguaUrlService.VideoUrls;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频URL生成服务
 * 更新为使用ChiguaUrlService的配置化域名架构
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@Service
public class VideoUrlGeneratorService 
{
    private static final Logger logger = LoggerFactory.getLogger(VideoUrlGeneratorService.class);
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    // 保持向后兼容的配置
    @Value("${ppvod.default.domain:https://chigua-r2-worker.xingaikaka.workers.dev}")
    private String defaultDomain;

    @Value("${ppvod.default.pic-domain:https://chigua-r2-worker.xingaikaka.workers.dev}")
    private String defaultPicDomain;

    /**
     * 生成视频播放URL (MP4) - 使用新的配置化架构
     * 
     * @param video 视频对象
     * @return MP4播放URL
     */
    public String generateVideoUrl(Video video) {
        if (video == null) {
            return null;
        }
        
        // 使用Video模型的访问器获取资源路径
        String resourcePath = video.getVideoUrl();
        return chiguaUrlService.generateUrl(resourcePath, ResourceType.VIDEO);
    }

    /**
     * 生成M3U8播放URL (HLS) - 使用新的配置化架构
     * 
     * @param video 视频对象
     * @return M3U8播放URL
     */
    public String generateM3u8Url(Video video) {
        if (video == null) {
            return null;
        }
        
        // 使用Video模型的访问器获取资源路径
        String resourcePath = video.getM3u8Url();
        return chiguaUrlService.generateUrl(resourcePath, ResourceType.STREAM);
    }

    /**
     * 生成封面图片URL - 使用新的配置化架构
     * 
     * @param video 视频对象
     * @return 封面图片URL（默认带decrypt=true，给后端管理使用）
     */
    public String generateCoverUrl(Video video) {
        if (video == null) {
            return null;
        }
        String resourcePath = video.getCoverImage();
        return chiguaUrlService.generateUrl(resourcePath, ResourceType.COVER);
    }

    /**
     * 生成封面图片 Worker URL（管理后台专用，始终走 tycgimage1.org）
     * Worker 负责服务端解密，admin 前端直接显示无需客户端解密。
     *
     * @param video 视频对象
     * @return Worker 签名封面URL
     */
    public String generateWorkerCoverUrl(Video video) {
        if (video == null) return null;
        String resourcePath = video.getCoverImage();
        return chiguaUrlService.generateWorkerUrl(resourcePath, ResourceType.COVER);
    }

    /**
     * 生成封面图片URL（前端专用，不带decrypt=true）
     * 前端会使用JavaScript解密，CDN可以缓存加密数据
     * 
     * @param video 视频对象
     * @return 封面图片URL（不带decrypt=true）
     */
    public String generateCoverUrlForWeb(Video video) {
        if (video == null) {
            return null;
        }
        String resourcePath = video.getCoverImage();
        return chiguaUrlService.generateUrl(resourcePath, ResourceType.COVER, false);
    }

    /**
     * 生成缩略图URL列表 - 使用新的配置化架构
     * 
     * @param video 视频对象
     * @return 缩略图URL列表
     */
    public List<String> getThumbnailUrls(Video video) {
        List<String> urls = new ArrayList<>();
        
        if (video == null) {
            return urls;
        }
        
        // 使用Video模型的访问器获取资源路径列表
        List<String> resourcePaths = video.getThumbnailUrls();
        for (String resourcePath : resourcePaths) {
            String url = chiguaUrlService.generateUrl(resourcePath, ResourceType.THUMBNAIL);
            if (url != null) {
                urls.add(url);
            }
        }
        
        return urls;
    }

    /**
     * 生成简单的富文本内容 (标题+描述+图片+播放器)
     * 使用新的配置化URL生成架构，一次性生成所有URL提升性能
     * 
     * @param video 视频对象
     * @return 富文本HTML内容
     */
    public String generateSimpleRichTextContent(Video video) {
        if (video == null) {
            return "";
        }
        
        // 一次性生成所有相关URL
        VideoUrls urls = chiguaUrlService.generateVideoUrls(video);
        
        StringBuilder html = new StringBuilder();
        
        // 1. 标题
        if (video.getTitle() != null) {
            html.append("<h1>").append(video.getTitle()).append("</h1>\n");
        }
        
        // 2. 描述
        if (video.getDescription() != null && !video.getDescription().trim().isEmpty()) {
            html.append("<p>").append(video.getDescription()).append("</p>\n");
        }
        
        // 3. 封面图片 - 使用配置化域名生成的URL
        if (urls.hasCover()) {
            html.append("<img src=\"").append(urls.getCoverUrl()).append("\"");
            html.append(" alt=\"").append(video.getTitle() != null ? video.getTitle() + "封面" : "视频封面").append("\"");
            html.append(" style=\"width: 100%; max-width: 800px; height: auto; margin: 20px 0;\">\n");
        }
        
        // 4. 视频播放器 - 使用配置化域名生成的URL
        if (urls.hasPlayUrl()) {
            html.append("<video controls style=\"width: 100%; max-width: 800px; height: auto;\">\n");
            
            // 优先使用M3U8格式（流媒体域名）
            if (urls.getM3u8Url() != null) {
                html.append("    <source src=\"").append(urls.getM3u8Url()).append("\" type=\"application/x-mpegURL\">\n");
            }
            
            // 备用MP4格式（视频域名）
            if (urls.getVideoUrl() != null) {
                html.append("    <source src=\"").append(urls.getVideoUrl()).append("\" type=\"video/mp4\">\n");
            }
            
            html.append("    您的浏览器不支持视频播放。\n");
            html.append("</video>\n");
        }
        
        return html.toString();
    }

    /**
     * 检查视频URL是否可用
     * 
     * @param url 视频URL
     * @return 是否可用
     */
    public boolean isVideoUrlAvailable(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // 这里可以实现真实的URL检查逻辑
        // 比如发送HEAD请求检查资源是否存在
        // 为了演示，简单检查URL格式
        return url.startsWith("http") && (url.contains(".mp4") || url.contains(".m3u8"));
    }

    /**
     * 生成视频播放器HTML代码
     * 
     * @param video 视频对象
     * @return HTML代码
     */
    public String generateVideoPlayerHtml(Video video) {
        if (video == null) {
            return "";
        }
        
        String videoUrl = generateVideoUrl(video);
        String m3u8Url = generateM3u8Url(video);
        String coverUrl = generateCoverUrl(video);
        
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"video-player-container\">");
        html.append("<video controls width=\"100%\" height=\"400\"");
        
        if (coverUrl != null) {
            html.append(" poster=\"").append(coverUrl).append("\"");
        }
        
        html.append(">");
        
        // 优先使用HLS格式
        if (m3u8Url != null) {
            html.append("<source src=\"").append(m3u8Url).append("\" type=\"application/x-mpegURL\">");
        }
        
        // 备用MP4格式
        if (videoUrl != null) {
            html.append("<source src=\"").append(videoUrl).append("\" type=\"video/mp4\">");
        }
        
        html.append("您的浏览器不支持视频播放。");
        html.append("</video>");
        html.append("</div>");
        
        return html.toString();
    }

    /**
     * 生成图片画廊HTML代码
     * 
     * @param video 视频对象
     * @return HTML代码
     */
    public String generateImageGalleryHtml(Video video) {
        List<String> thumbnails = getThumbnailUrls(video);
        if (thumbnails.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"image-gallery\">");
        html.append("<div class=\"gallery-grid\">");
        
        for (String url : thumbnails) {
            html.append("<div class=\"gallery-item\">");
            html.append("<img src=\"").append(url).append("\" alt=\"视频截图\" loading=\"lazy\">");
            html.append("</div>");
        }
        
        html.append("</div>");
        html.append("</div>");
        
        return html.toString();
    }

    /**
     * 生成技术信息HTML代码
     * 
     * @param video 视频对象
     * @return HTML代码
     */
    public String generateTechnicalInfoHtml(Video video) {
        if (video == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"technical-info\">");
        html.append("<h3>📊 技术参数</h3>");
        html.append("<div class=\"info-grid\">");
        
        if (video.getResolution() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">分辨率:</span> ").append(video.getResolution()).append("</div>");
        }
        
        if (video.getQuality() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">质量:</span> ").append(video.getQuality()).append("</div>");
        }
        
        if (video.getDuration() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">时长:</span> ").append(formatDuration(video.getDuration())).append("</div>");
        }
        
        if (video.getFileSize() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">文件大小:</span> ").append(formatFileSize(video.getFileSize())).append("</div>");
        }
        
        if (video.getBitrate() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">码率:</span> ").append(video.getBitrate()).append(" kbps</div>");
        }
        
        if (video.getFps() != null) {
            html.append("<div class=\"info-item\"><span class=\"label\">帧率:</span> ").append(video.getFps()).append(" fps</div>");
        }
        
        html.append("</div>");
        html.append("</div>");
        
        return html.toString();
    }

    /**
     * 格式化时长显示
     * 
     * @param seconds 秒数
     * @return 格式化的时长字符串
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "未知";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d小时%d分钟%d秒", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, secs);
        } else {
            return String.format("%d秒", secs);
        }
    }

    /**
     * 格式化文件大小显示
     * 
     * @param bytes 字节数
     * @return 格式化的文件大小字符串
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "未知";
        }
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
} 