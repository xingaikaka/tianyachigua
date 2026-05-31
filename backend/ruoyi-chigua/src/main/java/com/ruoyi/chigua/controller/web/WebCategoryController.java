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
import com.ruoyi.chigua.domain.vo.web.WebCategoryVO;
import com.ruoyi.chigua.service.web.IWebCategoryService;
import com.ruoyi.chigua.domain.vo.web.WebVideoVO;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.IAdvertisementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

/**
 * Web分类Controller
 * 
 * @author chigua
 * @date 2024-12-20
 */
@RestController
@RequestMapping("/web/api/category")
@CrossOrigin(origins = "*")
public class WebCategoryController
{
    private static final Logger logger = LoggerFactory.getLogger(WebCategoryController.class);
    
    @Autowired
    private IWebCategoryService webCategoryService;
    
    @Autowired
    private ICategoryService categoryService;
    
    @Autowired
    private IAdvertisementService advertisementService;

    /**
     * 查询分类列表
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/list")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult list()
    {
        com.ruoyi.chigua.domain.vo.web.WebCategoryListResponse response = webCategoryService.selectWebCategoryList();
        return AjaxResult.success(response);
    }

    /**
     * 获取分类列表版本号
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/version")
    public AjaxResult getCategoryVersion()
    {
        String version = webCategoryService.getCategoryListVersion();
        Map<String, Object> result = new HashMap<>();
        result.put("version", version);
        return AjaxResult.success(result);
    }

    /**
     * 获取分类详细信息（VO格式）
     */
    @RateLimiter(time = 60, count = 80, limitType = LimitType.IP)
    @GetMapping(value = "/{categoryId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        WebCategoryVO category = webCategoryService.selectWebCategoryById(categoryId);
        if (category != null) {
            return AjaxResult.success(category);
        } else {
            return AjaxResult.error("分类不存在");
        }
    }

    /**
     * 获取分类完整信息（包含isCollection字段）
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping(value = "/{categoryId}/detail")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryDetail(@PathVariable("categoryId") Long categoryId)
    {
        logger.debug("查询分类详情: categoryId={}", categoryId);
        Category category = categoryService.selectCategoryById(categoryId);
        if (category != null) {
            logger.debug("分类详情: id={}, name={}, status={}", category.getId(), category.getName(), category.getStatus());
            if (category.getStatus() == 1) {
                return AjaxResult.success(category);
            } else {
                logger.warn("⚠️ [分类详情] 分类已禁用: id={}, status={}", categoryId, category.getStatus());
                return AjaxResult.error("分类不存在或已禁用");
            }
        } else {
            logger.warn("⚠️ [分类详情] 分类不存在: id={}", categoryId);
            return AjaxResult.error("分类不存在或已禁用");
        }
    }

    /**
     * 获取视频列表
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/videos")
    @EncryptResponse // 启用响应数据加密
    public TableDataInfo getVideoList(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        return webCategoryService.selectWebVideoList(categoryId, pageNum, pageSize);
    }

    /**
     * 获取视频列表（优化版，仅必需字段）
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/videos/optimized")
    @EncryptResponse // 启用响应数据加密
    public TableDataInfo getVideoListOptimized(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "title", required = false) String title, // 标题搜索
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "tagIds", required = false) String tagIdsStr, // 多个标签ID，逗号分隔
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "authors", required = false) String authorsStr, // 多个作者，逗号分隔
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        // 解析多个标签ID
        List<Long> tagIds = null;
        if (tagIdsStr != null && !tagIdsStr.trim().isEmpty()) {
            try {
                tagIds = java.util.Arrays.stream(tagIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                logger.warn("解析标签ID列表失败: {}", tagIdsStr, e);
            }
        }
        
        // 解析多个作者
        List<String> authors = null;
        if (authorsStr != null && !authorsStr.trim().isEmpty()) {
            authors = java.util.Arrays.stream(authorsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
        
        return webCategoryService.selectWebVideoListOptimized(categoryId, title, tagId, tagIds, author, authors, pageNum, pageSize);
    }

    /**
     * 短视频模式：按分类或标签查询短视频列表
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/videos/short")
    @EncryptResponse // 启用响应数据加密
    public TableDataInfo getShortVideoList(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "title", required = false) String title, // 标题搜索
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "tagIds", required = false) String tagIdsStr, // 多个标签ID，逗号分隔
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "authors", required = false) String authorsStr, // 多个作者，逗号分隔
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        // 解析多个标签ID
        List<Long> tagIds = null;
        if (tagIdsStr != null && !tagIdsStr.trim().isEmpty()) {
            try {
                tagIds = java.util.Arrays.stream(tagIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                logger.warn("解析标签ID列表失败: {}", tagIdsStr, e);
            }
        }
        
        // 解析多个作者
        List<String> authors = null;
        if (authorsStr != null && !authorsStr.trim().isEmpty()) {
            authors = java.util.Arrays.stream(authorsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
        
        return webCategoryService.selectWebShortVideoList(categoryId, title, tagId, tagIds, author, authors, pageNum, pageSize);
    }

    /**
     * 获取视频详情（支持基于查询上下文的相邻视频）
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/video/detail/{id}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getVideoDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "fromCategory", required = false) Long categoryId,
            @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
            @RequestParam(value = "sortType", required = false) String sortType)
    {
        try {
            // 获取视频详情
            WebVideoVO webVideo = webCategoryService.selectWebVideoDetail(id);
            if (webVideo == null) {
                return AjaxResult.error("视频不存在或已下架");
            }
            
                         // 🔄 基于查询上下文获取相邻视频（使用优化查询）
             if (categoryId != null || searchKeyword != null || sortType != null) {
                 logger.info("🔍 基于查询上下文获取相邻视频 (优化版本): categoryId={}, searchKeyword={}, sortType={}", 
                     categoryId, searchKeyword, sortType);
                 
                 try {
                     WebVideoVO.AdjacentVideoInfo contextPreviousVideo = 
                         webCategoryService.selectPreviousVideo(id, categoryId, searchKeyword, sortType);
                     WebVideoVO.AdjacentVideoInfo contextNextVideo = 
                         webCategoryService.selectNextVideo(id, categoryId, searchKeyword, sortType);
                     
                     // 覆盖默认的相邻视频信息
                     webVideo.setPreviousVideo(contextPreviousVideo);
                     webVideo.setNextVideo(contextNextVideo);
                     
                     logger.info("✅ 基于上下文的相邻视频已更新 (优化版本): previous={}, next={}", 
                         contextPreviousVideo != null ? contextPreviousVideo.getId() : "null",
                         contextNextVideo != null ? contextNextVideo.getId() : "null");
                 } catch (Exception e) {
                     logger.warn("⚠️ 基于上下文获取相邻视频失败，使用默认逻辑: {}", e.getMessage());
                     // 继续使用默认的相邻视频，不影响主要功能
                 }
             }
            
                         return AjaxResult.success(webVideo);
         } catch (Exception e) {
             logger.error("❌ 获取视频详情失败: videoId={}, 错误: {}", id, e.getMessage(), e);
             return AjaxResult.error("获取视频详情失败");
         }
    }

    /**
     * 获取热门推荐视频列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/videos/hot-recommended")
    @EncryptResponse // 启用响应数据加密
    public TableDataInfo getHotRecommendedVideoList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize)
    {
        return webCategoryService.selectWebHotRecommendedVideoList(pageNum, pageSize);
    }

    /**
     * 获取上一篇视频
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/video/{id}/previous")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getPreviousVideo(@PathVariable("id") Long id)
    {
        try {
            WebVideoVO.AdjacentVideoInfo previousVideo = webCategoryService.selectPreviousVideo(id);
            if (previousVideo != null) {
                return AjaxResult.success(previousVideo);
            } else {
                return AjaxResult.error("没有找到上一篇视频");
            }
        } catch (Exception e) {
            return AjaxResult.error("获取上一篇视频失败");
        }
    }

    /**
     * 获取下一篇视频
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/video/{id}/next")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getNextVideo(@PathVariable("id") Long id)
    {
        try {
            WebVideoVO.AdjacentVideoInfo nextVideo = webCategoryService.selectNextVideo(id);
            if (nextVideo != null) {
                return AjaxResult.success(nextVideo);
            } else {
                return AjaxResult.error("没有找到下一篇视频");
            }
        } catch (Exception e) {
            return AjaxResult.error("获取下一篇视频失败");
        }
    }

    /**
     * 获取相邻视频（上一篇和下一篇）
     */
    @RateLimiter(time = 60, count = 200, limitType = LimitType.IP)
    @GetMapping("/video/{id}/adjacent")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getAdjacentVideos(@PathVariable("id") Long id)
    {
        try {
            WebVideoVO.AdjacentVideosVO adjacentVideos = webCategoryService.selectAdjacentVideos(id);
            return AjaxResult.success(adjacentVideos);
        } catch (Exception e) {
            return AjaxResult.error("获取相邻视频失败");
        }
    }

    /**
     * 获取分类的广告显示配置
     * @param categoryId 分类ID
     * @return 广告显示配置信息
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{categoryId}/ad-config")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryAdConfig(@PathVariable("categoryId") Long categoryId)
    {
        try {
            Category category = categoryService.selectCategoryById(categoryId);
            if (category == null) {
                return AjaxResult.error("分类不存在");
            }
            
            if (category.getStatus() != 1) {
                return AjaxResult.error("分类已禁用");
            }
            
            Map<String, Object> adConfig = new HashMap<>();
            adConfig.put("categoryId", categoryId);
            adConfig.put("categoryName", category.getName());
            adConfig.put("adDisplayMode", category.getAdDisplayMode() != null ? category.getAdDisplayMode() : 1);
            adConfig.put("adInterval", category.getAdInterval() != null ? category.getAdInterval() : 3);
            
            // 添加显示模式名称
            String modeName = "集中显示";
            if (category.getAdDisplayMode() != null) {
                switch (category.getAdDisplayMode()) {
                    case 1:
                        modeName = "集中显示";
                        break;
                    case 2:
                        modeName = "交替显示";
                        break;
                    case 3:
                        modeName = "不显示广告";
                        break;
                }
            }
            adConfig.put("adDisplayModeName", modeName);
            
            logger.info("获取分类广告配置成功: id={}, mode={}, interval={}", 
                categoryId, adConfig.get("adDisplayMode"), adConfig.get("adInterval"));
            
            return AjaxResult.success(adConfig);
        } catch (Exception e) {
            logger.error("获取分类广告配置失败: id={}, 错误: {}", categoryId, e.getMessage(), e);
            return AjaxResult.error("获取分类广告配置失败");
        }
    }

    /**
     * 获取分类的混合内容（广告+视频，根据显示模式）
     * @param categoryId 分类ID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 混合内容列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{categoryId}/mixed-content")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryMixedContent(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize)
    {
        try {
            Category category = categoryService.selectCategoryById(categoryId);
            if (category == null) {
                return AjaxResult.error("分类不存在");
            }
            
            if (category.getStatus() != 1) {
                return AjaxResult.error("分类已禁用");
            }
            
            // 获取广告显示配置
            Integer adDisplayMode = category.getAdDisplayMode() != null ? category.getAdDisplayMode() : 1;
            Integer adInterval = category.getAdInterval() != null ? category.getAdInterval() : 3;
            
            // 根据显示模式创建混合内容
            Map<String, Object> result = createMixedContent(categoryId, adDisplayMode, adInterval, page, pageSize);
            
            logger.info("获取分类混合内容成功: id={}, mode={}, interval={}, page={}, showAds={}", 
                categoryId, adDisplayMode, adInterval, page, result.get("showAds"));
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("获取分类混合内容失败: id={}, 错误: {}", categoryId, e.getMessage(), e);
            return AjaxResult.error("获取分类混合内容失败");
        }
    }

    /**
     * 创建混合内容（广告+视频）
     * @param categoryId 分类ID
     * @param adDisplayMode 广告显示模式
     * @param adInterval 广告间隔
     * @param page 页码
     * @param pageSize 每页大小
     * @return 混合内容
     */
    private Map<String, Object> createMixedContent(Long categoryId, Integer adDisplayMode, 
            Integer adInterval, Integer page, Integer pageSize)
    {
        Map<String, Object> result = new HashMap<>();
        
        // 模式3：不显示广告，直接返回空的广告和配置信息
        if (adDisplayMode == 3) {
            result.put("adDisplayMode", 3);
            result.put("adInterval", adInterval);
            result.put("showAds", false);
            result.put("mixedItems", Collections.emptyList()); // 返回空列表，前端只显示内容
            result.put("adsFirst", Collections.emptyList());
            result.put("message", "该分类不显示广告");
            
            logger.info("分类{}设置为不显示广告模式", categoryId);
            return result;
        }
        
        // 获取分类的广告
        List<Advertisement> ads = advertisementService.selectAdvertisementByCategory(categoryId);
        
        // 模式1：集中显示
        if (adDisplayMode == 1) {
            result.put("adDisplayMode", 1);
            result.put("adInterval", adInterval);
            result.put("showAds", true);
            result.put("mixedItems", Collections.emptyList()); // 集中显示模式下，前端自行处理广告位置
            result.put("adsFirst", ads); // 所有广告放在前面
            
            logger.info("分类{}使用集中显示模式，广告数量: {}", categoryId, ads.size());
            return result;
        }
        
        // 模式2：交替显示 - 返回配置信息，让前端处理混合逻辑
        result.put("adDisplayMode", 2);
        result.put("adInterval", adInterval);
        result.put("showAds", true);
                    result.put("mixedItems", Collections.emptyList()); // 前端根据interval自行混合
        result.put("availableAds", ads); // 可用的广告列表
        
        logger.info("分类{}使用交替显示模式，广告数量: {}, 间隔: {}", categoryId, ads.size(), adInterval);
        return result;
    }

    /**
     * 获取分类下所有去重的标签
     * 
     * @param categoryId 分类ID
     * @return 标签列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{categoryId}/tags")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryTags(@PathVariable("categoryId") Long categoryId)
    {
        try {
            logger.info("🏷️ 获取分类{}的去重标签", categoryId);
            List<com.ruoyi.chigua.domain.Tag> tags = webCategoryService.selectDistinctTagsByCategory(categoryId);
            return AjaxResult.success(tags);
        } catch (Exception e) {
            logger.error("❌ 获取分类{}的去重标签失败", categoryId, e);
            return AjaxResult.error("获取标签失败");
        }
    }

    /**
     * 获取分类下所有去重的作者
     * 
     * @param categoryId 分类ID
     * @return 作者列表
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{categoryId}/authors")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryAuthors(@PathVariable("categoryId") Long categoryId)
    {
        try {
            logger.info("👤 获取分类{}的去重作者", categoryId);
            List<String> authors = webCategoryService.selectDistinctAuthorsByCategory(categoryId);
            return AjaxResult.success(authors);
        } catch (Exception e) {
            logger.error("❌ 获取分类{}的去重作者失败", categoryId, e);
            return AjaxResult.error("获取作者失败");
        }
    }
} 
