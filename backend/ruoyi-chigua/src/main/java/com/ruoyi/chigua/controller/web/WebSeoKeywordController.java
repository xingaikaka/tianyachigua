package com.ruoyi.chigua.controller.web;

import com.ruoyi.chigua.service.ISeoKeywordService;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * SEO关键词前端API
 * 
 * @author ruoyi
 * @date 2026-01-14
 */
@RestController
@RequestMapping("/web/api/seo-keywords")
@CrossOrigin(origins = "*")
public class WebSeoKeywordController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(WebSeoKeywordController.class);

    @Autowired
    private ISeoKeywordService seoKeywordService;

    /**
     * 根据关键词ID获取关键词详情及相关视频
     * 
     * @param id 关键词ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 关键词详情和视频列表
     */
    @GetMapping("/{id}")
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    public AjaxResult getKeywordDetail(
            @PathVariable Long id,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        
        logger.info("📍 SEO关键词详情请求: id={}, pageNum={}, pageSize={}", id, pageNum, pageSize);
        return seoKeywordService.getKeywordDetailWithVideos(id, pageNum, pageSize);
    }

    /**
     * 根据关键词内容获取详情及相关视频
     * 
     * @param keyword 关键词内容
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 关键词详情和视频列表
     */
    @GetMapping("/by-keyword")
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    public AjaxResult getByKeyword(
            @RequestParam String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        
        logger.info("📍 SEO关键词查询请求: keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);
        return seoKeywordService.getByKeywordWithVideos(keyword, pageNum, pageSize);
    }

    /**
     * 获取热门关键词列表（根据搜索次数排序）
     * 
     * @param limit 返回数量
     * @return 热门关键词列表
     */
    @GetMapping("/hot")
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    public AjaxResult getHotKeywords(
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        
        logger.info("📍 热门关键词请求: limit={}", limit);
        return seoKeywordService.getHotKeywords(limit);
    }

    /**
     * 获取随机关键词（用于首页展示）
     * 
     * @param limit 返回数量
     * @return 随机关键词列表
     */
    @GetMapping("/random")
    @RateLimiter(time = 60, count = 100, limitType = LimitType.IP)
    public AjaxResult getRandomKeywords(
            @RequestParam(value = "limit", defaultValue = "30") Integer limit) {
        
        logger.info("📍 随机关键词请求: limit={}", limit);
        return seoKeywordService.getRandomKeywords(limit);
    }

    /**
     * 记录关键词搜索（用于统计）
     * 
     * @param id 关键词ID
     * @return 结果
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/{id}/search")
    public AjaxResult recordSearch(@PathVariable Long id) {
        // 静默记录，不输出日志
        return seoKeywordService.incrementSearchCount(id);
    }

    /**
     * 记录关键词点击（用于统计）
     * 
     * @param id 关键词ID
     * @return 结果
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/{id}/click")
    public AjaxResult recordClick(@PathVariable Long id) {
        // 静默记录，不输出日志
        return seoKeywordService.incrementClickCount(id);
    }
}

