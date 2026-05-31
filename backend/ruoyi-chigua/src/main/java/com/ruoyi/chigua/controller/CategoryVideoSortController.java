package com.ruoyi.chigua.controller;

import java.util.List;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.CategoryVideoSort;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.service.ICategoryVideoSortService;
import com.ruoyi.chigua.service.ICategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分类视频排序Controller
 * 
 * @author ruoyi
 * @date 2025-01-18
 */
@RestController
@RequestMapping("/chigua/categoryVideoSort")
public class CategoryVideoSortController extends BaseController
{
    private static final Logger logger = LoggerFactory.getLogger(CategoryVideoSortController.class);

    @Autowired
    private ICategoryVideoSortService categoryVideoSortService;

    @Autowired
    private ICategoryService categoryService;

    /**
     * 查询分类视频排序列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:list')")
    @GetMapping("/list")
    public TableDataInfo list(CategoryVideoSort categoryVideoSort)
    {
        startPage();
        List<CategoryVideoSort> list = categoryVideoSortService.selectCategoryVideoSortList(categoryVideoSort);
        return getDataTable(list);
    }

    /**
     * 根据分类ID查询排序视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:list')")
    @GetMapping("/listByCategory/{categoryId}")
    public AjaxResult listByCategory(@PathVariable("categoryId") Long categoryId)
    {
        List<CategoryVideoSort> list = categoryVideoSortService.selectSortedVideosByCategory(categoryId);
        return success(list);
    }

    /**
     * 获取分类下可添加排序的视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:list')")
    @GetMapping("/availableVideos/{categoryId}")
    public AjaxResult getAvailableVideos(@PathVariable("categoryId") Long categoryId, 
                                       @RequestParam(value = "title", required = false) String title,
                                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                       @RequestParam(value = "pageSize", required = false) Integer pageSize)
    {
        try {
            List<Video> list = categoryVideoSortService.getAvailableVideosForSort(categoryId, title, pageNum, pageSize);
            logger.info("✅ 获取分类{}可添加视频列表成功，共{}条记录", categoryId, list.size());
            return success(list);
        } catch (Exception e) {
            logger.error("❌ 获取分类{}可添加视频列表失败", categoryId, e);
            return error("获取可添加视频列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有分类列表
     */
    // @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:list')")  // 临时移除权限检查
    @GetMapping("/categories")
    public AjaxResult getCategories()
    {
        try {
            Category category = new Category();
            category.setStatus(1); // 只获取启用的分类
            category.setIsCollection(0); // 排除合集类型的分类
            List<Category> list = categoryService.selectCategoryList(category);
            logger.info("✅ 获取分类列表成功，共{}个分类（已排除合集类型）", list.size());
            return success(list);
        } catch (Exception e) {
            logger.error("❌ 获取分类列表失败", e);
            return error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 导出分类视频排序列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:export')")
    @Log(title = "分类视频排序", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CategoryVideoSort categoryVideoSort)
    {
        List<CategoryVideoSort> list = categoryVideoSortService.selectCategoryVideoSortList(categoryVideoSort);
        ExcelUtil<CategoryVideoSort> util = new ExcelUtil<CategoryVideoSort>(CategoryVideoSort.class);
        util.exportExcel(response, list, "分类视频排序数据");
    }

    /**
     * 获取分类视频排序详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(categoryVideoSortService.selectCategoryVideoSortById(id));
    }

    /**
     * 新增分类视频排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:add')")
    @Log(title = "分类视频排序", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CategoryVideoSort categoryVideoSort)
    {
        return toAjax(categoryVideoSortService.insertCategoryVideoSort(categoryVideoSort));
    }

    /**
     * 修改分类视频排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:edit')")
    @Log(title = "分类视频排序", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CategoryVideoSort categoryVideoSort)
    {
        return toAjax(categoryVideoSortService.updateCategoryVideoSort(categoryVideoSort));
    }

    /**
     * 批量设置分类视频排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:edit')")
    @Log(title = "批量设置分类视频排序", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSet")
    public AjaxResult batchSet(@RequestBody BatchSetRequest request)
    {
        if (request.getCategoryId() == null || request.getVideoSorts() == null || request.getVideoSorts().isEmpty()) {
            return error("参数不能为空");
        }

        // 构建排序数据
        List<CategoryVideoSort> videoSorts = new ArrayList<>();
        for (int i = 0; i < request.getVideoSorts().size(); i++) {
            VideoSortItem item = request.getVideoSorts().get(i);
            CategoryVideoSort sort = new CategoryVideoSort();
            sort.setCategoryId(request.getCategoryId());
            sort.setVideoId(item.getVideoId());
            // 排序权重：数字越大排序越靠前，从最大值开始递减
            sort.setSortOrder(1000 - i);
            videoSorts.add(sort);
        }

        int result = categoryVideoSortService.batchSetCategoryVideoSort(request.getCategoryId(), videoSorts);
        return toAjax(result);
    }

    /**
     * 删除分类视频排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:remove')")
    @Log(title = "分类视频排序", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(categoryVideoSortService.deleteCategoryVideoSortByIds(ids));
    }

    /**
     * 移除分类视频排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:categoryVideoSort:remove')")
    @Log(title = "移除分类视频排序", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove/{categoryId}/{videoId}")
    public AjaxResult removeCategoryVideoSort(@PathVariable("categoryId") Long categoryId, 
                                            @PathVariable("videoId") Long videoId)
    {
        return toAjax(categoryVideoSortService.removeCategoryVideoSort(categoryId, videoId));
    }

    /**
     * 批量设置请求对象
     */
    public static class BatchSetRequest {
        private Long categoryId;
        private List<VideoSortItem> videoSorts;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public List<VideoSortItem> getVideoSorts() {
            return videoSorts;
        }

        public void setVideoSorts(List<VideoSortItem> videoSorts) {
            this.videoSorts = videoSorts;
        }
    }

    /**
     * 视频排序项
     */
    public static class VideoSortItem {
        private Long videoId;
        private String title;

        public Long getVideoId() {
            return videoId;
        }

        public void setVideoId(Long videoId) {
            this.videoId = videoId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
