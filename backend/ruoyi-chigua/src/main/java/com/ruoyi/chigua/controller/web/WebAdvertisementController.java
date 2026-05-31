package com.ruoyi.chigua.controller.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.service.web.IWebAdvertisementService;
import com.ruoyi.chigua.domain.vo.web.WebAdvertisementVO;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;

/**
 * Web广告Controller
 * 
 * @author ruoyi
 * @date 2025-01-21
 */
@RestController
@RequestMapping("/web/api/advertisement")
@CrossOrigin(origins = "*")
public class WebAdvertisementController
{
    @Autowired
    private IWebAdvertisementService webAdvertisementService;
    
    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 根据广告位置获取广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/position/{position}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getAdsByPosition(@PathVariable("position") String position)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectAdsByPosition(position);
        return AjaxResult.success(list);
    }

    /**
     * 根据广告类型获取广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/type/{adType}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getAdsByType(@PathVariable("adType") String adType)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectAdsByType(adType);
        return AjaxResult.success(list);
    }

    /**
     * 获取分类列表顶部横幅广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/category/{categoryId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryAds(@PathVariable("categoryId") Long categoryId)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectCategoryAds(categoryId);
        return AjaxResult.success(list);
    }

    /**
     * 获取分类列表底部横幅广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/category/{categoryId}/bottom")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCategoryBottomAds(@PathVariable("categoryId") Long categoryId)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectCategoryBottomAds(categoryId);
        return AjaxResult.success(list);
    }

    /**
     * 根据应用类型获取Logo广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/logo/{appType}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getLogoAdsByAppType(@PathVariable("appType") String appType)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectLogoAdsByAppType(appType);
        return AjaxResult.success(list);
    }

    /**
     * 获取详情页面广告（顶部+底部）
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/detail/{categoryId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getDetailPageAds(@PathVariable("categoryId") Long categoryId)
    {
        java.util.Map<String, java.util.List<WebAdvertisementVO>> result = webAdvertisementService.selectDetailPageAds(categoryId);
        return AjaxResult.success(result);
    }

    /**
     * 获取短视频广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/short-video/{categoryId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getShortVideoAds(@PathVariable("categoryId") Long categoryId)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectShortVideoAdsByCategory(categoryId);
        return AjaxResult.success(list);
    }

    /**
     * 获取分页模式广告
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/paged/{categoryId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getPagedAds(@PathVariable("categoryId") Long categoryId)
    {
        List<WebAdvertisementVO> list = webAdvertisementService.selectPagedAdsByCategory(categoryId);
        return AjaxResult.success(list);
    }

    /**
     * 广告点击统计
     */
    @RateLimiter(time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/click/{id}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult clickAd(@PathVariable("id") Long id)
    {
        try {
            webAdvertisementService.incrementClickCount(id);
            return AjaxResult.success("点击统计成功");
        } catch (Exception e) {
            return AjaxResult.error("点击统计失败");
        }
    }

    /**
     * 广告曝光统计
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/impression/{id}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult impressionAd(@PathVariable("id") Long id)
    {
        try {
            webAdvertisementService.incrementImpressionCount(id);
            return AjaxResult.success("曝光统计成功");
        } catch (Exception e) {
            return AjaxResult.error("曝光统计失败");
        }
    }
    
    /**
     * 获取最新的公告内容
     * @param categoryType 分类类型（可选）：公告、投稿、回家地址
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/notice/latest")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getLatestAnnouncement(@org.springframework.web.bind.annotation.RequestParam(value = "categoryType", required = false) String categoryType)
    {
        SysNotice query = new SysNotice();
        query.setNoticeType("2"); // 2表示公告类型
        query.setStatus("0");     // 0表示正常状态
        
        // 如果传入了分类类型，则按分类类型筛选
        if (categoryType != null && !categoryType.trim().isEmpty()) {
            query.setCategoryType(categoryType);
        }
        
        List<SysNotice> list = noticeService.selectNoticeList(query);
        
        if (!list.isEmpty()) {
            // 按创建时间降序排序，返回最新的一条公告
            SysNotice latestNotice = list.stream()
                .sorted((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                })
                .findFirst()
                .orElse(null);
            return AjaxResult.success(latestNotice);
        } else {
            return AjaxResult.success(null);
        }
    }
} 
