package com.ruoyi.chigua.controller.admin;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.chigua.service.ChiguaUrlService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.RedgifsVideo;
import com.ruoyi.chigua.service.IRedgifsVideoService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * RedGifs视频管理Controller（管理后台）
 * 
 * @author ruoyi
 * @date 2026-02-13
 */
@RestController
@RequestMapping("/chigua/redgifs/video")
public class RedgifsVideoAdminController extends BaseController
{
    @Autowired
    private IRedgifsVideoService redgifsVideoService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询RedGifs视频列表（根据用户ID）
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:list')")
    @GetMapping("/list")
    public TableDataInfo list(RedgifsVideo redgifsVideo)
    {
        startPage();
        // 使用userId进行过滤
        List<RedgifsVideo> list = redgifsVideoService.selectRedGifsVideoList(redgifsVideo);
        
        logger.info("查询到 {} 个视频（用户ID: {}）", list.size(), redgifsVideo.getUserId());
        
        // 为每个视频的URL生成签名
        for (RedgifsVideo video : list) {
            logger.info("视频 {} 原始封面URL: {}", video.getGifId(), video.getPosterUrl());
            logger.info("视频 {} 原始HD URL: {}", video.getGifId(), video.getHdUrl());
            processVideoUrls(video);
            logger.info("视频 {} 处理后封面URL: {}", video.getGifId(), video.getPosterUrl());
            logger.info("视频 {} 处理后HD URL: {}", video.getGifId(), video.getHdUrl());
        }
        
        return getDataTable(list);
    }

    /**
     * 查询所有RedGifs视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:list')")
    @GetMapping("/list/all")
    public TableDataInfo listAll(RedgifsVideo redgifsVideo)
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
     * 导出RedGifs视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:export')")
    @Log(title = "RedGifs视频", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RedgifsVideo redgifsVideo)
    {
        List<RedgifsVideo> list = redgifsVideoService.selectRedGifsVideoList(redgifsVideo);
        ExcelUtil<RedgifsVideo> util = new ExcelUtil<RedgifsVideo>(RedgifsVideo.class);
        util.exportExcel(response, list, "RedGifs视频数据");
    }

    /**
     * 获取RedGifs视频详细信息
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
     * 新增RedGifs视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:add')")
    @Log(title = "RedGifs视频", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RedgifsVideo redgifsVideo)
    {
        return toAjax(redgifsVideoService.insertRedGifsVideo(redgifsVideo));
    }

    /**
     * 修改RedGifs视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:edit')")
    @Log(title = "RedGifs视频", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RedgifsVideo redgifsVideo)
    {
        return toAjax(redgifsVideoService.updateRedGifsVideo(redgifsVideo));
    }

    /**
     * 修改视频状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:edit')")
    @Log(title = "RedGifs视频状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody RedgifsVideo redgifsVideo)
    {
        redgifsVideo.setStatus(redgifsVideo.getStatus());
        return toAjax(redgifsVideoService.updateRedGifsVideo(redgifsVideo));
    }

    /**
     * 删除RedGifs视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:remove')")
    @Log(title = "RedGifs视频", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(redgifsVideoService.deleteRedGifsVideoByIds(ids));
    }

    /**
     * 将视频封面同步为用户头像
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:video:edit')")
    @Log(title = "RedGifs视频-同步封面到用户头像", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/sync-poster-to-user")
    public AjaxResult syncPosterToUser(@PathVariable("id") Long id)
    {
        java.util.Map<String, Object> result = redgifsVideoService.syncPosterToUserAvatar(id);
        Boolean success = (Boolean) result.get("success");
        String message = (String) result.get("message");
        if (Boolean.TRUE.equals(success)) {
            return success(message);
        }
        return error(message);
    }
    
    /**
     * 为视频的URL生成 Worker 签名（管理后台始终走 tycgimage1.org，服务端解密）
     */
    private void processVideoUrls(RedgifsVideo video) {
        try {
            if (video.getHdUrl() != null && !video.getHdUrl().isEmpty()
                    && !video.getHdUrl().startsWith("http")) {
                video.setHdUrl(chiguaUrlService.generateWorkerUrl(video.getHdUrl(), ChiguaUrlService.ResourceType.STREAM));
            }
            if (video.getSdUrl() != null && !video.getSdUrl().isEmpty()
                    && !video.getSdUrl().startsWith("http")) {
                video.setSdUrl(chiguaUrlService.generateWorkerUrl(video.getSdUrl(), ChiguaUrlService.ResourceType.STREAM));
            }
            if (video.getPosterUrl() != null && !video.getPosterUrl().isEmpty()
                    && !video.getPosterUrl().startsWith("http")) {
                video.setPosterUrl(chiguaUrlService.generateWorkerUrl(video.getPosterUrl(), ChiguaUrlService.ResourceType.COVER));
            }
            if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty()
                    && !video.getThumbnailUrl().startsWith("http")) {
                video.setThumbnailUrl(chiguaUrlService.generateWorkerUrl(video.getThumbnailUrl(), ChiguaUrlService.ResourceType.THUMBNAIL));
            }
        } catch (Exception e) {
            logger.warn("为视频{}生成URL签名失败: {}", video.getGifId(), e.getMessage());
        }
    }
}
