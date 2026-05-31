package com.ruoyi.chigua.service.impl;

import java.util.List;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.ruoyi.chigua.mapper.CollectionMapper;
import com.ruoyi.chigua.mapper.VideoCollectionMapper;
import com.ruoyi.chigua.domain.Collection;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.VideoCollection;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.service.ICollectionService;
import com.ruoyi.chigua.service.CacheRefreshService;
import org.springframework.cache.annotation.Cacheable;

/**
 * 合集管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class CollectionServiceImpl implements ICollectionService 
{
    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private VideoCollectionMapper videoCollectionMapper;
    
    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 查询合集
     * 
     * @param id 合集主键
     * @return 合集
     */
    @Override
    public Collection selectCollectionById(Long id)
    {
        Collection collection = collectionMapper.selectCollectionById(id);
        if (collection != null) {
            // 查询关联的分类和视频
            List<Category> categories = collectionMapper.selectCategoriesByCollectionId(id);
            List<Video> videos = collectionMapper.selectVideosByCollectionId(id);
            
            collection.setCategories(categories);
            collection.setVideos(videos);
            
            // 设置分类ID数组，方便前端使用
            if (categories != null && !categories.isEmpty()) {
                List<Long> categoryIds = new java.util.ArrayList<>();
                for (Category category : categories) {
                    categoryIds.add(category.getId());
                }
                collection.setCategoryIds(categoryIds);
            }
        }
        return collection;
    }

    /**
     * 查询合集列表
     * 
     * @param collection 合集
     * @return 合集
     */
    @Override
    public List<Collection> selectCollectionList(Collection collection)
    {
        return collectionMapper.selectCollectionList(collection);
    }

    /**
     * 新增合集
     * 
     * @param collection 合集
     * @return 结果
     */
    @Override
    @Transactional
    public int insertCollection(Collection collection)
    {
        // 设置默认值
        if (collection.getViewCount() == null) {
            collection.setViewCount(0);
        }
        if (collection.getVideoCount() == null) {
            collection.setVideoCount(0);
        }
        if (collection.getSortOrder() == null) {
            collection.setSortOrder(0);
        }
        if (collection.getStatus() == null) {
            collection.setStatus(1); // 默认启用状态
        }
        
        int result = collectionMapper.insertCollection(collection);
        
        // 新增合集后更新相关缓存版本号
        if (result > 0) {
            // searchResult 已改为TTL自动过期，不需要主动清理
            // cacheVersionService.incrementVersions("webCollectionList", "searchResult");
            // 清除相关缓存
            cacheRefreshService.refreshCollectionCache(collection.getId(), null);
        }
        
        return result;
    }

    /**
     * 修改合集
     * 
     * @param collection 合集
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCollection(Collection collection)
    {
        int result = collectionMapper.updateCollection(collection);
        
        // 修改合集后更新相关缓存版本号
        if (result > 0) {
            // searchResult 已改为TTL自动过期，不需要主动清理
            // cacheVersionService.incrementVersions("webCollectionList", "searchResult");
            // 清除相关缓存
            cacheRefreshService.refreshCollectionCache(collection.getId(), null);
        }
        
        return result;
    }

    /**
     * 批量删除合集
     * 
     * @param ids 需要删除的合集主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteCollectionByIds(Long[] ids)
    {
        // 删除关联的视频合集关系
        for (Long id : ids) {
            videoCollectionMapper.deleteVideoCollectionByCollectionId(id);
        }
        
        int result = collectionMapper.deleteCollectionByIds(ids);
        
        // 删除后刷新合集独立缓存
        if (result > 0) {
            // 使用独立缓存策略，不依赖版本控制
            cacheRefreshService.refreshCollectionCache(null, null); // null表示批量删除，清理所有合集缓存
        }
        
        return result;
    }

    /**
     * 删除合集信息
     * 
     * @param id 合集主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteCollectionById(Long id)
    {
        // 删除关联的视频合集关系
        videoCollectionMapper.deleteVideoCollectionByCollectionId(id);
        
        return collectionMapper.deleteCollectionById(id);
    }

    /**
     * 查询合集中的视频列表
     * 
     * @param collectionId 合集ID
     * @return 视频集合
     */
    @Override
    public List<Video> selectVideosByCollectionId(Long collectionId)
    {
        return collectionMapper.selectVideosByCollectionId(collectionId);
    }

    /**
     * 查询不在指定合集中的视频列表
     * 
     * @param collectionId 合集ID
     * @param video 视频查询条件
     * @return 视频集合
     */
    @Override
    public List<Video> selectVideosNotInCollection(Long collectionId, Video video)
    {
        return collectionMapper.selectVideosNotInCollection(collectionId, video);
    }

    /**
     * 添加视频到合集
     * 
     * @param collectionId 合集ID
     * @param videoIds 视频ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int addVideosToCollection(Long collectionId, List<Long> videoIds)
    {
        if (CollectionUtils.isEmpty(videoIds)) {
            return 0;
        }
        
        int result = 0;
        // 获取当前合集中视频的最大排序号
        Integer maxSortOrder = videoCollectionMapper.selectMaxSortOrderByCollectionId(collectionId);
        if (maxSortOrder == null) {
            maxSortOrder = 0;
        }
        
        for (Long videoId : videoIds) {
            // 检查视频是否已在合集中
            VideoCollection existing = videoCollectionMapper.selectVideoCollectionByIds(collectionId, videoId);
            if (existing == null) {
                VideoCollection videoCollection = new VideoCollection();
                videoCollection.setCollectionId(collectionId);
                videoCollection.setVideoId(videoId);
                videoCollection.setSortOrder(++maxSortOrder);
                videoCollection.setCreatedAt(new Date());
                
                result += videoCollectionMapper.insertVideoCollection(videoCollection);
            }
        }
        
        // 更新合集视频数量
        if (result > 0) {
            collectionMapper.updateCollectionVideoCount(collectionId);
            // 清除合集相关缓存（视频列表发生变化）
            cacheRefreshService.refreshCollectionCache(collectionId, null);
        }
        
        return result;
    }

    /**
     * 从合集中移除视频
     * 
     * @param collectionId 合集ID
     * @param videoIds 视频ID列表
     * @return 结果
     */
    @Override
    @Transactional
    public int removeVideosFromCollection(Long collectionId, List<Long> videoIds)
    {
        if (CollectionUtils.isEmpty(videoIds)) {
            return 0;
        }
        
        int result = 0;
        for (Long videoId : videoIds) {
            VideoCollection videoCollection = videoCollectionMapper.selectVideoCollectionByIds(collectionId, videoId);
            if (videoCollection != null) {
                result += videoCollectionMapper.deleteVideoCollectionById(videoCollection.getId());
                // 调整后续视频的排序
                videoCollectionMapper.adjustSortOrderAfterDelete(collectionId, videoCollection.getSortOrder());
            }
        }
        
        // 更新合集视频数量
        if (result > 0) {
            collectionMapper.updateCollectionVideoCount(collectionId);
            // 清除合集相关缓存（视频列表发生变化）
            cacheRefreshService.refreshCollectionCache(collectionId, null);
        }
        
        return result;
    }

    /**
     * 更新合集中视频的排序
     * 
     * @param collectionId 合集ID
     * @param videoSortData 视频排序数据
     * @return 结果
     */
    @Override
    @Transactional
    public int updateVideoSortInCollection(Long collectionId, List<Map<String, Object>> videoSortData)
    {
        if (CollectionUtils.isEmpty(videoSortData)) {
            return 0;
        }
        
        List<VideoCollection> updateList = new java.util.ArrayList<>();
        for (Map<String, Object> sortItem : videoSortData) {
            Long videoId = Long.valueOf(sortItem.get("id").toString());
            Integer sortOrder = Integer.valueOf(sortItem.get("sortOrder").toString());
            
            VideoCollection videoCollection = videoCollectionMapper.selectVideoCollectionByIds(collectionId, videoId);
            if (videoCollection != null) {
                videoCollection.setSortOrder(sortOrder);
                updateList.add(videoCollection);
            }
        }
        
        if (!updateList.isEmpty()) {
            int result = videoCollectionMapper.batchUpdateSortOrder(updateList);
            if (result > 0) {
                // 清除合集相关缓存（视频排序发生变化）
                cacheRefreshService.refreshCollectionCache(collectionId, null);
            }
            return result;
        }
        
        return 0;
    }

    /**
     * 查询合集的分类列表
     * 
     * @param collectionId 合集ID
     * @return 分类集合
     */
    @Override
    public List<Category> selectCategoriesByCollectionId(Long collectionId)
    {
        return collectionMapper.selectCategoriesByCollectionId(collectionId);
    }

    /**
     * 更新合集观看次数
     * 
     * @param collectionId 合集ID
     * @param increment 增量
     * @return 结果
     */
    @Override
    public int updateCollectionViewCount(Long collectionId, Integer increment)
    {
        return collectionMapper.updateCollectionViewCount(collectionId, increment);
    }

    /**
     * 查询所有分类列表（用于下拉选择）
     * 
     * @return 分类集合
     */
    @Override
    public List<Category> selectAllCategories()
    {
        return collectionMapper.selectAllCategories();
    }

    /**
     * 批量修改合集状态
     * 
     * @param ids 合集ID数组
     * @param status 状态
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCollectionStatus(Long[] ids, Integer status)
    {
        int result = 0;
        for (Long id : ids) {
            Collection collection = new Collection();
            collection.setId(id);
            collection.setStatus(status);
            result += collectionMapper.updateCollection(collection);
        }
        return result;
    }
} 