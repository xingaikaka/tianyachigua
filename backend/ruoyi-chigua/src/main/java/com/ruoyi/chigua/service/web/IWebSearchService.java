package com.ruoyi.chigua.service.web;

import java.util.List;

import com.ruoyi.chigua.domain.Video;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Web搜索Service接口
 * 
 * @author chigua
 * @date 2025-01-22
 */
public interface IWebSearchService 
{
    /**
     * 根据关键词搜索视频
     * 
     * @param keyword 搜索关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    public TableDataInfo searchVideosByKeyword(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 搜索基础列表缓存（不含签名URL）
     * 支持"基础数据缓存 + 出口实时签名"模式
     */
    public List<Video> getCachedSearchVideoBaseList(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 搜索总数缓存（独立 key）
     */
    public long getCachedSearchVideoTotal(String keyword);
}
