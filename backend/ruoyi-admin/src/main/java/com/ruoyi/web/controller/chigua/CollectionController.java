package com.ruoyi.web.controller.chigua;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.service.ICollectionService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 合集管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/collection")
public class CollectionController extends BaseController
{
    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);
    
    @Autowired
    private ICollectionService collectionService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询合集列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:list')")
    @GetMapping("/list")
    public TableDataInfo list(Collection collection)
    {
        startPage();
        List<Collection> list = collectionService.selectCollectionList(collection);
        
        // 🔐 为合集列表中的封面图片生成签名URL
        for (Collection collectionItem : list) {
            if (collectionItem.getCoverImage() != null && !collectionItem.getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = chiguaUrlService.generateUrl(collectionItem.getCoverImage(), ChiguaUrlService.ResourceType.COVER);
                    if (signedCoverUrl != null) {
                        collectionItem.setCoverImage(signedCoverUrl);
                        logger.debug("✅ 合集{}封面URL已更新为签名URL: {}", collectionItem.getId(), signedCoverUrl);
                    }
                } catch (Exception e) {
                    logger.warn("❌ 为合集{}生成封面签名URL失败: {}", collectionItem.getId(), e.getMessage());
                }
            }
        }
        
        return getDataTable(list);
    }

    /**
     * 导出合集列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:export')")
    @Log(title = "合集管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Collection collection)
    {
        List<Collection> list = collectionService.selectCollectionList(collection);
        ExcelUtil<Collection> util = new ExcelUtil<Collection>(Collection.class);
        util.exportExcel(response, list, "合集数据");
    }

    /**
     * 获取合集详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        Collection collection = collectionService.selectCollectionById(id);
        
        // 🔐 为合集封面图片生成签名URL
        if (collection != null && collection.getCoverImage() != null && !collection.getCoverImage().trim().isEmpty()) {
            try {
                String signedCoverUrl = chiguaUrlService.generateUrl(collection.getCoverImage(), ChiguaUrlService.ResourceType.COVER);
                if (signedCoverUrl != null) {
                    collection.setCoverImage(signedCoverUrl);
                    logger.debug("✅ 合集{}封面URL已更新为签名URL: {}", id, signedCoverUrl);
                }
            } catch (Exception e) {
                logger.warn("❌ 为合集{}生成封面签名URL失败: {}", id, e.getMessage());
            }
        }
        
        return AjaxResult.success(collection);
    }

    /**
     * 新增合集
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:add')")
    @Log(title = "合集管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Collection collection)
    {
        return toAjax(collectionService.insertCollection(collection));
    }

    /**
     * 修改合集
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:edit')")
    @Log(title = "合集管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Collection collection)
    {
        return toAjax(collectionService.updateCollection(collection));
    }

    /**
     * 删除合集
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:remove')")
    @Log(title = "合集管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(collectionService.deleteCollectionByIds(ids));
    }

    /**
     * 查询合集中的视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:query')")
    @GetMapping("/{collectionId}/videos")
    public TableDataInfo getCollectionVideos(@PathVariable("collectionId") Long collectionId)
    {
        List<Video> list = collectionService.selectVideosByCollectionId(collectionId);
        
        // 🔐 为视频列表中的封面图片生成签名URL
        for (Video videoItem : list) {
            if (videoItem.getCoverImage() != null && !videoItem.getCoverImage().trim().isEmpty()) {
                try {
                    // 生成签名URL
                    String signedCoverUrl = chiguaUrlService.generateUrl(videoItem.getCoverImage(), ChiguaUrlService.ResourceType.COVER);
                    if (signedCoverUrl != null) {
                        videoItem.setCoverImage(signedCoverUrl);
                        logger.debug("✅ 视频{}封面URL已生成", videoItem.getId());
                    } else {
                        logger.warn("⚠️ 视频{}签名URL生成失败，保持原始URL", videoItem.getId());
                    }
                } catch (Exception e) {
                    logger.error("❌ 为视频{}生成封面URL失败: {}", videoItem.getId(), e.getMessage(), e);
                }
            }
        }
        
        return getDataTable(list);
    }

    /**
     * 查询不在指定合集中的视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:query')")
    @GetMapping("/{collectionId}/available-videos")
    public TableDataInfo getAvailableVideos(@PathVariable("collectionId") Long collectionId, Video video)
    {
        startPage();
        List<Video> list = collectionService.selectVideosNotInCollection(collectionId, video);
        
        // 🔐 为视频列表中的封面图片生成签名URL
        for (Video videoItem : list) {
            if (videoItem.getCoverImage() != null && !videoItem.getCoverImage().trim().isEmpty()) {
                try {
                    // 生成签名URL
                    String signedCoverUrl = chiguaUrlService.generateUrl(videoItem.getCoverImage(), ChiguaUrlService.ResourceType.COVER);
                    if (signedCoverUrl != null) {
                        videoItem.setCoverImage(signedCoverUrl);
                        logger.debug("✅ 视频{}封面URL已生成", videoItem.getId());
                    } else {
                        logger.warn("⚠️ 视频{}签名URL生成失败，保持原始URL", videoItem.getId());
                    }
                } catch (Exception e) {
                    logger.error("❌ 为视频{}生成封面URL失败: {}", videoItem.getId(), e.getMessage(), e);
                }
            }
        }
        
        return getDataTable(list);
    }

    /**
     * 添加视频到合集
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:edit')")
    @Log(title = "合集管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{collectionId}/videos")
    public AjaxResult addVideosToCollection(@PathVariable("collectionId") Long collectionId, @RequestBody List<Long> videoIds)
    {
        return toAjax(collectionService.addVideosToCollection(collectionId, videoIds));
    }

    /**
     * 从合集中移除视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:edit')")
    @Log(title = "合集管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{collectionId}/videos")
    public AjaxResult removeVideosFromCollection(@PathVariable("collectionId") Long collectionId, @RequestBody List<Long> videoIds)
    {
        return toAjax(collectionService.removeVideosFromCollection(collectionId, videoIds));
    }

    /**
     * 更新合集中视频的排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:edit')")
    @Log(title = "合集管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{collectionId}/videos/sort")
    public AjaxResult updateVideoSort(@PathVariable("collectionId") Long collectionId, @RequestBody List<Map<String, Object>> videoSortData)
    {
        return toAjax(collectionService.updateVideoSortInCollection(collectionId, videoSortData));
    }

    /**
     * 查询所有分类列表（用于下拉选择）
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:query')")
    @GetMapping("/categories")
    public AjaxResult getAllCategories()
    {
        List<Category> list = collectionService.selectAllCategories();
        return AjaxResult.success(list);
    }

    /**
     * 批量修改合集状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:collection:edit')")
    @Log(title = "合集管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{status}")
    public AjaxResult updateStatus(@PathVariable("status") Integer status, @RequestBody Long[] ids)
    {
        return toAjax(collectionService.updateCollectionStatus(ids, status));
    }

    /**
     * 更新合集观看次数
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/{collectionId}/view")
    public AjaxResult updateViewCount(@PathVariable("collectionId") Long collectionId)
    {
        return toAjax(collectionService.updateCollectionViewCount(collectionId, 1));
    }
} 