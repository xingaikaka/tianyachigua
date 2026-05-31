package com.ruoyi.chigua.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;

/**
 * 合集管理Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ICollectionService 
{
    /**
     * 查询合集
     * 
     * @param id 合集主键
     * @return 合集
     */
    public Collection selectCollectionById(Long id);

    /**
     * 查询合集列表
     * 
     * @param collection 合集
     * @return 合集集合
     */
    public List<Collection> selectCollectionList(Collection collection);

    /**
     * 新增合集
     * 
     * @param collection 合集
     * @return 结果
     */
    public int insertCollection(Collection collection);

    /**
     * 修改合集
     * 
     * @param collection 合集
     * @return 结果
     */
    public int updateCollection(Collection collection);

    /**
     * 批量删除合集
     * 
     * @param ids 需要删除的合集主键集合
     * @return 结果
     */
    public int deleteCollectionByIds(Long[] ids);

    /**
     * 删除合集信息
     * 
     * @param id 合集主键
     * @return 结果
     */
    public int deleteCollectionById(Long id);

    /**
     * 查询合集中的视频列表
     * 
     * @param collectionId 合集ID
     * @return 视频集合
     */
    public List<Video> selectVideosByCollectionId(Long collectionId);

    /**
     * 查询不在指定合集中的视频列表
     * 
     * @param collectionId 合集ID
     * @param video 视频查询条件
     * @return 视频集合
     */
    public List<Video> selectVideosNotInCollection(Long collectionId, Video video);

    /**
     * 添加视频到合集
     * 
     * @param collectionId 合集ID
     * @param videoIds 视频ID列表
     * @return 结果
     */
    public int addVideosToCollection(Long collectionId, List<Long> videoIds);

    /**
     * 从合集中移除视频
     * 
     * @param collectionId 合集ID
     * @param videoIds 视频ID列表
     * @return 结果
     */
    public int removeVideosFromCollection(Long collectionId, List<Long> videoIds);

    /**
     * 更新合集中视频的排序
     * 
     * @param collectionId 合集ID
     * @param videoSortData 视频排序数据
     * @return 结果
     */
    public int updateVideoSortInCollection(Long collectionId, List<Map<String, Object>> videoSortData);

    /**
     * 查询合集的分类列表
     * 
     * @param collectionId 合集ID
     * @return 分类集合
     */
    public List<Category> selectCategoriesByCollectionId(Long collectionId);

    /**
     * 更新合集观看次数
     * 
     * @param collectionId 合集ID
     * @param increment 增量
     * @return 结果
     */
    public int updateCollectionViewCount(Long collectionId, Integer increment);

    /**
     * 查询所有分类列表（用于下拉选择）
     * 
     * @return 分类集合
     */
    public List<Category> selectAllCategories();

    /**
     * 批量修改合集状态
     * 
     * @param ids 合集ID数组
     * @param status 状态
     * @return 结果
     */
    public int updateCollectionStatus(Long[] ids, Integer status);
} 