package com.ruoyi.chigua.service.web;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Web合集Service接口
 * 
 * @author chigua
 * @date 2025-01-20
 */
public interface IWebCollectionService 
{
    /**
     * 根据分类ID查询Web合集列表
     * 
     * @param categoryId 分类ID，为null时查询所有合集
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 合集列表
     */
    public TableDataInfo selectWebCollectionList(Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 根据合集ID查询Web合集详情
     * 
     * @param collectionId 合集ID
     * @return Web合集详情
     */
    public Collection selectWebCollectionById(Long collectionId);

    /**
     * 查询合集中的视频列表
     * 
     * @param collectionId 合集ID
     * @return 视频列表
     */
    public List<Video> selectVideosByCollectionId(Long collectionId);

    /**
     * Web合集基础列表缓存（不含签名URL）
     * 返回 Map：key="list" 为 List&lt;Collection&gt;，key="total" 为 Long 总数
     * 支持"基础数据缓存 + 出口实时签名"模式
     */
    public Map<String, Object> getCachedWebCollectionBaseList(Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 合集详情基础数据缓存（不含签名URL）
     */
    public Collection getCachedCollectionBaseDetail(Long collectionId);

    /**
     * 合集视频列表基础数据缓存（不含签名URL）
     */
    public List<Video> getCachedCollectionBaseVideos(Long collectionId);
} 