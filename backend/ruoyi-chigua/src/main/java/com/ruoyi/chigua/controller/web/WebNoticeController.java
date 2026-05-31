package com.ruoyi.chigua.controller.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.cache.annotation.Cacheable;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;
import com.ruoyi.chigua.service.WebRichTextProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web公告Controller
 * 
 * @author chigua
 * @date 2025-01-21
 */
@RestController
@RequestMapping("/web/api/notice")
@CrossOrigin(origins = "*")
public class WebNoticeController
{
    private static final Logger logger = LoggerFactory.getLogger(WebNoticeController.class);
    
    @Autowired
    private ISysNoticeService noticeService;
    
    @Autowired
    private WebRichTextProcessorService webRichTextProcessorService;

    /**
     * 获取底部描述信息（带缓存）
     */
    @RateLimiter(time = 60, count = 80, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/bottom-description")
    @Cacheable(value = "sysNoticeList", key = "'bottom_description'", unless = "#result == null")
    public AjaxResult getBottomDescription()
    {
        try {
            logger.info("🔍 开始获取底部描述信息");
            
            // 创建查询条件 - 获取category_type为"底部描述类型"的公告
            SysNotice queryNotice = new SysNotice();
            queryNotice.setCategoryType("底部描述类型");
            queryNotice.setStatus("0"); // 只获取状态为正常的公告
            
            List<SysNotice> noticeList = noticeService.selectNoticeList(queryNotice);
            
            if (noticeList != null && !noticeList.isEmpty()) {
                // 返回第一条底部描述数据
                SysNotice bottomNotice = noticeList.get(0);
                logger.info("✅ 成功获取底部描述信息: {}", bottomNotice.getNoticeTitle());
                return AjaxResult.success(bottomNotice);
            } else {
                logger.warn("⚠️ 未找到底部描述信息");
                return AjaxResult.success(null);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取底部描述信息失败", e);
            return AjaxResult.error("获取底部描述信息失败");
        }
    }

    /**
     * 获取最新公告信息（实时处理富文本，不缓存最终结果）
     * 注意：移除了@Cacheable注解，确保富文本中的签名URL每次都是新鲜的
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/latest")
    public AjaxResult getLatestAnnouncement(String categoryType)
    {
        try {
            logger.info("🔍 开始获取最新公告信息, 分类类型: {}", categoryType);
            
            // 创建查询条件
            SysNotice queryNotice = new SysNotice();
            queryNotice.setCategoryType(categoryType != null ? categoryType : "公告");
            queryNotice.setStatus("0"); // 只获取状态为正常的公告
            
            List<SysNotice> noticeList = noticeService.selectNoticeList(queryNotice);
            
            if (noticeList != null && !noticeList.isEmpty()) {
                // 返回第一条公告数据（按创建时间倒序，最新的在前）
                SysNotice latestNotice = noticeList.get(0);
                
                // 🚀 实时处理富文本内容，生成新鲜的签名URL（避免过期）
                if (latestNotice.getNoticeContent() != null && !latestNotice.getNoticeContent().trim().isEmpty()) {
                    try {
                        String processedContent = processNoticeRichTextWithFreshUrls(latestNotice.getNoticeContent());
                        latestNotice.setNoticeContent(processedContent);
                        logger.debug("🔗 公告富文本实时处理完成: {}", latestNotice.getNoticeTitle());
                    } catch (Exception e) {
                        logger.warn("❌ 公告富文本实时处理失败，使用原始内容: {}", e.getMessage());
                        // 继续使用原始内容，不影响正常显示
                    }
                }
                
                logger.info("✅ 成功获取最新公告: {}", latestNotice.getNoticeTitle());
                return AjaxResult.success(latestNotice);
            } else {
                logger.warn("⚠️ 未找到分类为 {} 的公告信息", categoryType);
                return AjaxResult.success(null);
            }
            
        } catch (Exception e) {
            logger.error("❌ 获取最新公告信息失败", e);
            return AjaxResult.error("获取最新公告信息失败");
        }
    }

    /**
     * 实时处理公告富文本内容，生成新鲜的签名URL（不缓存）
     * 解决公告中图片签名过期的问题
     */
    private String processNoticeRichTextWithFreshUrls(String richTextContent) {
        if (!org.springframework.util.StringUtils.hasText(richTextContent)) {
            return richTextContent;
        }

        try {
            logger.debug("🔄 实时处理公告富文本内容，生成新鲜签名URL");
            
            String processedContent = richTextContent;
            
            // 处理data-resource-key模式的图片
            processedContent = processNoticeDataResourceKeysWithFreshUrls(processedContent);
            
            // 处理相对路径的图片
            processedContent = processNoticeRelativePathsWithFreshUrls(processedContent);
            
            logger.debug("✅ 公告富文本实时处理完成");
            return processedContent;
            
        } catch (Exception e) {
            logger.error("❌ 公告富文本实时处理失败", e);
            return richTextContent; // 降级返回原始内容
        }
    }

    /**
     * 处理公告中data-resource-key模式，实时生成签名URL
     */
    private String processNoticeDataResourceKeysWithFreshUrls(String content) {
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                // 实时生成签名URL
                String signedUrl = webRichTextProcessorService.generateImageUrl(resourcePath);
                
                // 替换data-resource-key为src属性
                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                
               // logger.debug("🔗 公告实时生成图片签名URL: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("❌ 公告为资源{}生成签名URL失败: {}", resourcePath, e.getMessage());
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 处理公告中相对路径，实时生成签名URL
     */
    private String processNoticeRelativePathsWithFreshUrls(String content) {
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
                    // 实时生成签名URL
                    String signedUrl = webRichTextProcessorService.generateImageUrl(src);
                    
                    String newAttributes = imgAttributes.replace(
                        "src=\"" + src + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + src + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<img" + newAttributes + ">";
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    
                    //logger.debug("🔗 公告实时生成相对路径图片签名URL: {} -> {}", src, signedUrl);
                } catch (Exception e) {
                    logger.warn("❌ 公告为相对路径{}生成签名URL失败: {}", src, e.getMessage());
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
}