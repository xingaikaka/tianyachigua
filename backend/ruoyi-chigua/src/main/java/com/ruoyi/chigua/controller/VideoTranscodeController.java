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
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.mapper.VideoTranscodeMapper;

/**
 * 视频转码记录Controller
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@RestController
@RequestMapping("/chigua/transcode")
public class VideoTranscodeController extends BaseController
{
    @Autowired
    private IVideoTranscodeService videoTranscodeService;
    
    @Autowired
    private VideoTranscodeMapper videoTranscodeMapper;

    /**
     * 查询视频转码记录列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:list')")
    @GetMapping("/list")
    public TableDataInfo list(VideoTranscode videoTranscode)
    {
        startPage();
        List<VideoTranscode> list = videoTranscodeService.selectVideoTranscodeList(videoTranscode);
        return getDataTable(list);
    }

    /**
     * 查询未使用的转码记录列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:list')")
    @GetMapping("/unused")
    public TableDataInfo listUnused(VideoTranscode videoTranscode)
    {
        startPage();
        List<VideoTranscode> list = videoTranscodeService.selectUnusedTranscodes(videoTranscode);
        return getDataTable(list);
    }

    /**
     * 导出视频转码记录列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:export')")
    @Log(title = "视频转码记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VideoTranscode videoTranscode)
    {
        List<VideoTranscode> list = videoTranscodeService.selectVideoTranscodeList(videoTranscode);
        ExcelUtil<VideoTranscode> util = new ExcelUtil<VideoTranscode>(VideoTranscode.class);
        util.exportExcel(response, list, "视频转码记录数据");
    }

    /**
     * 获取视频转码记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(videoTranscodeService.selectVideoTranscodeById(id));
    }

    /**
     * 根据转码ID获取转码记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:query')")
    @GetMapping(value = "/transcode/{transcodeId}")
    public AjaxResult getInfoByTranscodeId(@PathVariable("transcodeId") String transcodeId)
    {
        return success(videoTranscodeService.selectVideoTranscodeByTranscodeId(transcodeId));
    }

    /**
     * 新增视频转码记录
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:add')")
    @Log(title = "视频转码记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VideoTranscode videoTranscode)
    {
        return toAjax(videoTranscodeService.insertVideoTranscode(videoTranscode));
    }

    /**
     * 修改视频转码记录
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:edit')")
    @Log(title = "视频转码记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VideoTranscode videoTranscode)
    {
        return toAjax(videoTranscodeService.updateVideoTranscode(videoTranscode));
    }

    /**
     * 删除视频转码记录
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:remove')")
    @Log(title = "视频转码记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(videoTranscodeService.deleteVideoTranscodeByIds(ids));
    }

    /**
     * 标记转码记录为已使用
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:edit')")
    @Log(title = "标记转码记录为已使用", businessType = BusinessType.UPDATE)
    @PutMapping("/markUsed/{transcodeId}/{videoId}")
    public AjaxResult markAsUsed(@PathVariable("transcodeId") String transcodeId, 
                                @PathVariable("videoId") Long videoId)
    {
        return toAjax(videoTranscodeService.markTranscodeAsUsed(transcodeId, videoId));
    }

    /**
     * 获取可用于富文本选择的转码记录列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:list')")
    @GetMapping("/richtext/list")
    public TableDataInfo getRichTextVideoList(VideoTranscode videoTranscode,
                                             @RequestParam(value = "showUsed", defaultValue = "false") Boolean showUsed)
    {
        startPage();
        // 只查询转码成功的记录
        videoTranscode.setTranscodeResult("ok");
        
        logger.info("🔍 接收到参数: showUsed={}, orgfile={}, resolution={}", 
            showUsed, videoTranscode.getOrgfile(), videoTranscode.getResolution());
        
        List<VideoTranscode> list;
        if (showUsed != null && showUsed) {
            // 显示所有视频（包含使用状态标记）
            logger.info("🔍 调用 selectAllTranscodesWithUsageStatus");
            list = videoTranscodeService.selectAllTranscodesWithUsageStatus(videoTranscode);
            logger.info("🔍 查询所有视频，共 {} 条", list.size());
        } else {
            // 默认：只显示未使用的视频
            logger.info("🔍 调用 selectUnusedTranscodes");
            list = videoTranscodeService.selectUnusedTranscodes(videoTranscode);
            logger.info("🔍 查询未使用视频，共 {} 条", list.size());
        }
        
        // 添加调试信息
        if (!list.isEmpty()) {
            VideoTranscode first = list.get(0);
            logger.info("🔍 第一条记录: transcodeId={}, orgfile={}, isUsed={}", 
                first.getTranscodeId(), first.getOrgfile(), first.getIsUsed());
        } else {
            logger.warn("⚠️ 查询结果为空！");
        }
        
        return getDataTable(list);
    }

    /**
     * 测试接口：检查关联状态
     */
    @GetMapping("/test/association")
    public AjaxResult testAssociation()
    {
        try {
            logger.info("🔍 开始数据库连接测试...");
            
            // 使用SQL直接查询统计数据
            List<java.util.Map<String, Object>> results = videoTranscodeMapper.testAssociationQuery();
            
            logger.info("🔍 数据库统计结果:");
            for (java.util.Map<String, Object> row : results) {
                logger.info("  {}", row);
            }
            
            // 先查询几条video_transcodes记录
            VideoTranscode query = new VideoTranscode();
            query.setTranscodeResult("ok");
            List<VideoTranscode> allTranscodes = videoTranscodeService.selectVideoTranscodeList(query);
            
            logger.info("🔍 Service层查询 - 总转码记录数: {}", allTranscodes.size());
            if (!allTranscodes.isEmpty()) {
                VideoTranscode first = allTranscodes.get(0);
                logger.info("🔍 第一条转码记录: transcodeId='{}', 长度={}", 
                    first.getTranscodeId(), first.getTranscodeId().length());
            }
            
            // 查询未使用的
            List<VideoTranscode> unusedTranscodes = videoTranscodeService.selectUnusedTranscodes(query);
            logger.info("🔍 Service层查询 - 未使用记录数: {}", unusedTranscodes.size());
            
            // 查询所有带状态的
            List<VideoTranscode> allWithStatus = videoTranscodeService.selectAllTranscodesWithUsageStatus(query);
            logger.info("🔍 Service层查询 - 所有记录数（带状态）: {}", allWithStatus.size());
            
            if (!allWithStatus.isEmpty()) {
                for (int i = 0; i < Math.min(3, allWithStatus.size()); i++) {
                    VideoTranscode vt = allWithStatus.get(i);
                    logger.info("🔍 记录{}: transcodeId='{}', isUsed={}", 
                        i+1, vt.getTranscodeId(), vt.getIsUsed());
                }
            }
            
            return AjaxResult.success("测试完成，请查看后端日志", results);
        } catch (Exception e) {
            logger.error("❌ 测试失败", e);
            return AjaxResult.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 生成视频富文本HTML代码
     * @deprecated 请使用新的VideoTranscodeUrlApiController的预览API
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:query')")
    @PostMapping("/richtext/generateHtml")
    public AjaxResult generateVideoHtml(@RequestBody VideoTranscode videoTranscode)
    {
        try {
            // 支持通过ID或transcodeId查找
            VideoTranscode transcode = null;
            if (videoTranscode.getId() != null) {
                transcode = videoTranscodeService.selectVideoTranscodeById(videoTranscode.getId());
            } else if (videoTranscode.getTranscodeId() != null) {
                transcode = videoTranscodeService.selectVideoTranscodeByTranscodeId(videoTranscode.getTranscodeId());
            }
            
            if (transcode == null) {
                return error("转码记录不存在");
            }
            
            String htmlCode = videoTranscodeService.generateVideoHtmlForRichText(transcode.getTranscodeId());
            return AjaxResult.success("生成成功", htmlCode);
        } catch (Exception e) {
            return error("生成视频HTML失败：" + e.getMessage());
        }
    }

    /**
     * 获取转码视频的播放URL（带签名）
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:query')")
    @GetMapping("/richtext/videoUrl/{transcodeId}")
    public AjaxResult getVideoUrl(@PathVariable("transcodeId") String transcodeId)
    {
        try {
            String videoUrl = videoTranscodeService.getVideoUrlForRichText(transcodeId);
            String posterUrl = videoTranscodeService.getPosterUrlForRichText(transcodeId);
            
            java.util.Map<String, String> urls = new java.util.HashMap<>();
            urls.put("videoUrl", videoUrl);
            urls.put("posterUrl", posterUrl);
            
            return success(urls);
        } catch (Exception e) {
            return error("获取视频URL失败：" + e.getMessage());
        }
    }

    /**
     * 批量获取转码视频的播放URL（带签名）
     */
    @PreAuthorize("@ss.hasPermi('chigua:transcode:query')")
    @PostMapping("/richtext/videoUrls")
    public AjaxResult getVideoUrls(@RequestBody java.util.List<String> transcodeIds)
    {
        try {
            java.util.Map<String, java.util.Map<String, String>> urlsMap = new java.util.HashMap<>();
            
            for (String transcodeId : transcodeIds) {
                try {
                    String videoUrl = videoTranscodeService.getVideoUrlForRichText(transcodeId);
                    String posterUrl = videoTranscodeService.getPosterUrlForRichText(transcodeId);
                    
                    java.util.Map<String, String> urls = new java.util.HashMap<>();
                    urls.put("videoUrl", videoUrl);
                    urls.put("posterUrl", posterUrl);
                    
                    urlsMap.put(transcodeId, urls);
                } catch (Exception e) {
                    logger.warn("获取转码ID {}的URL失败: {}", transcodeId, e.getMessage());
                }
            }
            
            return success(urlsMap);
        } catch (Exception e) {
            return error("批量获取视频URL失败：" + e.getMessage());
        }
    }
} 