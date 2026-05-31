package com.ruoyi.chigua.controller.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.service.web.IWebCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web合集Controller
 * 
 * @author chigua
 * @date 2025-01-20
 */
@RestController
@RequestMapping("/web/api/collection")
@CrossOrigin(origins = "*")
public class WebCollectionController
{
    private static final Logger logger = LoggerFactory.getLogger(WebCollectionController.class);
    
    @Autowired
    private IWebCollectionService webCollectionService;

    /**
     * 根据分类 ID查询合集列表
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/list")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult list(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        logger.info("🔍 获取合集列表: categoryId={}, page={}, pageSize={}", categoryId, page, pageSize);
        
        try {
            TableDataInfo result = webCollectionService.selectWebCollectionList(categoryId, page, pageSize);
            logger.info("✅ 获取合集列表成功: 共{}条", result.getTotal());
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("❌ 获取合集列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取合集列表失败");
        }
    }

    /**
     * 获取合集详细信息
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping(value = "/{collectionId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getInfo(@PathVariable("collectionId") Long collectionId)
    {
        logger.info("🔍 获取合集详情: collectionId={}", collectionId);
        
        try {
            Collection collection = webCollectionService.selectWebCollectionById(collectionId);
            if (collection != null) {
                logger.info("✅ 获取合集详情成功: title={}", collection.getTitle());
                return AjaxResult.success(collection);
            } else {
                logger.warn("⚠️ 合集不存在: collectionId={}", collectionId);
                return AjaxResult.error("合集不存在");
            }
        } catch (Exception e) {
            logger.error("❌ 获取合集详情失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取合集详情失败");
        }
    }

    /**
     * 获取合集中的视频列表
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping(value = "/{collectionId}/videos")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCollectionVideos(@PathVariable("collectionId") Long collectionId)
    {
        logger.info("🔍 获取合集视频列表: collectionId={}", collectionId);
        
        try {
            List<Video> videos = webCollectionService.selectVideosByCollectionId(collectionId);
            logger.info("✅ 获取合集视频列表成功: 共{}个视频", videos.size());
            return AjaxResult.success(videos);
        } catch (Exception e) {
            logger.error("❌ 获取合集视频列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取合集视频列表失败");
        }
    }
} 