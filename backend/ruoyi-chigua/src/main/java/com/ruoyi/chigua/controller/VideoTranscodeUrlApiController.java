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
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.service.ChiguaUrlService.VideoUrls;

/**
 * 视频转码记录URL生成API Controller
 * 提供实时URL生成，兼容pronhub项目的R2SignatureService设计
 * 解决签名URL过期问题
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@RestController
@RequestMapping("/chigua/transcode/api")
public class VideoTranscodeUrlApiController extends BaseController
{
    @Autowired
    private IVideoTranscodeService videoTranscodeService;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 获取转码记录的实时URL信息（带签名）
     * 使用ChiguaUrlService基于数据库字段统一生成URL
     */
    @GetMapping("/{id}/urls")
    public AjaxResult getTranscodeUrls(@PathVariable("id") Long id)
    {
        try {
            VideoTranscode transcode = videoTranscodeService.selectVideoTranscodeById(id);
            if (transcode == null) {
                return error("转码记录不存在");
            }

            // 使用ChiguaUrlService统一生成所有URL
            ChiguaUrlService.VideoUrls urls = chiguaUrlService.generateTranscodeUrls(transcode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("transcodeId", id);
            result.put("title", transcode.getTitle());
            result.put("videoUrl", urls.getVideoUrl());
            result.put("m3u8Url", urls.getM3u8Url());
            result.put("coverUrl", urls.getCoverUrl());
            result.put("thumbnailUrls", urls.getThumbnailUrls());
            result.put("preferredPlayUrl", urls.getPreferredPlayUrl());
            result.put("hasPlayUrl", urls.hasPlayUrl());
            result.put("hasCover", urls.hasCoverUrl());
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("获取转码URL成功: transcodeId={}, hasVideo={}, hasM3u8={}, hasCover={}", 
                id, urls.hasVideoUrl(), urls.hasM3u8Url(), urls.hasCoverUrl());
            
            return success(result);
        } catch (Exception e) {
            logger.error("获取转码URL失败: transcodeId={}, 错误: {}", id, e.getMessage(), e);
            return error("获取转码URL失败: " + e.getMessage());
        }
    }

    /**
     * 获取转码记录的完整HTML预览（带当前签名的URL）
     * 用于富文本编辑器预览，类似pornhub VideoService的processVideoWithSecureUrls
     */
    @GetMapping("/{id}/preview")
    public AjaxResult getTranscodePreview(@PathVariable("id") Long id)
    {
        try {
            VideoTranscode transcode = videoTranscodeService.selectVideoTranscodeById(id);
            if (transcode == null) {
                return error("转码记录不存在");
            }

            // 生成带当前签名的URL
            String videoUrl = chiguaUrlService.generateVideoUrl(transcode);
            String m3u8Url = chiguaUrlService.generateM3u8Url(transcode);
            String coverUrl = chiguaUrlService.generateCoverUrl(transcode);
            
            // 构建HTML预览内容
            StringBuilder html = new StringBuilder();
            
            // 1. 视频标题
            if (transcode.getTitle() != null) {
                html.append("<h2>").append(transcode.getTitle()).append("</h2>\n");
            }
            
            // 2. 视频描述
            if (transcode.getDescription() != null && !transcode.getDescription().trim().isEmpty()) {
                html.append("<p>").append(transcode.getDescription()).append("</p>\n");
            }
            
            // 3. 封面图片（如果有）
            if (coverUrl != null) {
                html.append("<img src=\"").append(coverUrl).append("\"");
                html.append(" alt=\"").append(transcode.getTitle() != null ? transcode.getTitle() + "封面" : "视频封面").append("\"");
                html.append(" style=\"width: 100%; max-width: 800px; height: auto; margin: 20px 0;\">\n");
            }
            
            // 4. 视频播放器
            if (videoUrl != null || m3u8Url != null) {
                html.append("<video controls style=\"width: 100%; max-width: 800px; height: auto;\"");
                
                // 添加封面图片作为poster
                if (coverUrl != null) {
                    html.append(" poster=\"").append(coverUrl).append("\"");
                }
                
                html.append(">\n");
                
                // 优先使用M3U8格式（流媒体）
                if (m3u8Url != null) {
                    html.append("    <source src=\"").append(m3u8Url).append("\" type=\"application/x-mpegURL\">\n");
                }
                
                // 备用MP4格式
                if (videoUrl != null) {
                    html.append("    <source src=\"").append(videoUrl).append("\" type=\"video/mp4\">\n");
                }
                
                html.append("    您的浏览器不支持视频播放。\n");
                html.append("</video>\n");
            }
            
            // 5. 技术信息
            html.append("<div style=\"margin-top: 20px; font-size: 12px; color: #666;\">\n");
            html.append("    <p>分辨率: ").append(transcode.getResolution() != null ? transcode.getResolution() : "未知").append("</p>\n");
            html.append("    <p>时长: ").append(transcode.getDuration() != null ? formatDuration(transcode.getDuration()) : "未知").append("</p>\n");
            html.append("    <p>质量: ").append(transcode.getQuality() != null ? transcode.getQuality().toUpperCase() : "HD").append("</p>\n");
            html.append("</div>\n");
            
            Map<String, Object> result = new HashMap<>();
            result.put("transcodeId", id);
            result.put("html", html.toString());
            result.put("videoUrl", videoUrl);
            result.put("m3u8Url", m3u8Url);
            result.put("coverUrl", coverUrl);
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("获取转码预览失败: transcodeId={}, 错误: {}", id, e.getMessage(), e);
            return error("获取转码预览失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取转码记录的URL信息
     * 类似pornhub VideoService的getBatchVideoUrls方法
     */
    @GetMapping("/batch/urls")
    public AjaxResult getBatchTranscodeUrls(String ids)
    {
        try {
            if (ids == null || ids.trim().isEmpty()) {
                return error("转码记录ID不能为空");
            }
            
            String[] idArray = ids.split(",");
            if (idArray.length > 20) {
                return error("最多支持20个转码记录的批量查询");
            }
            
            Map<String, Object> urlsMap = new HashMap<>();
            
            for (String idStr : idArray) {
                try {
                    Long id = Long.valueOf(idStr.trim());
                    VideoTranscode transcode = videoTranscodeService.selectVideoTranscodeById(id);
                    
                    if (transcode != null) {
                        Map<String, Object> urls = new HashMap<>();
                        urls.put("videoUrl", chiguaUrlService.generateVideoUrl(transcode));
                        urls.put("m3u8Url", chiguaUrlService.generateM3u8Url(transcode));
                        urls.put("coverUrl", chiguaUrlService.generateCoverUrl(transcode));
                        urls.put("title", transcode.getTitle());
                        urls.put("duration", transcode.getDuration());
                        
                        urlsMap.put(idStr.trim(), urls);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("无效的转码记录ID: {}", idStr);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("urls", urlsMap);
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
        } catch (Exception e) {
            logger.error("批量获取转码URL失败: ids={}, 错误: {}", ids, e.getMessage(), e);
            return error("批量获取转码URL失败: " + e.getMessage());
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
            logger.error("获取资源URL失败: path={}, type={}, 错误: {}", path, type, e.getMessage(), e);
            return error("获取资源URL失败: " + e.getMessage());
        }
    }

    /**
     * 验证URL签名是否有效
     * 兼容pornhub R2 Worker格式
     */
    @GetMapping("/verify-signature")
    public AjaxResult verifySignature(String key, String signature, Long expires, String downloads)
    {
        try {
            if (key == null || signature == null || expires == null) {
                return error("缺少必要的验证参数");
            }
            
            boolean isValid = chiguaUrlService.verifySignature(key, signature, expires, downloads != null ? downloads : "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("key", key);
            result.put("expires", expires);
            result.put("downloads", downloads);
            result.put("timestamp", System.currentTimeMillis());
            
            if (!isValid) {
                result.put("reason", expires < System.currentTimeMillis() / 1000 ? "签名已过期" : "签名验证失败");
            }
            
            return success(result);
        } catch (Exception e) {
            logger.error("验证签名失败: key={}, 错误: {}", key, e.getMessage(), e);
            return error("验证签名失败: " + e.getMessage());
        }
    }

    /**
     * 格式化时长（秒转换为 HH:MM:SS 格式）
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
} 