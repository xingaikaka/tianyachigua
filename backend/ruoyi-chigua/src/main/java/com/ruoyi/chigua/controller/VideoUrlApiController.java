package com.ruoyi.chigua.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.service.ChiguaUrlService.VideoUrls;

/**
 * 视频URL生成API Controller
 * 提供实时URL生成，解决签名URL过期问题
 * 类似pornhub项目的R2SignatureService设计
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@RestController
@RequestMapping("/chigua/video/api")
public class VideoUrlApiController extends BaseController
{
    @Autowired
    private IVideoService videoService;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 获取视频的实时URL信息（带签名）
     * 解决富文本中URL过期的问题
     */
    @GetMapping("/{id}/urls")
    public AjaxResult getVideoUrls(@PathVariable("id") Long id)
    {
        try {
            Video video = videoService.selectVideoById(id);
            if (video == null) {
                return error("视频不存在");
            }

            // 一次性生成所有URL，使用当前时间的签名
            VideoUrls urls = chiguaUrlService.generateVideoUrls(video);
            
            Map<String, Object> result = new HashMap<>();
            result.put("videoId", id);
            result.put("title", video.getTitle());
            result.put("videoUrl", urls.getVideoUrl());
            result.put("m3u8Url", urls.getM3u8Url());
            result.put("coverUrl", urls.getCoverUrl());
            result.put("thumbnailUrls", urls.getThumbnailUrls());
            result.put("preferredPlayUrl", urls.getPreferredPlayUrl());
            result.put("hasPlayUrl", urls.hasPlayUrl());
            result.put("hasCover", urls.hasCover());
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("获取视频URL失败: videoId={}, 错误: {}", id, e.getMessage(), e);
            return error("获取视频URL失败: " + e.getMessage());
        }
    }

    /**
     * 获取视频的完整HTML预览（带当前签名的URL）
     * 用于富文本编辑器预览
     */
    @GetMapping("/{id}/preview")
    public AjaxResult getVideoPreview(@PathVariable("id") Long id)
    {
        try {
            Video video = videoService.selectVideoById(id);
            if (video == null) {
                return error("视频不存在");
            }

            // 生成带当前签名的URL
            VideoUrls urls = chiguaUrlService.generateVideoUrls(video);
            
            // 构建HTML预览内容
            StringBuilder html = new StringBuilder();
            
            // 1. 视频标题
            if (video.getTitle() != null) {
                html.append("<h2>").append(video.getTitle()).append("</h2>\n");
            }
            
            // 2. 视频描述
            if (video.getDescription() != null && !video.getDescription().trim().isEmpty()) {
                html.append("<p>").append(video.getDescription()).append("</p>\n");
            }
            
            // 3. 封面图片（如果有）
            if (urls.hasCover()) {
                html.append("<img src=\"").append(urls.getCoverUrl()).append("\"");
                html.append(" alt=\"").append(video.getTitle() != null ? video.getTitle() + "封面" : "视频封面").append("\"");
                html.append(" style=\"width: 100%; max-width: 800px; height: auto; margin: 20px 0;\">\n");
            }
            
            // 4. 视频播放器
            if (urls.hasPlayUrl()) {
                html.append("<video controls style=\"width: 100%; max-width: 800px; height: auto;\"");
                if (urls.hasCover()) {
                    html.append(" poster=\"").append(urls.getCoverUrl()).append("\"");
                }
                html.append(">\n");
                
                // 优先使用M3U8格式
                if (urls.getM3u8Url() != null) {
                    html.append("    <source src=\"").append(urls.getM3u8Url()).append("\" type=\"application/x-mpegURL\">\n");
                }
                
                // 备用MP4格式
                if (urls.getVideoUrl() != null) {
                    html.append("    <source src=\"").append(urls.getVideoUrl()).append("\" type=\"video/mp4\">\n");
                }
                
                html.append("    您的浏览器不支持视频播放。\n");
                html.append("</video>\n");
            }
            
            // 5. 技术信息
            html.append("<div style=\"margin-top: 20px; padding: 10px; background-color: #f5f5f5; border-radius: 5px;\">\n");
            html.append("<h4>视频信息</h4>\n");
            if (video.getDuration() != null) {
                int minutes = video.getDuration() / 60;
                int seconds = video.getDuration() % 60;
                html.append("<p><strong>时长:</strong> ").append(String.format("%d:%02d", minutes, seconds)).append("</p>\n");
            }
            if (video.getResolution() != null) {
                html.append("<p><strong>分辨率:</strong> ").append(video.getResolution()).append("</p>\n");
            }
            if (video.getFileSize() != null) {
                long mb = video.getFileSize() / (1024 * 1024);
                html.append("<p><strong>文件大小:</strong> ").append(mb).append(" MB</p>\n");
            }
            html.append("</div>\n");
            
            Map<String, Object> result = new HashMap<>();
            result.put("videoId", id);
            result.put("html", html.toString());
            result.put("urls", urls);
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("获取视频预览失败: videoId={}, 错误: {}", id, e.getMessage(), e);
            return error("获取视频预览失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个资源的签名URL
     * 通用接口，支持任意资源路径
     */
    @GetMapping("/resource/url")
    public AjaxResult getResourceUrl(String path, String type)
    {
        try {
            if (path == null || path.trim().isEmpty()) {
                return error("资源路径不能为空");
            }
            
            // 确定资源类型
            ChiguaUrlService.ResourceType resourceType = ChiguaUrlService.ResourceType.IMAGE;
            if ("video".equalsIgnoreCase(type)) {
                resourceType = ChiguaUrlService.ResourceType.VIDEO;
            } else if ("stream".equalsIgnoreCase(type)) {
                resourceType = ChiguaUrlService.ResourceType.STREAM;
            } else if ("cover".equalsIgnoreCase(type)) {
                resourceType = ChiguaUrlService.ResourceType.COVER;
            } else if ("thumbnail".equalsIgnoreCase(type)) {
                resourceType = ChiguaUrlService.ResourceType.THUMBNAIL;
            }
            
            String signedUrl = chiguaUrlService.generateUrl(path.trim(), resourceType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("originalPath", path.trim());
            result.put("signedUrl", signedUrl);
            result.put("resourceType", resourceType.name());
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("生成资源URL失败: path={}, type={}, 错误: {}", path, type, e.getMessage(), e);
            return error("生成资源URL失败: " + e.getMessage());
        }
    }
} 