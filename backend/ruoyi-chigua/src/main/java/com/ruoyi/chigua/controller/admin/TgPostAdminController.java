package com.ruoyi.chigua.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.TgPost;
import com.ruoyi.chigua.domain.TgMedia;
import com.ruoyi.chigua.mapper.TgPostMapper;
import com.ruoyi.chigua.service.ChiguaUrlService;

/**
 * Telegram 帖子管理后台 Controller
 */
@RestController
@RequestMapping("/chigua/tgpost")
public class TgPostAdminController extends BaseController
{
    @Autowired
    private TgPostMapper tgPostMapper;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /** 帖子列表（分页） */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:list')")
    @GetMapping("/list")
    public TableDataInfo list(TgPost tgPost,
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String beginCreatedAt,
            @RequestParam(required = false) String endCreatedAt)
    {
        startPage();
        if (beginDate      != null && !beginDate.isEmpty())      tgPost.getParams().put("beginDate",      beginDate);
        if (endDate        != null && !endDate.isEmpty())        tgPost.getParams().put("endDate",        endDate);
        if (beginCreatedAt != null && !beginCreatedAt.isEmpty()) tgPost.getParams().put("beginCreatedAt", beginCreatedAt);
        if (endCreatedAt   != null && !endCreatedAt.isEmpty())   tgPost.getParams().put("endCreatedAt",   endCreatedAt);
        List<TgPost> list = tgPostMapper.selectTgPostList(tgPost);
        return getDataTable(list);
    }

    /** 查询帖子下所有媒体（展开行用），URL 已签名可直接访问 */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:list')")
    @GetMapping("/media/{postId}")
    public AjaxResult getMedia(@PathVariable Long postId)
    {
        List<TgMedia> media = tgPostMapper.selectTgMediaByPostId(postId);
        for (TgMedia m : media) {
            processMediaUrls(m);
        }
        return AjaxResult.success(media);
    }

    /** 修改帖子状态/排序等 */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:edit')")
    @Log(title = "TG帖子管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TgPost tgPost)
    {
        return toAjax(tgPostMapper.updateTgPost(tgPost));
    }

    /** 删除帖子（同时删除其所有媒体） */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:remove')")
    @Log(title = "TG帖子管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult removePosts(@PathVariable Long[] ids)
    {
        tgPostMapper.deleteTgMediaByPostIds(ids);
        tgPostMapper.deleteTgPostByIds(ids);
        return AjaxResult.success();
    }

    /** 删除单条媒体记录 */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:edit')")
    @Log(title = "TG媒体管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/media/{mediaId}")
    public AjaxResult removeMedia(@PathVariable Long mediaId)
    {
        tgPostMapper.deleteTgMediaById(mediaId);
        return AjaxResult.success();
    }

    /** 切换帖子置顶状态 */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:edit')")
    @Log(title = "TG帖子管理", businessType = BusinessType.UPDATE)
    @PutMapping("/top/{id}")
    public AjaxResult toggleTop(@PathVariable Long id, @RequestParam Integer isTop)
    {
        TgPost p = new TgPost();
        p.setId(id);
        p.setIsTop(isTop);
        return toAjax(tgPostMapper.updateTgPost(p));
    }

    /** 切换媒体推荐状态 */
    @PreAuthorize("@ss.hasPermi('chigua:tgpost:edit')")
    @Log(title = "TG媒体管理", businessType = BusinessType.UPDATE)
    @PutMapping("/media/recommend/{id}")
    public AjaxResult toggleRecommend(@PathVariable Long id, @RequestParam Integer isRecommend)
    {
        return toAjax(tgPostMapper.updateTgMediaRecommend(id, isRecommend));
    }

    // ── 私有：处理媒体 URL，添加签名 + decrypt=true ──────────────

    private void processMediaUrls(TgMedia m)
    {
        try {
            if ("photo".equals(m.getMediaType())) {
                m.setLocalUrl(workerUrl(m.getLocalUrl(), ChiguaUrlService.ResourceType.IMAGE));
            } else {
                m.setLocalUrl(workerUrl(m.getLocalUrl(), ChiguaUrlService.ResourceType.STREAM));
            }
            m.setThumbUrl(workerUrl(m.getThumbUrl(), ChiguaUrlService.ResourceType.THUMBNAIL));
        } catch (Exception e) {
            logger.warn("TgMedia {} URL 签名失败: {}", m.getId(), e.getMessage());
        }
    }

    private String workerUrl(String path, ChiguaUrlService.ResourceType type)
    {
        if (path == null || path.isEmpty() || path.startsWith("http")) return path;
        return chiguaUrlService.generateWorkerUrl(path, type);
    }
}
