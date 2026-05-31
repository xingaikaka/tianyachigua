package com.ruoyi.chigua.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.domain.vo.web.WebTgPostVO;
import com.ruoyi.chigua.domain.vo.web.WebTgPageResult;
import com.ruoyi.chigua.service.web.IWebTgPostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Telegram 帖子 Web 接口
 *
 * GET /web/api/tg/posts?categoryId=X&page=1&size=20
 * GET /web/api/tg/posts/{id}
 */
@RestController
@RequestMapping("/web/api/tg")
@CrossOrigin(origins = "*")
public class WebTgPostController
{
    private static final Logger logger = LoggerFactory.getLogger(WebTgPostController.class);

    @Autowired
    private IWebTgPostService webTgPostService;

    /**
     * 分页查询 Telegram 帖子列表（含媒体）
     */
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/posts")
    public AjaxResult listPosts(
            @RequestParam(required = true)                    Integer categoryId,
            @RequestParam(defaultValue = "1")                 int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(required = false)                   String caption)
    {
        if (size > 50) size = 50;
        if (page < 1)  page = 1;

        WebTgPageResult result = webTgPostService.selectTgPostPage(categoryId, page, size, caption);
        return AjaxResult.success(result);
    }

    /**
     * 查询单个帖子详情
     */
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/posts/{id}")
    public AjaxResult getPost(@PathVariable Long id)
    {
        WebTgPostVO vo = webTgPostService.selectTgPostById(id);
        if (vo == null) {
            return AjaxResult.error("帖子不存在");
        }
        return AjaxResult.success(vo);
    }
}
