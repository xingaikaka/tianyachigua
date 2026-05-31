package com.ruoyi.chigua.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.service.IAdvertisementService;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.ChiguaUrlService;

/**
 * 广告管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/advertisement")
public class AdvertisementController extends BaseController
{
    @Autowired
    private IAdvertisementService advertisementService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询广告列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:list')")
    @GetMapping("/list")
    public TableDataInfo list(Advertisement advertisement)
    {
        startPage();
        List<Advertisement> list = advertisementService.selectAdvertisementList(advertisement);
        return getDataTable(list);
    }

    /**
     * 导出广告列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:export')")
    @Log(title = "广告管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Advertisement advertisement)
    {
        List<Advertisement> list = advertisementService.selectAdvertisementList(advertisement);
        ExcelUtil<Advertisement> util = new ExcelUtil<Advertisement>(Advertisement.class);
        util.exportExcel(response, list, "广告数据");
    }

    /**
     * 获取广告详细信息（后台编辑用，imageUrl 转换为签名URL方便预览）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        Advertisement advertisement = advertisementService.selectAdvertisementById(id);
        if (advertisement != null) {
            String imageUrl = advertisement.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()
                    && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                // R2 相对路径 → 签名URL，供后台图片预览组件直接显示
                try {
                    String signedUrl = chiguaUrlService.generateWorkerUrl(imageUrl, ChiguaUrlService.ResourceType.IMAGE);
                    if (signedUrl != null) {
                        advertisement.setImageUrl(signedUrl);
                    }
                } catch (Exception e) {
                    logger.warn("广告{}封面生成签名URL失败: {}", id, e.getMessage());
                }
            }
        }
        return success(advertisement);
    }

    /**
     * 新增广告
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:add')")
    @Log(title = "广告管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Advertisement advertisement)
    {
        if (!advertisementService.checkAdvertisementTitleUnique(advertisement))
        {
            return error("新增广告'" + advertisement.getTitle() + "'失败，广告标题已存在");
        }
        return toAjax(advertisementService.insertAdvertisement(advertisement));
    }

    /**
     * 修改广告（imageUrl 若为签名CDN URL则提取 key 参数后再存库）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:edit')")
    @Log(title = "广告管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Advertisement advertisement)
    {
        // 将签名CDN URL还原为R2资源key，避免把签名URL持久化到数据库
        String imageUrl = advertisement.getImageUrl();
        if (imageUrl != null && imageUrl.startsWith("http") && imageUrl.contains("key=")) {
            try {
                java.net.URL urlObj = new java.net.URL(imageUrl);
                String query = urlObj.getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("key=")) {
                            String resourceKey = java.net.URLDecoder.decode(param.substring(4), "UTF-8");
                            advertisement.setImageUrl(resourceKey);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("解析广告imageUrl签名参数失败，保持原值: {}", e.getMessage());
            }
        }
        if (!advertisementService.checkAdvertisementTitleUnique(advertisement))
        {
            return error("修改广告'" + advertisement.getTitle() + "'失败，广告标题已存在");
        }
        return toAjax(advertisementService.updateAdvertisement(advertisement));
    }

    /**
     * 删除广告
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:remove')")
    @Log(title = "广告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(advertisementService.deleteAdvertisementByIds(ids));
    }

    /**
     * 获取分类列表（用于广告编辑时选择分类）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:query')")
    @GetMapping("/categoryList")
    public AjaxResult getCategoryList()
    {
        Category category = new Category();
        category.setStatus(1); // 只查询启用状态的分类
        List<Category> list = categoryService.selectCategoryList(category);
        return success(list);
    }

    /**
     * 按位置查询有效广告（前端展示接口）
     */
    @GetMapping("/position/{position}")
    public AjaxResult getAdvertisementByPosition(@PathVariable("position") String position)
    {
        List<Advertisement> list = advertisementService.selectAdvertisementByPosition(position);
        return success(list);
    }

    /**
     * 按分类查询有效广告（前端展示接口）
     */
    @GetMapping("/category/{categoryId}")
    public AjaxResult getAdvertisementByCategory(@PathVariable("categoryId") Long categoryId)
    {
        List<Advertisement> list = advertisementService.selectAdvertisementByCategory(categoryId);
        return success(list);
    }

    /**
     * 广告点击统计（旧接口，请使用 /web/api/advertisement/click/{id}）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:query')")
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/click/{id}")
    public AjaxResult clickAdvertisement(@PathVariable("id") Long id)
    {
        return toAjax(advertisementService.clickAdvertisement(id));
    }

    /**
     * 广告展示统计（旧接口，请使用 /web/api/advertisement/impression/{id}）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:query')")
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @PostMapping("/impression/{id}")
    public AjaxResult impressionAdvertisement(@PathVariable("id") Long id)
    {
        return toAjax(advertisementService.impressionAdvertisement(id));
    }

    /**
     * 获取短视频广告（前端展示接口）
     * 根据分类ID获取短视频广告类型（adType=5）的广告列表
     */
    @GetMapping("/short-video/{categoryId}")
    public AjaxResult getShortVideoAds(@PathVariable("categoryId") Long categoryId)
    {
        List<Advertisement> list = advertisementService.selectShortVideoAdsByCategory(categoryId);
        return success(list);
    }
} 