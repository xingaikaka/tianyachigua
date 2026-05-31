package com.ruoyi.chigua.controller;

import java.util.ArrayList;
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
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.dto.VideoWithUrlsDto;

/**
 * 视频管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/video")
public class VideoController extends BaseController
{
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    
    @Autowired
    private IVideoService videoService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @GetMapping("/list")
    public TableDataInfo list(Video video)
    {
        startPage();
        List<Video> list = videoService.selectVideoList(video);
        return getDataTable(list);
    }

    /**
     * 导出视频列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:export')")
    @Log(title = "视频管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Video video)
    {
        List<Video> list = videoService.selectVideoList(video);
        ExcelUtil<Video> util = new ExcelUtil<Video>(Video.class);
        util.exportExcel(response, list, "视频数据");
    }

    /**
     * 获取视频详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        logger.info("🔍 获取视频详情请求: videoId={}", id);
        Video video = videoService.selectVideoById(id);
        if (video != null) {
            // 🔐 重要：获取处理后的富文本内容（自动拼接签名URL）
            if (video.getVideoContent() != null) {
                logger.info("🔍 原始视频内容长度: {}", video.getVideoContent().length());
                String processedContent = videoService.getProcessedVideoContent(id);
                logger.info("🔍 处理后视频内容长度: {}", processedContent.length());
                video.setVideoContent(processedContent);
            }
            
            // 🔐 重要：为封面图片生成签名URL（修复编辑时封面不显示的问题）
            if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
                try {
                    String coverUrl = video.getCoverImage(); // 直接使用coverImage字段
                    if (coverUrl != null) {
                        // 使用ChiguaUrlService生成签名URL
                        String signedCoverUrl = chiguaUrlService.generateWorkerUrl(coverUrl, ChiguaUrlService.ResourceType.COVER);
                        if (signedCoverUrl != null) {
                            // 使用coverUrl临时字段，不会持久化到数据库
                            video.setCoverUrl(signedCoverUrl);
                            logger.info("✅ 视频{}封面URL已更新为签名URL: {}", id, signedCoverUrl);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("❌ 为视频{}生成封面签名URL失败: {}", id, e.getMessage());
                }
            }
        }
        return success(video);
    }

    /**
     * 获取视频原始内容（用于编辑，包含data-resource-key格式）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping(value = "/{id}/raw")
    public AjaxResult getRawInfo(@PathVariable("id") Long id)
    {
        return success(videoService.selectVideoById(id));
    }

    /**
     * 获取视频的处理后富文本内容（用于显示，包含签名URL）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping(value = "/{id}/processed-content")
    public AjaxResult getProcessedContent(@PathVariable("id") Long id)
    {
        try {
            logger.info("🔄 获取视频{}的处理后富文本内容", id);
            String processedContent = videoService.getProcessedVideoContent(id);
            logger.info("📄 处理后内容长度: {}", processedContent != null ? processedContent.length() : 0);
            
            // 检查是否包含data-resource-key（应该被转换掉）
            if (processedContent != null && processedContent.contains("data-resource-key")) {
                logger.warn("⚠️ 处理后的内容仍包含data-resource-key，可能转换失败");
            }
            
            // 检查是否包含签名URL
            if (processedContent != null && processedContent.contains("signature=")) {
                logger.info("✅ 处理后的内容包含签名URL");
            }
            
            return success(processedContent);
        } catch (Exception e) {
            logger.error("获取处理后富文本内容失败: videoId={}, 错误: {}", id, e.getMessage(), e);
            return error("获取视频内容失败: " + e.getMessage());
        }
    }

    /**
     * 新增视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:add')")
    @Log(title = "视频管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Video video)
    {
        return toAjax(videoService.insertVideo(video));
    }

    /**
     * 修改视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Video video)
    {
        // 🔧 调试：记录接收到的视频内容
        if (video.getVideoContent() != null) {
            logger.info("📝 接收到视频更新请求: videoId={}, contentLength={}", 
                video.getId(), video.getVideoContent().length());
            logger.debug("📝 视频内容预览: {}", 
                video.getVideoContent().length() > 200 ? 
                video.getVideoContent().substring(0, 200) + "..." : 
                video.getVideoContent());
        }
        
        return toAjax(videoService.updateVideo(video));
    }

    /**
     * 删除视频
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:remove')")
    @Log(title = "视频管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(videoService.deleteVideoByIds(ids));
    }

    /**
     * 获取所有分类列表（用于下拉选择）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @GetMapping("/categories")
    public AjaxResult getCategories()
    {
        List<Category> categories = videoService.selectAllCategories();
        return success(categories);
    }

    /**
     * 获取所有标签列表（用于下拉选择）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @GetMapping("/tags")
    public AjaxResult getTags()
    {
        List<Tag> tags = videoService.selectAllTags();
        return success(tags);
    }

    /**
     * 分页获取标签列表（用于标签选择弹出层）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @GetMapping("/tags/page")
    public TableDataInfo getTagsPage(Tag tag)
    {
        startPage();
        List<Tag> list = videoService.selectTagsPage(tag);
        return getDataTable(list);
    }

    /**
     * 根据ID列表获取标签信息（用于编辑时回显已选标签）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @PostMapping("/tags/byIds")
    public AjaxResult getTagsByIds(@RequestBody List<Long> tagIds)
    {
        if (tagIds == null || tagIds.isEmpty()) {
            return success(new ArrayList<>());
        }
        List<Tag> tags = videoService.selectTagsByIds(tagIds);
        return success(tags);
    }

    /**
     * 搜索标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @GetMapping("/searchTags")
    public AjaxResult searchTags(@RequestParam String keyword)
    {
        List<Tag> tags = videoService.searchTags(keyword);
        return success(tags);
    }

    /**
     * 根据已选标签推荐相关标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:list')")
    @PostMapping("/recommendTags")
    public AjaxResult recommendTags(@RequestBody List<Long> selectedTagIds)
    {
        // 获取推荐标签，限制最多返回30条
        List<Tag> recommendedTags = videoService.recommendTags(selectedTagIds);
        return success(recommendedTags);
    }

    /**
     * 获取视频的分类列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping("/{id}/categories")
    public AjaxResult getVideoCategories(@PathVariable("id") Long id)
    {
        List<Category> categories = videoService.selectCategoriesByVideoId(id);
        return success(categories);
    }

    /**
     * 获取视频的标签列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping("/{id}/tags")
    public AjaxResult getVideoTags(@PathVariable("id") Long id)
    {
        List<Tag> tags = videoService.selectTagsByVideoId(id);
        return success(tags);
    }

    /**
     * 批量修改视频状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        Integer status = video.getStatus();
        return toAjax(videoService.updateVideoStatus(ids, status));
    }

    /**
     * 批量设置推荐状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/recommended")
    public AjaxResult updateRecommended(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        Integer isRecommended = video.getIsRecommended();
        return toAjax(videoService.updateVideoRecommended(ids, isRecommended));
    }

    /**
     * 批量设置热门状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/hot")
    public AjaxResult updateHot(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        Integer isHot = video.getIsHot();
        return toAjax(videoService.updateVideoHot(ids, isHot));
    }

    /**
     * 批量设置多分类
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/categories")
    public AjaxResult updateCategories(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        List<Long> categoryIds = video.getCategoryIds();
        return toAjax(videoService.batchUpdateVideoCategories(ids, categoryIds));
    }

    /**
     * 批量修改视频主分类
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/primaryCategory")
    public AjaxResult updatePrimaryCategory(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        Long categoryId = video.getCategoryId();
        
        // 参数验证
        if (ids == null || ids.length == 0) {
            return error("请选择要修改的视频");
        }
        if (ids.length > 1000) {
            return error("单次最多只能修改1000个视频");
        }
        if (categoryId == null || categoryId <= 0) {
            return error("请选择有效的目标分类");
        }
        
        try {
            int result = videoService.batchUpdateVideoPrimaryCategory(ids, categoryId);
            if (result > 0) {
                return success(String.format("成功修改 %d 个视频的主分类", result));
            } else {
                return error("修改失败，请检查视频是否存在");
            }
        } catch (Exception e) {
            logger.error("批量修改视频主分类失败: {}", e.getMessage(), e);
            return error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 批量添加标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/addTags")
    public AjaxResult addTags(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        List<Long> tagIds = video.getTagIds();
        return toAjax(videoService.batchAddVideoTags(ids, tagIds));
    }

    /**
     * 批量移除标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/removeTags")
    public AjaxResult removeTags(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        List<Long> tagIds = video.getTagIds();
        return toAjax(videoService.batchRemoveVideoTags(ids, tagIds));
    }

    /**
     * 批量替换标签（清空原标签后设置新标签）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/replaceTags")
    public AjaxResult replaceTags(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        List<Long> tagIds = video.getTagIds();
        
        // 参数验证
        if (ids == null || ids.length == 0) {
            return error("请选择要修改的视频");
        }
        if (ids.length > 1000) {
            return error("单次最多只能修改1000个视频");
        }
        if (tagIds != null && tagIds.size() > 50) {
            return error("单个视频最多只能设置50个标签");
        }
        
        try {
            int result = videoService.batchReplaceVideoTags(ids, tagIds);
            if (result > 0) {
                String message = tagIds == null || tagIds.isEmpty() ? 
                    String.format("成功清空 %d 个视频的标签", result) :
                    String.format("成功替换 %d 个视频的标签", result);
                return success(message);
            } else {
                return error("操作失败，请检查视频是否存在");
            }
        } catch (Exception e) {
            logger.error("批量替换视频标签失败: {}", e.getMessage(), e);
            return error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 更新视频统计数据
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/statistics")
    public AjaxResult updateStatistics(@RequestBody Video video)
    {
        return toAjax(videoService.updateVideoStatistics(video));
    }

    /**
     * 创建新标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:add')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping("/createTag")
    public AjaxResult createTag(@RequestBody Tag tag)
    {
        try {
            System.out.println("🏷️ 创建标签请求 - 输入数据: " + tag);
            int result = videoService.insertTag(tag);
            System.out.println("🏷️ 插入结果: " + result + ", 标签ID: " + tag.getId());
            
            if (result > 0) {
                // 返回新创建的标签数据
                System.out.println("🏷️ 返回标签数据: " + tag);
                return AjaxResult.success("标签创建成功", tag);
            }
            return AjaxResult.error("标签创建失败");
        } catch (Exception e) {
            System.err.println("🏷️ 创建标签异常: " + e.getMessage());
            e.printStackTrace();
            return AjaxResult.error("标签创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新视频内容
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/content")
    public AjaxResult updateVideoContent(@RequestBody Video video)
    {
        return toAjax(videoService.updateVideoContent(video.getId(), video.getVideoContent()));
    }

    /**
     * 获取包含URL信息的视频详情
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:query')")
    @GetMapping("/withUrls/{id}")
    public AjaxResult getVideoWithUrls(@PathVariable("id") Long id)
    {
        VideoWithUrlsDto video = videoService.selectVideoWithUrlsById(id);
        return success(video);
    }

    /**
     * 生成视频的完整富文本内容（包含播放器和技术信息）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PostMapping("/generateContent/{id}")
    public AjaxResult generateVideoContent(@PathVariable("id") Long id)
    {
        String content = videoService.generateEnhancedVideoContent(id);
        return success(content);
    }

    /**
     * 更新视频的富文本内容（包含播放器和技术信息）
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/updateContentWithUrls/{id}")
    public AjaxResult updateVideoContentWithUrls(@PathVariable("id") Long id)
    {
        return toAjax(videoService.updateVideoContentWithUrls(id));
    }

    /**
     * 批量更新视频的富文本内容
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "视频管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batchUpdateContentWithUrls")
    public AjaxResult batchUpdateVideoContentWithUrls(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        if (ids == null || ids.length == 0) {
            return error("请选择要更新的视频");
        }
        
        int successCount = 0;
        for (Long id : ids) {
            try {
                int result = videoService.updateVideoContentWithUrls(id);
                if (result > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("更新视频{}的富文本内容失败: {}", id, e.getMessage());
            }
        }
        
        return success(String.format("成功更新%d个视频的富文本内容", successCount));
    }

    /**
     * 更新视频富文本中的视频URL签名
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "更新视频富文本签名", businessType = BusinessType.UPDATE)
    @PutMapping("/updateContentSignatures/{id}")
    public AjaxResult updateVideoContentSignatures(@PathVariable("id") Long id)
    {
        try {
            int result = videoService.updateVideoContentSignatures(id);
            if (result > 0) {
                return success("更新视频富文本签名成功");
            } else {
                return error("视频不存在或无富文本内容");
            }
        } catch (Exception e) {
            return error("更新视频富文本签名失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新视频富文本中的视频URL签名
     */
    @PreAuthorize("@ss.hasPermi('chigua:video:edit')")
    @Log(title = "批量更新视频富文本签名", businessType = BusinessType.UPDATE)
    @PutMapping("/batchUpdateContentSignatures")
    public AjaxResult batchUpdateVideoContentSignatures(@RequestBody Video video)
    {
        Long[] ids = video.getIds();
        if (ids == null || ids.length == 0) {
            return error("请选择要更新的视频");
        }
        
        try {
            List<Long> videoIds = java.util.Arrays.asList(ids);
            int successCount = videoService.batchUpdateVideoContentSignatures(videoIds);
            return success(String.format("成功更新%d个视频的富文本签名", successCount));
        } catch (Exception e) {
            return error("批量更新视频富文本签名失败：" + e.getMessage());
        }
    }
} 