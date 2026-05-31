package com.ruoyi.chigua.service;

import com.ruoyi.chigua.domain.SeoKeyword;
import com.ruoyi.common.core.domain.AjaxResult;
import java.util.List;

/**
 * SEO关键词Service接口
 * 
 * @author ruoyi
 * @date 2026-01-14
 */
public interface ISeoKeywordService {
    
    /**
     * 根据关键词ID获取详情及相关视频
     * 
     * @param id 关键词ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 关键词详情和视频列表
     */
    AjaxResult getKeywordDetailWithVideos(Long id, Integer pageNum, Integer pageSize);

    /**
     * 根据关键词内容获取详情及相关视频
     * 
     * @param keyword 关键词内容
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 关键词详情和视频列表
     */
    AjaxResult getByKeywordWithVideos(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取热门关键词
     * 
     * @param limit 返回数量
     * @return 热门关键词列表
     */
    AjaxResult getHotKeywords(Integer limit);

    /**
     * 获取随机关键词
     * 
     * @param limit 返回数量
     * @return 随机关键词列表
     */
    AjaxResult getRandomKeywords(Integer limit);

    /**
     * 增加搜索次数
     * 
     * @param id 关键词ID
     * @return 结果
     */
    AjaxResult incrementSearchCount(Long id);

    /**
     * 增加点击次数
     * 
     * @param id 关键词ID
     * @return 结果
     */
    AjaxResult incrementClickCount(Long id);

    /**
     * 获取所有启用的关键词（用于sitemap生成）
     * 
     * @return 关键词列表
     */
    List<SeoKeyword> getAllActiveKeywords();
    
    /**
     * 统计启用的关键词数量
     * 
     * @return 数量
     */
    int countActiveKeywords();

    /**
     * 生成 SEO 关键词 sitemap 分片 XML（按 baseUrl + 分片号缓存，分页查询避免全表加载）
     *
     * @param baseUrl     站点根 URL（如 https://tycg7.com）
     * @param shardNumber 分片号，从 1 开始
     * @param batchSize   每片 URL 数量
     * @return sitemap XML 字符串；分片越界时返回空 urlset
     */
    String generateKeywordSitemapShard(String baseUrl, int shardNumber, int batchSize);
}

