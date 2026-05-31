package com.ruoyi.chigua.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.RedgifsVideo;
import com.ruoyi.chigua.service.IRedgifsVideoService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * RedGifs视频信息Controller
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@RestController
@RequestMapping("/web/api/redgifs/videos")
public class RedgifsVideoController extends BaseController
{
    @Autowired
    private IRedgifsVideoService redgifsVideoService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询RedGifs视频信息列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:list')")
    @GetMapping("/list")
    public TableDataInfo list(RedgifsVideo redgifsVideo)
    {
        startPage();
        List<RedgifsVideo> list = redgifsVideoService.selectRedGifsVideoList(redgifsVideo);
        
        // 为每个视频的URL生成签名
        for (RedgifsVideo video : list) {
            processVideoUrls(video);
        }
        
        return getDataTable(list);
    }

    /**
     * 导出RedGifs视频信息列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:export')")
    @Log(title = "RedGifs视频信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RedgifsVideo redgifsVideo)
    {
        List<RedgifsVideo> list = redgifsVideoService.selectRedGifsVideoList(redgifsVideo);
        ExcelUtil<RedgifsVideo> util = new ExcelUtil<RedgifsVideo>(RedgifsVideo.class);
        util.exportExcel(response, list, "RedGifs视频数据");
    }

    /**
     * 获取RedGifs视频信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        RedgifsVideo video = redgifsVideoService.selectRedGifsVideoById(id);
        if (video != null) {
            processVideoUrls(video);
        }
        return success(video);
    }

    /**
     * 根据gifId获取RedGifs视频信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:query')")
    @GetMapping(value = "/gif/{gifId}")
    public AjaxResult getInfoByGifId(@PathVariable("gifId") String gifId)
    {
        RedgifsVideo video = redgifsVideoService.selectRedGifsVideoByGifId(gifId);
        if (video != null) {
            processVideoUrls(video);
        }
        return success(video);
    }

    /**
     * 新增RedGifs视频信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:add')")
    @Log(title = "RedGifs视频信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RedgifsVideo redgifsVideo)
    {
        return toAjax(redgifsVideoService.insertRedGifsVideo(redgifsVideo));
    }

    /**
     * 修改RedGifs视频信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:edit')")
    @Log(title = "RedGifs视频信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RedgifsVideo redgifsVideo)
    {
        return toAjax(redgifsVideoService.updateRedGifsVideo(redgifsVideo));
    }

    /**
     * 删除RedGifs视频信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:remove')")
    @Log(title = "RedGifs视频信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(redgifsVideoService.deleteRedGifsVideoByIds(ids));
    }

    /**
     * 批量检查视频是否存在
     */
    @PostMapping("/check-batch")
    public AjaxResult checkVideosExist(@RequestBody Map<String, List<String>> request)
    {
        List<String> gifIds = request.get("gifIds");
        if (gifIds == null || gifIds.isEmpty()) {
            return error("视频ID列表不能为空");
        }
        Map<String, Map<String, Object>> result = redgifsVideoService.checkRedgifsVideosExist(gifIds);
        return success(result);
    }

    /**
     * 入库RedGifs视频
     */
    @PostMapping("/ingest")
    public AjaxResult ingestVideo(@RequestBody RedgifsVideo redgifsVideo)
    {
        if (redgifsVideo.getGifId() == null || redgifsVideo.getGifId().isEmpty()) {
            return error("视频ID不能为空");
        }
        if (redgifsVideo.getUserId() == null || redgifsVideo.getUserId() <= 0) {
            return error("用户ID不能为空");
        }
        Map<String, Object> result = redgifsVideoService.ingestRedgifsVideo(redgifsVideo);
        return success(result);
    }
    
    /**
     * 为视频的URL生成签名
     */
    private void processVideoUrls(RedgifsVideo video) {
        try {
            // 处理高清视频URL
            if (video.getHdUrl() != null && !video.getHdUrl().isEmpty() 
                && !video.getHdUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getHdUrl(), 
                    ChiguaUrlService.ResourceType.VIDEO, 
                    false
                );
                video.setHdUrl(signedUrl);
            }
            
            // 处理标清视频URL
            if (video.getSdUrl() != null && !video.getSdUrl().isEmpty() 
                && !video.getSdUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getSdUrl(), 
                    ChiguaUrlService.ResourceType.VIDEO, 
                    false
                );
                video.setSdUrl(signedUrl);
            }
            
            // 处理封面图URL
            if (video.getPosterUrl() != null && !video.getPosterUrl().isEmpty() 
                && !video.getPosterUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getPosterUrl(), 
                    ChiguaUrlService.ResourceType.COVER, 
                    false
                );
                video.setPosterUrl(signedUrl);
            }
            
            // 处理缩略图URL
            if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty() 
                && !video.getThumbnailUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getThumbnailUrl(), 
                    ChiguaUrlService.ResourceType.THUMBNAIL, 
                    false
                );
                video.setThumbnailUrl(signedUrl);
            }
        } catch (Exception e) {
            // 签名失败不影响数据返回，只记录日志
            logger.warn("为视频{}生成URL签名失败: {}", video.getGifId(), e.getMessage());
        }
    }
}
