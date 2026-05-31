package com.ruoyi.chigua.service.web;

import com.ruoyi.chigua.domain.vo.web.WebTgPostVO;
import com.ruoyi.chigua.domain.vo.web.WebTgPageResult;

/**
 * Telegram 帖子 Web 服务接口
 */
public interface IWebTgPostService
{
    /**
     * 分页查询指定分类下的 Telegram 帖子（含媒体列表）
     *
     * @param categoryId 分类ID
     * @param page       页码（从1开始）
     * @param size       每页条数
     * @return 分页结果
     */
    WebTgPageResult selectTgPostPage(Integer categoryId, int page, int size, String caption);

    /**
     * 查询单个帖子详情
     *
     * @param id 帖子ID
     * @return 帖子VO（含媒体列表）
     */
    WebTgPostVO selectTgPostById(Long id);
}
