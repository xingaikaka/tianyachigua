package com.ruoyi.chigua.controller.web;

import com.ruoyi.chigua.service.web.IWebArchivesService;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web端往期内容Controller
 * 提供按日期分组的视频归档功能
 */
@RestController
@RequestMapping("/web/api/archives")
@CrossOrigin(origins = "*")
public class WebArchivesController {

    @Autowired
    private IWebArchivesService webArchivesService;

    /**
     * 根据日期获取往期内容
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 按日期分组的视频数据
     */
    @RateLimiter(time = 60, count = 40, limitType = LimitType.IP)
    @GetMapping("/by-date")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getArchivesByDate(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {
        return webArchivesService.getArchivesByDate(pageNum, pageSize);
    }

    /**
     * 获取标签列表（支持限制数量）
     * @param limit 限制返回的标签数量，默认为18个（往期内容页面使用）
     * @return 标签列表
     */
    @RateLimiter(time = 60, count = 80, limitType = LimitType.IP)
    @GetMapping("/tags")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getAllTags(@RequestParam(value = "limit", defaultValue = "18") Integer limit) {
        return webArchivesService.getAllTags(limit);
    }
}