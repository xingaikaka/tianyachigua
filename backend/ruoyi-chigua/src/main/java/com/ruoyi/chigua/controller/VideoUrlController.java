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
import com.ruoyi.chigua.domain.VideoUrl;
import com.ruoyi.chigua.service.IVideoUrlService;

/**
 * 视频地址管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/videoUrl")
public class VideoUrlController extends BaseController
{
    @Autowired
    private IVideoUrlService videoUrlService;

    /**
     * 查询视频地址列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:list')")
    @GetMapping("/list")
    public TableDataInfo list(VideoUrl videoUrl)
    {
        startPage();
        List<VideoUrl> list = videoUrlService.selectVideoUrlList(videoUrl);
        return getDataTable(list);
    }

    /**
     * 根据视频ID查询地址列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/video/{videoId}")
    public AjaxResult getUrlsByVideoId(@PathVariable("videoId") Long videoId)
    {
        List<VideoUrl> list = videoUrlService.selectVideoUrlsByVideoId(videoId);
        return success(list);
    }

    /**
     * 导出视频地址列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:export')")
    @Log(title = "视频地址管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VideoUrl videoUrl)
    {
        List<VideoUrl> list = videoUrlService.selectVideoUrlList(videoUrl);
        ExcelUtil<VideoUrl> util = new ExcelUtil<VideoUrl>(VideoUrl.class);
        util.exportExcel(response, list, "视频地址数据");
    }

    /**
     * 获取视频地址详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(videoUrlService.selectVideoUrlById(id));
    }

    /**
     * 新增视频地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:add')")
    @Log(title = "视频地址管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VideoUrl videoUrl)
    {
        return toAjax(videoUrlService.insertVideoUrl(videoUrl));
    }

    /**
     * 修改视频地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:edit')")
    @Log(title = "视频地址管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VideoUrl videoUrl)
    {
        return toAjax(videoUrlService.updateVideoUrl(videoUrl));
    }

    /**
     * 删除视频地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:remove')")
    @Log(title = "视频地址管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(videoUrlService.deleteVideoUrlByIds(ids));
    }

    /**
     * 设置主要地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:edit')")
    @Log(title = "视频地址管理", businessType = BusinessType.UPDATE)
    @PutMapping("/setPrimary")
    public AjaxResult setPrimary(@RequestParam Long videoId, @RequestParam Long urlId)
    {
        return toAjax(videoUrlService.setPrimaryUrl(videoId, urlId));
    }

    /**
     * 获取视频的主要地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/primary/{videoId}")
    public AjaxResult getPrimaryUrl(@PathVariable("videoId") Long videoId)
    {
        VideoUrl primaryUrl = videoUrlService.selectPrimaryUrlByVideoId(videoId);
        return success(primaryUrl);
    }

    /**
     * 统计视频地址数量
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/count/{videoId}")
    public AjaxResult countUrls(@PathVariable("videoId") Long videoId)
    {
        int count = videoUrlService.countUrlsByVideoId(videoId);
        return success(count);
    }

    /**
     * 批量更新地址排序
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:edit')")
    @Log(title = "视频地址管理", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult updateSort(@RequestBody List<VideoUrl> urls)
    {
        return toAjax(videoUrlService.batchUpdateUrlSort(urls));
    }

    /**
     * 检查播放地址是否已存在
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/checkUrl")
    public AjaxResult checkVideoUrl(@RequestParam String videoUrl, @RequestParam Long videoId, 
                                   @RequestParam(required = false) Long excludeId)
    {
        boolean exists = videoUrlService.checkVideoUrlExists(videoUrl, videoId, excludeId);
        return success(exists);
    }

    /**
     * 更新播放次数
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:edit')")
    @Log(title = "视频地址管理", businessType = BusinessType.UPDATE)
    @PutMapping("/play/{id}")
    public AjaxResult incrementPlayCount(@PathVariable("id") Long id)
    {
        return toAjax(videoUrlService.incrementPlayCount(id));
    }

    /**
     * 根据清晰度查询地址列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/quality")
    public AjaxResult getUrlsByQuality(@RequestParam Long videoId, @RequestParam String quality)
    {
        List<VideoUrl> list = videoUrlService.selectVideoUrlsByQuality(videoId, quality);
        return success(list);
    }

    /**
     * 根据格式查询地址列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/format")
    public AjaxResult getUrlsByFormat(@RequestParam Long videoId, @RequestParam String format)
    {
        List<VideoUrl> list = videoUrlService.selectVideoUrlsByFormat(videoId, format);
        return success(list);
    }

    /**
     * 批量导入视频地址
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:add')")
    @Log(title = "视频地址管理", businessType = BusinessType.INSERT)
    @PostMapping("/batchImport")
    public AjaxResult batchImport(@RequestParam Long videoId, @RequestBody List<VideoUrl> urls)
    {
        return toAjax(videoUrlService.batchInsertUrls(videoId, urls));
    }

    /**
     * 测试链接可用性
     */
    @PreAuthorize("@ss.hasPermi('chigua:videoUrl:query')")
    @GetMapping("/test")
    public AjaxResult testUrl(@RequestParam String videoUrl)
    {
        boolean available = videoUrlService.testVideoUrl(videoUrl);
        return success(available);
    }
} 