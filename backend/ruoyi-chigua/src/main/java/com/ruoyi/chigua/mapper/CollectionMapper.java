package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import org.apache.ibatis.annotations.Param;

/**
 * 合集管理Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface CollectionMapper 
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
     * 删除合集
     * 
     * @param id 合集主键
     * @return 结果
     */
    public int deleteCollectionById(Long id);

    /**
     * 批量删除合集
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCollectionByIds(Long[] ids);

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
    public List<Video> selectVideosNotInCollection(@Param("collectionId") Long collectionId, @Param("video") Video video);

    /**
     * 查询合集的分类列表
     * 
     * @param collectionId 合集ID
     * @return 分类集合
     */
    public List<Category> selectCategoriesByCollectionId(Long collectionId);

    /**
     * 更新合集视频数量
     * 
     * @param collectionId 合集ID
     * @return 结果
     */
    public int updateCollectionVideoCount(Long collectionId);

    /**
     * 更新合集观看次数
     * 
     * @param collectionId 合集ID
     * @param increment 增量
     * @return 结果
     */
    public int updateCollectionViewCount(@Param("collectionId") Long collectionId, @Param("increment") Integer increment);

    /**
     * 查询所有分类列表（用于下拉选择）
     * 
     * @return 分类集合
     */
    public List<Category> selectAllCategories();
} 