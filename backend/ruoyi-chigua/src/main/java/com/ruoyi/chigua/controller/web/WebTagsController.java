package com.ruoyi.chigua.controller.web;

import com.ruoyi.chigua.service.web.IWebTagsService;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web端标签Controller
 * 提供标签数据的查询功能
 */
@RestController
@RequestMapping("/web/api/tags")
@CrossOrigin(origins = "*")
public class WebTagsController {

    @Autowired
    private IWebTagsService webTagsService;

    /**
     * 获取所有标签
     * @return 标签列表
     */
    @RateLimiter(time = 60, count = 80, limitType = LimitType.IP)
    @GetMapping("/list")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getAllTags() {
        return webTagsService.getAllTags();
    }

    /**
     * 分页获取标签
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页标签数据
     */
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/page")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getTagsPaginated(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "150") Integer pageSize) {
        return webTagsService.getTagsPaginated(pageNum, pageSize);
    }

    /**
     * 根据标签ID获取相关视频
     * @param tagId 标签ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 标签相关视频数据
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{tagId}/videos")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getVideosByTag(
            @PathVariable Long tagId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return webTagsService.getVideosByTag(tagId, pageNum, pageSize);
    }

    /**
     * 根据标签ID获取标签信息
     * @param tagId 标签ID
     * @return 标签信息
     */
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    @GetMapping("/{tagId}/info")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getTagInfo(@PathVariable Long tagId) {
        return webTagsService.getTagInfo(tagId);
    }
}