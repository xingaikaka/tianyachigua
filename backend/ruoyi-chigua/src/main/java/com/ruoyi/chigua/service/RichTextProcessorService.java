package com.ruoyi.chigua.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 富文本处理服务
 * 负责处理富文本中的资源占位符，动态生成签名URL
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
@Service
public class RichTextProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(RichTextProcessorService.class);

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    // 占位符模式：{{RESOURCE_TYPE:RESOURCE_ID:RESOURCE_PATH}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
        "\\{\\{(VIDEO|M3U8|COVER|THUMBNAIL):(\\d+):([^}]+)\\}\\}"
    );

    // data-resource-key模式：data-resource-key="path"
    private static final Pattern DATA_RESOURCE_PATTERN = Pattern.compile(
        "data-resource-key=\"([^\"]+)\""
    );

    /**
     * 处理富文本内容，将占位符替换为带签名的URL（使用系统参数CDN，供 chigua-web 调用）
     */
    public String processRichTextForDisplay(String richTextContent) {
        return processRichTextInternal(richTextContent, false);
    }

    /**
     * 处理富文本内容，将占位符替换为带签名的 Worker URL（管理后台专用）
     * 始终使用 tycgimage1.org，忽略系统参数中的 CDN 配置。
     */
    public String processRichTextForAdmin(String richTextContent) {
        return processRichTextInternal(richTextContent, true);
    }

    private String processRichTextInternal(String richTextContent, boolean useWorker) {
        if (!StringUtils.hasText(richTextContent)) {
            logger.debug("📝 富文本内容为空，直接返回");
            return richTextContent;
        }

        try {
            logger.info("🔄 开始处理富文本内容，原始长度: {}, useWorker={}", richTextContent.length(), useWorker);
            
            // 检查内容中是否包含需要处理的元素
            boolean hasDataResourceKey = richTextContent.contains("data-resource-key");
            boolean hasPlaceholders = richTextContent.contains("{{");
            boolean hasRelativePaths = hasRelativePathSources(richTextContent);
            
            logger.info("📋 内容分析: data-resource-key={}, placeholders={}, relative-paths={}", 
                hasDataResourceKey, hasPlaceholders, hasRelativePaths);
            
            if (!hasDataResourceKey && !hasPlaceholders && !hasRelativePaths) {
                logger.info("✅ 内容无需处理，直接返回");
                return richTextContent;
            }

            String processedContent = richTextContent;

            if (hasPlaceholders) {
                logger.info("🔧 处理占位符模式...");
                processedContent = processPlaceholders(processedContent, useWorker);
            }

            if (hasDataResourceKey) {
                logger.info("🔧 处理data-resource-key模式...");
                processedContent = processDataResourceKeys(processedContent, useWorker);
                if (processedContent.contains("data-resource-key")) {
                    logger.warn("⚠️ 处理后仍包含data-resource-key，可能有转换失败的项目");
                }
            }

            if (hasRelativePaths) {
                logger.info("🔧 处理相对路径模式...");
                processedContent = processRelativePathSources(processedContent, useWorker);
            }

            logger.info("✅ 富文本处理完成: 原始长度={}, 处理后长度={}", 
                richTextContent.length(), processedContent.length());

            return processedContent;

        } catch (Exception e) {
            logger.error("❌ 处理富文本失败，返回原始内容: {}", e.getMessage(), e);
            return richTextContent;
        }
    }

    /** 统一的 URL 生成入口，useWorker=true 时始终走 Worker 域名 */
    private String signUrl(String resourcePath, ChiguaUrlService.ResourceType type, boolean useWorker) {
        return useWorker
            ? chiguaUrlService.generateWorkerUrl(resourcePath, type)
            : chiguaUrlService.generateUrl(resourcePath, type);
    }

    /**
     * 检查是否包含相对路径的source或img标签
     */
    private boolean hasRelativePathSources(String content) {
        logger.debug("🔍 检查是否包含相对路径源");
        
        // 匹配src属性的标签
        Pattern relativePathPattern = Pattern.compile(
            "<(?:source|img|video)[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = relativePathPattern.matcher(content);
        while (matcher.find()) {
            String srcValue = matcher.group(1);
            logger.debug("🔍 检查src值: {}", srcValue);
            // 只检查相对路径（不以http://、https://、blob:开头）
            if (!srcValue.startsWith("http://") && !srcValue.startsWith("https://") && !srcValue.startsWith("blob:") && !srcValue.isEmpty()) {
                logger.info("✅ 发现相对路径src: {}", srcValue);
                return true;
            }
        }
        
        // 也检查poster属性
        Pattern posterPattern = Pattern.compile(
            "<video[^>]*poster\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher posterMatcher = posterPattern.matcher(content);
        while (posterMatcher.find()) {
            String posterValue = posterMatcher.group(1);
            logger.debug("🔍 检查poster值: {}", posterValue);
            if (!posterValue.startsWith("http://") && !posterValue.startsWith("https://") && !posterValue.startsWith("blob:") && !posterValue.isEmpty()) {
                logger.info("✅ 发现相对路径poster: {}", posterValue);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 处理相对路径的source和img标签
     */
    private String processRelativePathSources(String content, boolean useWorker) {
        content = processRelativePathInSourceTags(content, useWorker);
        content = processRelativePathInImgTags(content, useWorker);
        content = processRelativePathInVideoPoster(content, useWorker);
        return content;
    }

    /**
     * 处理source标签中的相对路径
     */
    private String processRelativePathInSourceTags(String content, boolean useWorker) {
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
                    String signedUrl = signUrl(srcValue, resourceType, useWorker);
                    
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
                    logger.info("🔄 source相对路径转换: {} -> {}", srcValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("source相对路径处理失败: {}, 错误: {}", srcValue, e.getMessage());
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
     * 处理img标签中的相对路径
     */
    private String processRelativePathInImgTags(String content, boolean useWorker) {
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
                    // 图片类型
                    String signedUrl = signUrl(srcValue, ChiguaUrlService.ResourceType.IMAGE, useWorker);
                    
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
                    logger.info("🔄 img相对路径转换: {} -> {}", srcValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("img相对路径处理失败: {}, 错误: {}", srcValue, e.getMessage());
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
     * 处理video标签poster属性中的相对路径
     */
    private String processRelativePathInVideoPoster(String content, boolean useWorker) {
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
                    // 封面图片类型
                    String signedUrl = signUrl(posterValue, ChiguaUrlService.ResourceType.COVER, useWorker);
                    
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
                    logger.info("🔄 video poster相对路径转换: {} -> {}", posterValue, signedUrl);
                } catch (Exception e) {
                    logger.warn("video poster相对路径处理失败: {}, 错误: {}", posterValue, e.getMessage());
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
     * 处理占位符模式：{{VIDEO:123:path/to/video.mp4}}
     */
    private String processPlaceholders(String content, boolean useWorker) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String resourceType = matcher.group(1);
            String resourceId = matcher.group(2);
            String resourcePath = matcher.group(3);

            try {
                String signedUrl = generateSignedUrlByType(resourceType, resourcePath, useWorker);
                matcher.appendReplacement(result, Matcher.quoteReplacement(signedUrl));
                logger.debug("占位符替换: {}:{}:{} -> {}", resourceType, resourceId, resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("占位符处理失败: {}:{}:{}, 错误: {}", resourceType, resourceId, resourcePath, e.getMessage());
                // 保留原始占位符
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理data-resource-key模式：data-resource-key="path" -> src="signed_url"
     */
    private String processDataResourceKeys(String content, boolean useWorker) {
        content = processVideoPosterKeys(content, useWorker);
        content = processSourceSrcKeys(content, useWorker);
        content = processOtherSrcKeys(content, useWorker);
        return content;
    }

    /**
     * 处理video标签的poster属性
     */
    private String processVideoPosterKeys(String content, boolean useWorker) {
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
                    
                    String signedUrl = signUrl(resourcePath, resourceType, useWorker);
                    
                    // 替换data-resource-key为poster属性
                    String newAttributes = videoAttributes.replace(
                        "data-resource-key=\"" + resourcePath + "\"",
                        "poster=\"" + signedUrl + "\""
                    );
                    
                    String replacement = "<video" + newAttributes + ">";
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    logger.info("🔄 video poster转换: {} -> {}", resourcePath, signedUrl);
                } else {
                    // 不是poster，保留原样
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            } catch (Exception e) {
                logger.warn("video poster处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 处理source标签的src属性
     */
    private String processSourceSrcKeys(String content, boolean useWorker) {
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
                String signedUrl = signUrl(resourcePath, resourceType, useWorker);
                
                // 替换data-resource-key为src属性
                String newAttributes = sourceAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<source" + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                logger.info("🔄 source src转换: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("source src处理失败: {}, 错误: {}", resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理其他标签的src属性（图片等）
     */
    private String processOtherSrcKeys(String content, boolean useWorker) {
        // 处理img标签和其他标签的data-resource-key
        Pattern imgPattern = Pattern.compile(
            "<(img|a)([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tagName = matcher.group(1);
            String tagAttributes = matcher.group(2);
            String resourcePath = matcher.group(3);
            
            try {
                // 根据文件扩展名判断资源类型
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                String signedUrl = signUrl(resourcePath, resourceType, useWorker);
                
                // 🔧 修复：如果已经有src属性，则替换它；如果没有，则添加src属性
                String attributeName = "img".equals(tagName) ? "src" : "href";
                
                String newAttributes;
                if (tagAttributes.contains(attributeName + "=\"")) {
                    // 已有src/href属性，替换为新的签名URL
                    newAttributes = tagAttributes.replaceFirst(
                        attributeName + "=\"[^\"]*\"", 
                        attributeName + "=\"" + signedUrl + "\""
                    );
                    // 保留data-resource-key（用于编辑时识别）
                    logger.info("🔄 {} {}更新: {} -> {}", tagName, attributeName, resourcePath, signedUrl);
                } else {
                    // 没有src/href属性，添加新的
                    newAttributes = tagAttributes.replace(
                        "data-resource-key=\"" + resourcePath + "\"",
                        attributeName + "=\"" + signedUrl + "\" data-resource-key=\"" + resourcePath + "\""
                    );
                    logger.info("🔄 {} {}添加: {} -> {}", tagName, attributeName, resourcePath, signedUrl);
                }
                
                String replacement = "<" + tagName + newAttributes + ">";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                logger.warn("{}处理失败: {}, 错误: {}", tagName, resourcePath, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }



    /**
     * 根据资源类型生成签名URL
     */
    private String generateSignedUrlByType(String resourceType, String resourcePath, boolean useWorker) {
        ChiguaUrlService.ResourceType type;
        
        switch (resourceType.toUpperCase()) {
            case "VIDEO":
                type = ChiguaUrlService.ResourceType.VIDEO;
                break;
            case "M3U8":
                type = ChiguaUrlService.ResourceType.STREAM;
                break;
            case "COVER":
                type = ChiguaUrlService.ResourceType.COVER;
                break;
            case "THUMBNAIL":
                type = ChiguaUrlService.ResourceType.THUMBNAIL;
                break;
            default:
                type = ChiguaUrlService.ResourceType.IMAGE;
        }

        return signUrl(resourcePath, type, useWorker);
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
     * 将富文本内容转换为存储格式（移除签名URL，使用占位符）
     * 
     * @param richTextContent 包含签名URL的富文本内容
     * @return 适合存储的富文本内容（使用占位符）
     */
    public String prepareRichTextForStorage(String richTextContent) {
        if (!StringUtils.hasText(richTextContent)) {
            return richTextContent;
        }

        try {
            String processedContent = richTextContent;

            // 将签名URL转换为data-resource-key格式
            processedContent = convertUrlsToResourceKeys(processedContent);

            logger.debug("富文本存储格式转换完成: 原始长度={}, 处理后长度={}", 
                richTextContent.length(), processedContent.length());

            return processedContent;

        } catch (Exception e) {
            logger.error("转换富文本存储格式失败，返回原始内容: {}", e.getMessage(), e);
            return richTextContent;
        }
    }

    /**
     * 将签名URL转换为data-resource-key格式
     */
    private String convertUrlsToResourceKeys(String content) {
        String processedContent = content;
        
        // 处理各种包含签名URL的属性
        String[] urlAttributes = {"src", "poster", "href"};
        
        for (String attribute : urlAttributes) {
            // 🔧 修复：支持多种签名URL格式的正则表达式
            // 匹配多种可能的签名参数：key=, signature=, token=, sign= 等
            Pattern signedUrlPattern = Pattern.compile(
                attribute + "=\"([^\"]*?)\\?[^\"]*?(?:key|signature|token|sign|resource)=([^&\"]+?)(?:[&\"]+[^\"]*?)*\""
            );
            
            Matcher matcher = signedUrlPattern.matcher(processedContent);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String fullUrl = matcher.group(1);
                String resourceKey = matcher.group(2);

                // 尝试从URL中提取更准确的资源键
                String extractedKey = extractResourceKeyFromUrl(fullUrl, resourceKey);

                // 替换为data-resource-key格式
                String replacement = "data-resource-key=\"" + extractedKey + "\"";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                logger.info("🔄 URL转换为资源键: {}=\"{}\" -> data-resource-key=\"{}\"", 
                    attribute, fullUrl, extractedKey);
            }
            matcher.appendTail(result);
            processedContent = result.toString();
        }
        
        // 🔧 修复：更安全的清理逻辑，只清理确实有问题的URL
        processedContent = cleanupProblematicUrls(processedContent);
        
        return processedContent;
    }

    /**
     * 从URL中提取资源键
     */
    private String extractResourceKeyFromUrl(String fullUrl, String paramValue) {
        // 优先使用URL路径作为资源键（更准确）
        try {
            // 尝试从完整URL中提取路径部分
            if (fullUrl.contains("://")) {
                // 完整URL: https://domain.com/path/to/resource.ext
                String[] parts = fullUrl.split("://");
                if (parts.length > 1) {
                    String afterProtocol = parts[1];
                    int slashIndex = afterProtocol.indexOf('/');
                    if (slashIndex > 0 && slashIndex < afterProtocol.length() - 1) {
                        String resourcePath = afterProtocol.substring(slashIndex + 1);
                        // 移除查询参数
                        int questionIndex = resourcePath.indexOf('?');
                        if (questionIndex > 0) {
                            resourcePath = resourcePath.substring(0, questionIndex);
                        }
                        if (!resourcePath.isEmpty()) {
                            logger.debug("从URL路径提取资源键: {} -> {}", fullUrl, resourcePath);
                            return resourcePath;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("从URL路径提取资源键失败: {}", fullUrl, e);
        }
        
        // 备选方案：使用参数值
        return paramValue;
    }

    /**
     * 清理有问题的URL，但保留有效的URL
     */
    private String cleanupProblematicUrls(String content) {
        // 只移除确实有问题的blob URL
        content = content.replaceAll("src=\"blob:[^\"]*\"", "");
        
        // 移除明确为空的src属性，但要更精确
        content = content.replaceAll("\\s+src=\"\"(?=\\s|>)", "");
        
        // 🔧 新增：确保video标签至少有一个有效的source或src
        content = ensureVideoHasValidSource(content);
        
        return content;
    }

    /**
     * 确保video标签有有效的视频源
     */
    private String ensureVideoHasValidSource(String content) {
        // 查找没有有效源的video标签并记录警告
        Pattern videoPattern = Pattern.compile(
            "<video[^>]*>.*?</video>", 
            Pattern.DOTALL
        );
        
        Matcher matcher = videoPattern.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String videoBlock = matcher.group();
            
            // 检查是否有有效的源
            boolean hasValidSrc = videoBlock.contains("src=\"") && 
                                !videoBlock.contains("src=\"\"") &&
                                !videoBlock.contains("src=\"blob:");
            
            boolean hasValidSource = videoBlock.contains("<source") && 
                                   videoBlock.contains("src=\"") &&
                                   !videoBlock.contains("src=\"\"") &&
                                   !videoBlock.contains("src=\"blob:");
            
            boolean hasDataResourceKey = videoBlock.contains("data-resource-key=\"");
            
            if (!hasValidSrc && !hasValidSource && !hasDataResourceKey) {
                logger.warn("⚠️ 发现没有有效视频源的video标签: {}", 
                    videoBlock.length() > 200 ? videoBlock.substring(0, 200) + "..." : videoBlock);
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(videoBlock));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
} 