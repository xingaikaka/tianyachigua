package com.ruoyi.chigua.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.service.web.IWebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web搜索Controller
 * 
 * @author chigua
 * @date 2025-01-22
 */
@RestController
@RequestMapping("/web/api/search")
@CrossOrigin(origins = "*")
public class WebSearchController
{
    private static final Logger logger = LoggerFactory.getLogger(WebSearchController.class);
    
    @Autowired
    private IWebSearchService webSearchService;

    /**
     * 根据关键词搜索视频
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping("/videos")
    @EncryptResponse // 启用响应数据加密
    public TableDataInfo searchVideos(
            @RequestParam(value = "keyword", required = true) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        logger.info("🔍 搜索视频: keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            logger.warn("⚠️ 搜索关键词为空");
            TableDataInfo emptyResult = new TableDataInfo();
            emptyResult.setTotal(0);
            emptyResult.setRows(java.util.Collections.emptyList());
            return emptyResult;
        }
        
        try {
            TableDataInfo result = webSearchService.searchVideosByKeyword(keyword.trim(), pageNum, pageSize);
            logger.info("✅ 搜索完成: 关键词={}, 结果数量={}", keyword, result.getTotal());
            return result;
        } catch (Exception e) {
            logger.error("❌ 搜索视频失败: keyword={}, 错误: {}", keyword, e.getMessage(), e);
            TableDataInfo errorResult = new TableDataInfo();
            errorResult.setTotal(0);
            errorResult.setRows(java.util.Collections.emptyList());
            return errorResult;
        }
    }
}