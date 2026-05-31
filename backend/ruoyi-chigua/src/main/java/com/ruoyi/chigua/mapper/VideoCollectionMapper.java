package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.VideoCollection;
import org.apache.ibatis.annotations.Param;

/**
 * 视频合集关系Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface VideoCollectionMapper 
{
    /**
     * 查询视频合集关系
     * 
     * @param id 视频合集关系主键
     * @return 视频合集关系
     */
    public VideoCollection selectVideoCollectionById(Long id);

    /**
     * 查询视频合集关系列表
     * 
     * @param videoCollection 视频合集关系
     * @return 视频合集关系集合
     */
    public List<VideoCollection> selectVideoCollectionList(VideoCollection videoCollection);

    /**
     * 新增视频合集关系
     * 
     * @param videoCollection 视频合集关系
     * @return 结果
     */
    public int insertVideoCollection(VideoCollection videoCollection);

    /**
     * 修改视频合集关系
     * 
     * @param videoCollection 视频合集关系
     * @return 结果
     */
    public int updateVideoCollection(VideoCollection videoCollection);

    /**
     * 删除视频合集关系
     * 
     * @param id 视频合集关系主键
     * @return 结果
     */
    public int deleteVideoCollectionById(Long id);

    /**
     * 批量删除视频合集关系
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoCollectionByIds(Long[] ids);

    /**
     * 根据合集ID删除所有关系
     * 
     * @param collectionId 合集ID
     * @return 结果
     */
    public int deleteVideoCollectionByCollectionId(Long collectionId);

    /**
     * 根据视频ID删除所有关系
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoCollectionByVideoId(Long videoId);

    /**
     * 检查视频是否已在合集中
     * 
     * @param collectionId 合集ID
     * @param videoId 视频ID
     * @return 关系记录
     */
    public VideoCollection selectVideoCollectionByIds(@Param("collectionId") Long collectionId, @Param("videoId") Long videoId);

    /**
     * 获取合集中视频的最大排序号
     * 
     * @param collectionId 合集ID
     * @return 最大排序号
     */
    public Integer selectMaxSortOrderByCollectionId(Long collectionId);

    /**
     * 批量插入视频合集关系
     * 
     * @param videoCollections 视频合集关系列表
     * @return 结果
     */
    public int batchInsertVideoCollection(List<VideoCollection> videoCollections);

    /**
     * 批量更新排序
     * 
     * @param videoCollections 视频合集关系列表
     * @return 结果
     */
    public int batchUpdateSortOrder(List<VideoCollection> videoCollections);

    /**
     * 调整排序（当删除某个视频时，后续视频排序前移）
     * 
     * @param collectionId 合集ID
     * @param sortOrder 被删除视频的排序号
     * @return 结果
     */
    public int adjustSortOrderAfterDelete(@Param("collectionId") Long collectionId, @Param("sortOrder") Integer sortOrder);
} 