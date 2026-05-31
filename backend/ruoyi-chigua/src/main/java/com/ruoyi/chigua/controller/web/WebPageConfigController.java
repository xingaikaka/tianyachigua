package com.ruoyi.chigua.controller.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.chigua.domain.PageConfig;
import com.ruoyi.chigua.service.IPageConfigService;
import com.ruoyi.chigua.service.WebRichTextProcessorService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web页面配置Controller
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
@RestController
@RequestMapping("/web/api/pageconfig")
@CrossOrigin(origins = "*")
public class WebPageConfigController
{
    private static final Logger logger = LoggerFactory.getLogger(WebPageConfigController.class);
    
    @Autowired
    private IPageConfigService pageConfigService;
    
    @Autowired
    private WebRichTextProcessorService webRichTextProcessorService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 获取所有有效的页面配置（实时处理富文本签名URL）
     */
    @RateLimiter(time = 60, count = 50, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/all")
    public AjaxResult getAllConfigs()
    {
        try {
            logger.info("🔍 开始获取所有页面配置");
            
            Map<String, PageConfig> rawConfigs = pageConfigService.getAllActivePageConfigsSigned();
            String version = pageConfigService.getCurrentConfigVersion();
            
            // 对 image 类型实时签名（不能依赖缓存里的静态 key）
            Map<String, PageConfig> configs = new HashMap<>(rawConfigs);
            for (Map.Entry<String, PageConfig> entry : configs.entrySet()) {
                PageConfig cfg = entry.getValue();
                if ("image".equals(cfg.getConfigType()) && StringUtils.isNotEmpty(cfg.getBasicContent())) {
                    String raw = cfg.getBasicContent();
                    // 如果已经是签名URL（上一轮缓存中混入），跳过
                    if (!raw.startsWith("http")) {
                        try {
                            PageConfig copy = new PageConfig();
                            org.springframework.beans.BeanUtils.copyProperties(cfg, copy);
                            copy.setBasicContent(chiguaUrlService.generateUrl(raw, ChiguaUrlService.ResourceType.IMAGE));
                            entry.setValue(copy);
                        } catch (Exception ignore) {}
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("configs", configs);
            result.put("version", version);
            
            logger.info("✅ 获取页面配置成功，共{}个配置项，版本号：{}", configs.size(), version);
            return AjaxResult.success("获取页面配置成功", result);
            
        } catch (Exception e) {
            logger.error("❌ 获取页面配置失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取页面配置失败");
        }
    }

    /**
     * 检查配置版本是否需要更新
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/version")
    public AjaxResult checkVersion(@RequestParam(required = false) String clientVersion)
    {
        try {
            logger.info("🔍 检查配置版本，客户端版本：{}", clientVersion);
            
            String serverVersion = pageConfigService.getCurrentConfigVersion();
            boolean needUpdate = StringUtils.isEmpty(clientVersion) || !serverVersion.equals(clientVersion);
            
            Map<String, Object> result = new HashMap<>();
            result.put("needUpdate", needUpdate);
            result.put("serverVersion", serverVersion);
            result.put("clientVersion", clientVersion);
            
            logger.info("✅ 版本检查完成，需要更新：{}，服务器版本：{}", needUpdate, serverVersion);
            return AjaxResult.success("版本检查成功", result);
            
        } catch (Exception e) {
            logger.error("❌ 版本检查失败：{}", e.getMessage(), e);
            return AjaxResult.error("版本检查失败");
        }
    }

    /**
     * 根据配置键值获取页面配置
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/key/{configKey}")
    public AjaxResult getConfigByKey(@PathVariable("configKey") String configKey)
    {
        try {
            logger.info("🔍 根据键值获取页面配置：{}", configKey);
            
            PageConfig config = pageConfigService.selectPageConfigByKey(configKey);
            
            if (config != null) {
                // 单条按需也做签名处理，避免其它入口返回无签名内容
                PageConfig processed = processPageConfigRichText(config);
                logger.info("✅ 获取页面配置成功：{}", config.getConfigName());
                return AjaxResult.success("获取页面配置成功", processed);
            } else {
                logger.warn("⚠️ 未找到配置项：{}", configKey);
                return AjaxResult.error("未找到指定的配置项");
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取页面配置失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取页面配置失败");
        }
    }

    /**
     * 根据配置类型获取页面配置列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/type/{configType}")
    public AjaxResult getConfigsByType(@PathVariable("configType") String configType)
    {
        try {
            logger.info("🔍 根据类型获取页面配置：{}", configType);
            
            List<PageConfig> configs = pageConfigService.selectPageConfigListByType(configType);
            
            logger.info("✅ 获取页面配置成功，共{}个配置项", configs.size());
            return AjaxResult.success("获取页面配置成功", configs);
            
        } catch (Exception e) {
            logger.error("❌ 获取页面配置失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取页面配置失败");
        }
    }

    /**
     * 根据配置分类获取页面配置列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/category/{configCategory}")
    public AjaxResult getConfigsByCategory(@PathVariable("configCategory") String configCategory)
    {
        try {
            logger.info("🔍 根据分类获取页面配置：{}", configCategory);
            
            List<PageConfig> configs = pageConfigService.selectPageConfigListByCategory(configCategory);
            
            logger.info("✅ 获取页面配置成功，共{}个配置项", configs.size());
            return AjaxResult.success("获取页面配置成功", configs);
            
        } catch (Exception e) {
            logger.error("❌ 获取页面配置失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取页面配置失败");
        }
    }

    /**
     * 获取底部页面信息（兼容原有接口）
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/footer")
    public AjaxResult getFooterInfo()
    {
        try {
            logger.info("🔍 获取底部页面信息");
            
            PageConfig config = pageConfigService.selectPageConfigByKey("footer_page_info");
            
            if (config != null) {
                // 构造兼容原有格式的返回数据（富文本拼接decrypt=true）
                Map<String, Object> result = new HashMap<>();
                result.put("noticeTitle", config.getConfigName());
                String content = config.getRichContent();
                if (StringUtils.isNotEmpty(content)) {
                    try {
                        content = webRichTextProcessorService.processRichTextForAdminEditor(content);
                    } catch (Exception ignore) {}
                }
                result.put("noticeContent", content);
                
                logger.info("✅ 获取底部页面信息成功");
                return AjaxResult.success("获取底部页面信息成功", result);
            } else {
                logger.warn("⚠️ 未找到底部页面信息配置");
                return AjaxResult.error("未找到底部页面信息");
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取底部页面信息失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取底部页面信息失败");
        }
    }

    /**
     * 获取站点基础配置信息
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/site")
    public AjaxResult getSiteConfigs()
    {
        try {
            logger.info("🔍 获取站点基础配置信息");
            
            List<PageConfig> configs = pageConfigService.selectPageConfigListByCategory("site_config");
            
            // 转换为键值对格式
            Map<String, String> siteConfigs = new HashMap<>();
            for (PageConfig config : configs) {
                siteConfigs.put(config.getConfigKey(), config.getBasicContent());
            }
            
            logger.info("✅ 获取站点配置成功，共{}个配置项", siteConfigs.size());
            return AjaxResult.success("获取站点配置成功", siteConfigs);
            
        } catch (Exception e) {
            logger.error("❌ 获取站点配置失败：{}", e.getMessage(), e);
            return AjaxResult.error("获取站点配置失败");
        }
    }

    /**
     * 处理页面配置中的富文本内容，实时生成签名URL
     * 
     * @param config 原始页面配置
     * @return 处理后的页面配置
     */
    private PageConfig processPageConfigRichText(PageConfig config) {
        if (config == null) {
            return null;
        }

        try {
            // 创建新的配置对象，避免修改原始缓存数据
            PageConfig processedConfig = new PageConfig();
            processedConfig.setConfigId(config.getConfigId());
            processedConfig.setConfigKey(config.getConfigKey());
            processedConfig.setConfigName(config.getConfigName());
            processedConfig.setConfigType(config.getConfigType());
            processedConfig.setConfigCategory(config.getConfigCategory());
            processedConfig.setStatus(config.getStatus());
            processedConfig.setSortOrder(config.getSortOrder());
            processedConfig.setCreateBy(config.getCreateBy());
            processedConfig.setCreateTime(config.getCreateTime());
            processedConfig.setUpdateBy(config.getUpdateBy());
            processedConfig.setUpdateTime(config.getUpdateTime());
            processedConfig.setRemark(config.getRemark());

            // 🚀 处理富文本内容 - 实时生成签名URL
            if (StringUtils.isNotEmpty(config.getRichContent())) {
                String processedRichContent = processConfigRichTextWithFreshUrls(config.getRichContent());
                processedConfig.setRichContent(processedRichContent);
                //logger.debug("🔗 页面配置富文本实时处理完成: {}", config.getConfigKey());
            } else {
                processedConfig.setRichContent(config.getRichContent());
            }

            // 图片配置：basicContent 存的是 R2 key，用当前 CDN 配置签名后返回
            if ("image".equals(config.getConfigType()) && StringUtils.isNotEmpty(config.getBasicContent())) {
                try {
                    String signedUrl = chiguaUrlService.generateUrl(config.getBasicContent(), ChiguaUrlService.ResourceType.IMAGE);
                    processedConfig.setBasicContent(signedUrl);
                } catch (Exception e) {
                    processedConfig.setBasicContent(config.getBasicContent());
                }
            } else {
                processedConfig.setBasicContent(config.getBasicContent());
            }
            processedConfig.setJumpUrl(config.getJumpUrl());

            return processedConfig;

        } catch (Exception e) {
            logger.warn("❌ 页面配置{}富文本处理失败，使用原始内容: {}", config.getConfigKey(), e.getMessage());
            return config; // 降级返回原始配置
        }
    }

    /**
     * 实时处理页面配置富文本内容，生成新鲜的签名URL（不缓存）
     * 解决页面配置中图片签名过期的问题
     */
    private String processConfigRichTextWithFreshUrls(String richTextContent) {
        if (!StringUtils.hasText(richTextContent)) {
            return richTextContent;
        }

        try {
            //logger.debug("🔄 实时处理页面配置富文本内容，生成新鲜签名URL");
            
            String processedContent = richTextContent;
            
            // 处理data-resource-key模式的图片
            processedContent = processConfigDataResourceKeysWithFreshUrls(processedContent);
            
            // 处理相对路径的图片
            processedContent = processConfigRelativePathsWithFreshUrls(processedContent);
            
            //logger.debug("✅ 页面配置富文本实时处理完成");
            return processedContent;
            
        } catch (Exception e) {
            logger.error("❌ 页面配置富文本实时处理失败", e);
            return richTextContent; // 降级返回原始内容
        }
    }

    /**
     * 处理页面配置中data-resource-key模式，实时生成签名URL
     */
    private String processConfigDataResourceKeysWithFreshUrls(String content) {
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                ChiguaUrlService.ResourceType resourceType = detectResourceType(resourcePath);
                String signedUrl = chiguaUrlService.generateUrl(resourcePath, resourceType, false);
                
                // 替换data-resource-key为src属性
                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                
                //logger.debug("🔗 页面配置实时生成图片签名URL: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("❌ 页面配置为资源{}生成签名URL失败: {}", resourcePath, e.getMessage());
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 处理页面配置中相对路径，实时生成签名URL
     */
    private String processConfigRelativePathsWithFreshUrls(String content) {
        // 处理img标签的src属性
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String src = matcher.group(2);

            // 只处理相对路径
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                try {
                ChiguaUrlService.ResourceType resourceType = detectResourceType(src);
                String signedUrl = chiguaUrlService.generateUrl(src, resourceType, false);
                    
                    String newAttributes = imgAttributes.replace(
                        "src=\"" + src + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + src + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<img" + newAttributes + ">";
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    
                    //logger.debug("🔗 页面配置实时生成相对路径图片签名URL: {} -> {}", src, signedUrl);
                } catch (Exception e) {
                    logger.warn("❌ 页面配置为相对路径{}生成签名URL失败: {}", src, e.getMessage());
                    // 保持原始标签
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            } else {
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
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
}