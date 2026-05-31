package com.ruoyi.chigua.controller;

import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chigua 资源URL生成控制器
 * 提供资源URL生成服务，支持图片、视频、封面等资源类型
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@RestController
@RequestMapping("/chigua/resource")
public class ChiguaResourceController extends BaseController {

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 生成单个资源URL
     * 
     * @param resourceKey 资源键名
     * @param resourceType 资源类型 (image, video, cover, thumbnail)
     * @return 包含签名URL的响应
     */
    @PostMapping("/url")
    public AjaxResult generateResourceUrl(@RequestBody Map<String, String> request) {
        try {
            String resourceKey = request.get("resourceKey");
            String resourceType = request.get("resourceType");
            
            if (resourceKey == null || resourceKey.trim().isEmpty()) {
                return error("资源键名不能为空");
            }
            
            // 确定资源类型
            ChiguaUrlService.ResourceType type = parseResourceType(resourceType);
            
            // 生成签名URL
            String signedUrl = chiguaUrlService.generateUrl(resourceKey.trim(), type);
            
            if (signedUrl == null) {
                return error("生成资源URL失败");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("resourceKey", resourceKey.trim());
            result.put("signedUrl", signedUrl);
            result.put("resourceType", type.name());
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("生成资源URL失败: {}", e.getMessage(), e);
            return error("生成资源URL失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成资源URL
     * 
     * @param resourceKeys 资源键名列表
     * @param resourceType 资源类型
     * @return 包含多个签名URL的响应
     */
    @PostMapping("/urls/batch")
    public AjaxResult generateBatchResourceUrls(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> resourceKeys = (List<String>) request.get("resourceKeys");
            String resourceType = (String) request.get("resourceType");
            
            if (resourceKeys == null || resourceKeys.isEmpty()) {
                return error("资源键名列表不能为空");
            }
            
            // 确定资源类型
            ChiguaUrlService.ResourceType type = parseResourceType(resourceType);
            
            Map<String, String> urlMap = new HashMap<>();
            
            for (String resourceKey : resourceKeys) {
                if (resourceKey != null && !resourceKey.trim().isEmpty()) {
                    String signedUrl = chiguaUrlService.generateUrl(resourceKey.trim(), type);
                    if (signedUrl != null) {
                        urlMap.put(resourceKey.trim(), signedUrl);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("urls", urlMap);
            result.put("resourceType", type.name());
            result.put("count", urlMap.size());
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("批量生成资源URL失败: {}", e.getMessage(), e);
            return error("批量生成资源URL失败: " + e.getMessage());
        }
    }

    /**
     * 验证资源URL是否有效
     * 
     * @param url 要验证的URL
     * @return 验证结果
     */
    @PostMapping("/validate")
    public AjaxResult validateResourceUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            
            if (url == null || url.trim().isEmpty()) {
                return error("URL不能为空");
            }
            
            // 简单的URL格式验证
            boolean isValid = url.startsWith("http") && 
                             (url.contains("signature=") || url.contains("token="));
            
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("valid", isValid);
            result.put("timestamp", System.currentTimeMillis());
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("验证资源URL失败: {}", e.getMessage(), e);
            return error("验证资源URL失败: " + e.getMessage());
        }
    }

    /**
     * 解析资源类型字符串为枚举值
     */
    private ChiguaUrlService.ResourceType parseResourceType(String resourceType) {
        if (resourceType == null) {
            return ChiguaUrlService.ResourceType.IMAGE;
        }
        
        switch (resourceType.toLowerCase()) {
            case "video":
                return ChiguaUrlService.ResourceType.VIDEO;
            case "stream":
            case "m3u8":
                return ChiguaUrlService.ResourceType.STREAM;
            case "cover":
                return ChiguaUrlService.ResourceType.COVER;
            case "thumbnail":
            case "thumb":
                return ChiguaUrlService.ResourceType.THUMBNAIL;
            case "image":
            default:
                return ChiguaUrlService.ResourceType.IMAGE;
        }
    }
} 