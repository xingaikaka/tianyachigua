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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.VideoImage;
import com.ruoyi.chigua.service.IVideoImageService;

/**
 * 视频图片管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/videoImage")
public class VideoImageController extends BaseController
{
    @Autowired
    private IVideoImageService videoImageService;

    /**
     * 查询视频图片列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:list')")
    @GetMapping("/list")
    public TableDataInfo list(VideoImage videoImage)
    {
        startPage();
        List<VideoImage> list = videoImageService.selectVideoImageList(videoImage);
        return getDataTable(list);
    }

    /**
     * 根据视频ID查询图片列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:query')")
    @GetMapping("/video/{videoId}")
    public AjaxResult getImagesByVideoId(@PathVariable("videoId") Long videoId)
    {
        List<VideoImage> list = videoImageService.selectVideoImagesByVideoId(videoId);
        return success(list);
    }

    /**
     * 导出视频图片列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:export')")
    @Log(title = "视频图片管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VideoImage videoImage)
    {
        List<VideoImage> list = videoImageService.selectVideoImageList(videoImage);
        ExcelUtil<VideoImage> util = new ExcelUtil<VideoImage>(VideoImage.class);
        util.exportExcel(response, list, "视频图片数据");
    }

    /**
     * 获取视频图片详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(videoImageService.selectVideoImageById(id));
    }

    /**
     * 新增视频图片
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:add')")
    @Log(title = "视频图片管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VideoImage videoImage)
    {
        return toAjax(videoImageService.insertVideoImage(videoImage));
    }

    /**
     * 修改视频图片
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:edit')")
    @Log(title = "视频图片管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VideoImage videoImage)
    {
        return toAjax(videoImageService.updateVideoImage(videoImage));
    }

    /**
     * 删除视频图片
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:remove')")
    @Log(title = "视频图片管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(videoImageService.deleteVideoImageByIds(ids));
    }

    /**
     * 设置主图
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:edit')")
    @Log(title = "视频图片管理", businessType = BusinessType.UPDATE)
    @PutMapping("/setPrimary")
    public AjaxResult setPrimary(@RequestParam Long videoId, @RequestParam Long imageId)
    {
        return toAjax(videoImageService.setPrimaryImage(videoId, imageId));
    }

    /**
     * 获取视频的主图
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:query')")
    @GetMapping("/primary/{videoId}")
    public AjaxResult getPrimaryImage(@PathVariable("videoId") Long videoId)
    {
        VideoImage primaryImage = videoImageService.selectPrimaryImageByVideoId(videoId);
        return success(primaryImage);
    }

    /**
     * 统计视频图片数量
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:query')")
    @GetMapping("/count/{videoId}")
    public AjaxResult countImages(@PathVariable("videoId") Long videoId)
    {
        int count = videoImageService.countImagesByVideoId(videoId);
        return success(count);
    }

    /**
     * 批量更新图片排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:edit')")
    @Log(title = "视频图片管理", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult updateSort(@RequestBody List<VideoImage> images)
    {
        return toAjax(videoImageService.batchUpdateImageSort(images));
    }

    /**
     * 检查图片URL是否已存在
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:query')")
    @GetMapping("/checkUrl")
    public AjaxResult checkImageUrl(@RequestParam String imageUrl, @RequestParam Long videoId, 
                                   @RequestParam(required = false) Long excludeId)
    {
        boolean exists = videoImageService.checkImageUrlExists(imageUrl, videoId, excludeId);
        return success(exists);
    }

    /**
     * 批量上传图片
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoImage:add')")
    @Log(title = "视频图片管理", businessType = BusinessType.INSERT)
    @PostMapping("/batchUpload")
    public AjaxResult batchUpload(@RequestParam Long videoId, @RequestBody List<String> imageUrls)
    {
        return toAjax(videoImageService.batchInsertImages(videoId, imageUrls));
    }
} 