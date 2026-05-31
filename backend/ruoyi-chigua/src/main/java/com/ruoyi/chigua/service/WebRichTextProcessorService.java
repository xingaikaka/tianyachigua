package com.ruoyi.chigua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Web前端富文本处理服务
 * 专门用于Web前端，图片URL不包含解密参数，由前端处理解密
 * 
 * @author chigua
 * @date 2025-01-22
 */
@Service
public class WebRichTextProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(WebRichTextProcessorService.class);

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    // data-resource-key模式：data-resource-key="path"
    private static final Pattern DATA_RESOURCE_PATTERN = Pattern.compile(
        "data-resource-key=\"([^\"]+)\""
    );

    /**
     * 处理Web前端富文本内容，将资源路径替换为带签名的URL
     * 图片URL不包含解密参数，由前端处理解密
     * 
     * @param richTextContent 原始富文本内容
     * @return 处理后的富文本内容（包含签名URL）
     */
    /**
     * ⚠️ 严格"方案 B"：移除 @Cacheable，永不缓存"含 CDN 域名的处理结果"，
     * 每次请求出口处都现场调用 chiguaUrlService.generateUrl(...)，由其内部按
     * 当前请求 IP 选择 CDN 域名，天然按 region 区分。
     */
    public String processRichTextForWebDisplay(String richTextContent) {
        if (!StringUtils.hasText(richTextContent)) {
            logger.debug("📝 富文本内容为空，直接返回");
            return richTextContent;
        }

        try {
            logger.info("🔄 开始处理Web前端富文本内容，原始长度: {}", richTextContent.length());
            
            // 检查内容中是否包含需要处理的元素
            boolean hasDataResourceKey = richTextContent.contains("data-resource-key");
            boolean hasRelativePaths = hasRelativePathSources(richTextContent);
            
            logger.info("📋 Web内容分析: data-resource-key={}, relative-paths={}", 
                hasDataResourceKey, hasRelativePaths);
            
            if (!hasDataResourceKey && !hasRelativePaths) {
                logger.info("✅ Web内容无需处理，直接返回");
                return richTextContent;
            }

            String processedContent = richTextContent;

            // 1. 处理data-resource-key模式（图片不带decrypt参数）
            if (hasDataResourceKey) {
                logger.info("🔧 处理Web前端data-resource-key模式...");
                processedContent = processWebDataResourceKeys(processedContent);
            }

            // 2. 处理相对路径（图片不带decrypt参数）
            if (hasRelativePaths) {
                logger.info("🔧 处理Web前端相对路径...");
                processedContent = processWebRelativePaths(processedContent);
            }

            logger.info("✅ Web前端富文本处理完成，处理后长度: {}", processedContent.length());
            return processedContent;
            
        } catch (Exception e) {
            logger.error("❌ Web前端富文本处理失败", e);
            return richTextContent; // 降级返回原始内容
        }
    }

    /**
     * 处理后台管理编辑器用的富文本：图片等资源拼接&decrypt=true
     */
    public String processRichTextForAdminEditor(String richTextContent) {
        if (!StringUtils.hasText(richTextContent)) {
            return richTextContent;
        }

        try {
            String processedContent = richTextContent;

            boolean hasDataResourceKey = richTextContent.contains("data-resource-key");
            boolean hasRelativePaths = hasRelativePathSources(richTextContent);

            // 管理端始终使用 Worker（tycgimage1.org），不受系统 CDN 配置影响
            if (hasDataResourceKey) {
                processedContent = processAdminDataResourceKeys(processedContent);
            }
            if (hasRelativePaths) {
                processedContent = processAdminImageRelativePaths(processedContent);
            }
            return processedContent;
        } catch (Exception e) {
            logger.error("❌ 管理端富文本处理失败", e);
            return richTextContent;
        }
    }

    /**
     * 管理端专用：处理 data-resource-key，使用 Worker URL（含 decrypt=true）
     */
    private String processAdminDataResourceKeys(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );

        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                String signedUrl = chiguaUrlService.generateWorkerUrl(resourcePath, ChiguaUrlService.ResourceType.IMAGE);

                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );

                matcher.appendReplacement(result, Matcher.quoteReplacement("<img" + newAttributes + ">"));
                logger.info("🔄 Admin img Worker URL: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("Admin img Worker URL 处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 管理端专用：处理相对路径图片，使用 Worker URL（含 decrypt=true）
     */
    private String processAdminImageRelativePaths(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String srcValue = matcher.group(2);

            try {
                String finalUrl = srcValue;

                if (!srcValue.startsWith("http://") && !srcValue.startsWith("https://") && !srcValue.startsWith("blob:")) {
                    // 相对路径：生成 Worker 签名 URL
                    finalUrl = chiguaUrlService.generateWorkerUrl(srcValue, ChiguaUrlService.ResourceType.IMAGE);
                    logger.info("🔄 Admin img 相对路径转 Worker URL: {} -> {}", srcValue, finalUrl);
                } else if (srcValue.contains("signature=") && !srcValue.contains("decrypt=true")) {
                    // 已有签名但无 decrypt：追加 decrypt=true
                    String separator = srcValue.contains("?") ? "&" : "?";
                    finalUrl = srcValue + separator + "decrypt=true";
                }

                String newAttributes = imgAttributes.replace(
                    "src=\"" + srcValue + "\"",
                    "src=\"" + finalUrl + "\""
                ).replace(
                    "src='" + srcValue + "'",
                    "src='" + finalUrl + "'"
                );
                matcher.appendReplacement(result, Matcher.quoteReplacement("<img" + newAttributes + ">"));
            } catch (Exception e) {
                logger.warn("Admin img 路径处理失败: {}", e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 检查是否有相对路径的资源
     */
    private boolean hasRelativePathSources(String content) {
        // 检查img标签的src属性
        Pattern imgPattern = Pattern.compile("<img[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher imgMatcher = imgPattern.matcher(content);
        while (imgMatcher.find()) {
            String src = imgMatcher.group(1);
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                return true;
            }
        }

        // 检查video标签的poster属性
        Pattern posterPattern = Pattern.compile("<video[^>]*poster\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher posterMatcher = posterPattern.matcher(content);
        while (posterMatcher.find()) {
            String poster = posterMatcher.group(1);
            if (!poster.startsWith("http://") && !poster.startsWith("https://") && !poster.startsWith("blob:")) {
                return true;
            }
        }

        // 检查source标签的src属性
        Pattern sourcePattern = Pattern.compile("<source[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher sourceMatcher = sourcePattern.matcher(content);
        while (sourceMatcher.find()) {
            String src = sourceMatcher.group(1);
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理Web前端data-resource-key模式：data-resource-key="path" -> src="signed_url"
     */
    private String processWebDataResourceKeys(String content) {
        // 1. 处理img标签的data-resource-key属性
        content = processWebImageResourceKeys(content);
        
        // 2. 处理video标签的poster属性
        content = processWebVideoPosterKeys(content);
        
        // 3. 处理source标签的src属性
        content = processWebSourceSrcKeys(content);
        
        return content;
    }

    /**
     * 处理Web前端img标签的data-resource-key属性
     */
    private String processWebImageResourceKeys(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                // 图片资源不添加解密参数（Web端）
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, false);
                
                // 替换data-resource-key为src属性
                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                logger.info("🔄 Web img src转换: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("Web img src处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 管理端：data-resource-key，图片添加decrypt=true
     */
    private String processWebDataResourceKeysWithDecrypt(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );

        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, true);

                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );

                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                logger.info("🔄 Admin img src转换(decrypt): {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("Admin img src处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理Web前端video标签的poster属性
     */
    private String processWebVideoPosterKeys(String content) {
        Pattern videoPattern = Pattern.compile(
            "<video([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        Matcher matcher = videoPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String videoAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);
            
            try {
                // 检查是否为图片资源（poster）
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                if (resourceType == ChiguaUrlService.ResourceType.COVER || 
                    resourceType == ChiguaUrlService.ResourceType.THUMBNAIL ||
                    resourceType == ChiguaUrlService.ResourceType.IMAGE) {
                    
                    // 封面图不添加解密参数（Web端）
                    String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, false);
                    
                    // 替换data-resource-key为poster属性
                    String newAttributes = videoAttributes.replace(
                        "data-resource-key=\"" + resourcePath + "\"",
                        "poster=\"" + signedUrl + "\""
                    );
                    
                    String replacement = "<video" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 Web video poster转换: {} -> {}", resourcePath, signedUrl);
                } else {
                    // 不是poster，保留原样
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } catch (Exception e) {
                logger.warn("Web video poster处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 管理端：video poster 添加decrypt=true
     */
    private String processWebVideoPosterKeysWithDecrypt(String content) {
        Pattern videoPattern = Pattern.compile(
            "<video([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );

        Matcher matcher = videoPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String videoAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);
            
            try {
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                if (resourceType == ChiguaUrlService.ResourceType.COVER || 
                    resourceType == ChiguaUrlService.ResourceType.THUMBNAIL ||
                    resourceType == ChiguaUrlService.ResourceType.IMAGE) {
                    
                    String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, true);
                    
                    String newAttributes = videoAttributes.replace(
                        "data-resource-key=\"" + resourcePath + "\"",
                        "poster=\"" + signedUrl + "\""
                    );
                    
                    String replacement = "<video" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 Admin video poster转换(decrypt): {} -> {}", resourcePath, signedUrl);
                } else {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } catch (Exception e) {
                logger.warn("Admin video poster处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理Web前端source标签的src属性
     */
    private String processWebSourceSrcKeys(String content) {
        Pattern sourcePattern = Pattern.compile(
            "<source([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        Matcher matcher = sourcePattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String sourceAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                // 根据文件扩展名判断资源类型
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, true); // 视频保持原有逻辑
                
                // 替换data-resource-key为src属性
                String newAttributes = sourceAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<source" + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                logger.info("🔄 Web source src转换: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("Web source src处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理Web前端相对路径
     */
    private String processWebRelativePaths(String content) {
        // 1. 处理img标签的相对路径
        content = processWebImageRelativePaths(content);
        
        // 2. 处理video标签的poster相对路径
        content = processWebVideoPosterRelativePaths(content);
        
        // 3. 处理source标签的相对路径
        content = processWebSourceRelativePaths(content);
        
        return content;
    }

    /**
     * 处理Web前端img标签的相对路径
     */
    private String processWebImageRelativePaths(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String srcValue = matcher.group(2);

            // 只处理相对路径，跳过完整URL
            if (!srcValue.startsWith("http://") && !srcValue.startsWith("https://") && !srcValue.startsWith("blob:")) {
                try {
                    // 根据文件扩展名判断资源类型（Web端图片不添加解密参数）
                    ChiguaUrlService.ResourceType resourceType = detectResourceType(srcValue);
                    String signedUrl = chiguaUrlService.generateUrl(srcValue, resourceType, false);
                    
                    // 替换src属性值
                    String newAttributes = imgAttributes.replace(
                        "src=\"" + srcValue + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + srcValue + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<img" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 Web img相对路径转换: {} -> {}", srcValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("Web img相对路径处理失败: {}, 错误: {}", srcValue, e.getMessage());
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } else {
                // 不是相对路径，保留原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 管理端：img相对路径添加decrypt=true
     * 🔧 修复：同时处理相对路径和已包含签名但缺少decrypt=true的完整URL
     */
    private String processWebImageRelativePathsWithDecrypt(String content) {
        Pattern imgPattern = Pattern.compile(
            "<img([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String srcValue = matcher.group(2);
            
            try {
                String finalUrl = srcValue;
                
                // 1. 处理相对路径：生成签名URL并添加decrypt=true
                if (!srcValue.startsWith("http://") && !srcValue.startsWith("https://") && !srcValue.startsWith("blob:")) {
                    ChiguaUrlService.ResourceType resourceType = detectResourceType(srcValue);
                    finalUrl = chiguaUrlService.generateUrl(srcValue, resourceType, true);
                    logger.info("🔄 Admin img相对路径转换(decrypt): {} -> {}", srcValue, finalUrl);
                } 
                // 2. 处理完整URL：如果已有签名但缺少decrypt=true，则添加
                else if (srcValue.contains("signature=") && !srcValue.contains("decrypt=true")) {
                    // 已有签名但缺少decrypt参数，添加decrypt=true
                    String separator = srcValue.contains("?") ? "&" : "?";
                    finalUrl = srcValue + separator + "decrypt=true";
                    logger.info("🔄 Admin img完整URL添加decrypt参数: {} -> {}", srcValue, finalUrl);
                }
                // 3. 如果URL已经包含decrypt=true，保持不变
                
                String newAttributes = imgAttributes.replace(
                    "src=\"" + srcValue + "\"",
                    "src=\"" + finalUrl + "\""
                ).replace(
                    "src='" + srcValue + "'",
                    "src='" + finalUrl + "'"
                );
                
                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                logger.warn("Admin img处理失败: {}, 错误: {}", srcValue, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 处理Web前端video标签的poster相对路径
     */
    private String processWebVideoPosterRelativePaths(String content) {
        Pattern videoPattern = Pattern.compile(
            "<video([^>]*poster\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = videoPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String videoAttributes = matcher.group(1);
            String posterValue = matcher.group(2);

            // 只处理相对路径，跳过完整URL
            if (!posterValue.startsWith("http://") && !posterValue.startsWith("https://") && !posterValue.startsWith("blob:")) {
                try {
                    // 管理端：封面图添加解密参数
                    String signedUrl = chiguaUrlService.generateUrl(posterValue, ChiguaUrlService.ResourceType.COVER, true);
                    
                    // 替换poster属性值
                    String newAttributes = videoAttributes.replace(
                        "poster=\"" + posterValue + "\"",
                        "poster=\"" + signedUrl + "\""
                    ).replace(
                        "poster='" + posterValue + "'",
                        "poster='" + signedUrl + "'"
                    );
                    
                    String replacement = "<video" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 Web video poster相对路径转换: {} -> {}", posterValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("Web video poster相对路径处理失败: {}, 错误: {}", posterValue, e.getMessage());
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } else {
                // 不是相对路径，保留原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 管理端：source相对路径沿用原逻辑（视频保持true）
     */
    private String processWebRelativePathsWithDecrypt(String content) {
        content = processWebImageRelativePathsWithDecrypt(content);
        content = processWebVideoPosterRelativePaths(content); // 已改为true
        content = processWebSourceRelativePaths(content);      // 视频原本就是true
        return content;
    }

    /**
     * 处理Web前端source标签的相对路径
     */
    private String processWebSourceRelativePaths(String content) {
        Pattern sourcePattern = Pattern.compile(
            "<source([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = sourcePattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String sourceAttributes = matcher.group(1);
            String srcValue = matcher.group(2);

            // 只处理相对路径，跳过完整URL
            if (!srcValue.startsWith("http://") && !srcValue.startsWith("https://") && !srcValue.startsWith("blob:")) {
                try {
                    // 根据文件扩展名判断资源类型
                    ChiguaUrlService.ResourceType resourceType = detectResourceType(srcValue);
                    String signedUrl = chiguaUrlService.generateUrl(srcValue, resourceType, true); // 视频保持原有逻辑
                    
                    // 替换src属性值
                    String newAttributes = sourceAttributes.replace(
                        "src=\"" + srcValue + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + srcValue + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<source" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 Web source相对路径转换: {} -> {}", srcValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("Web source相对路径处理失败: {}, 错误: {}", srcValue, e.getMessage());
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } else {
                // 不是相对路径，保留原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 根据文件路径检测资源类型
     */
    private ChiguaUrlService.ResourceType detectResourceType(String resourcePath) {
        if (resourcePath == null) {
            return ChiguaUrlService.ResourceType.IMAGE;
        }

        String path = resourcePath.toLowerCase();
        
        if (path.endsWith(".m3u8")) {
            return ChiguaUrlService.ResourceType.STREAM;
        } else if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".avi")) {
            return ChiguaUrlService.ResourceType.VIDEO;
        } else if (path.contains("cover") || path.contains("poster")) {
            return ChiguaUrlService.ResourceType.COVER;
        } else if (path.contains("thumb") || path.contains("thumbnail")) {
            return ChiguaUrlService.ResourceType.THUMBNAIL;
        } else {
            return ChiguaUrlService.ResourceType.IMAGE;
        }
    }

    /**
     * 生成图片签名URL的公共方法
     * 供其他服务调用，用于实时生成新鲜的签名URL
     */
    public String generateImageUrl(String resourcePath) {
        try {
            ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
            // 图片不添加解密参数，由前端处理
            return chiguaUrlService.generateUrl(resourcePath, resourceType, false);
        } catch (Exception e) {
            logger.error("❌ 生成图片签名URL失败: {}", resourcePath, e);
            throw new RuntimeException("生成图片签名URL失败: " + e.getMessage());
        }
    }
} 