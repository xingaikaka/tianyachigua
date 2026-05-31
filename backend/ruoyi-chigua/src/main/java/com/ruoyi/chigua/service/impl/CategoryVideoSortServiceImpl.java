package com.ruoyi.chigua.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import com.github.pagehelper.PageHelper;
import com.ruoyi.chigua.mapper.CategoryVideoSortMapper;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.domain.CategoryVideoSort;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.service.ICategoryVideoSortService;
import com.ruoyi.chigua.service.VideoUrlGeneratorService;
import com.ruoyi.chigua.service.CacheRefreshService;

/**
 * 分类视频排序Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-18
 */
@Service
public class CategoryVideoSortServiceImpl implements ICategoryVideoSortService 
{
    private static final Logger logger = LoggerFactory.getLogger(CategoryVideoSortServiceImpl.class);

    @Autowired
    private CategoryVideoSortMapper categoryVideoSortMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Autowired
    private VideoUrlGeneratorService videoUrlGeneratorService;

    /**
     * 查询分类视频排序
     * 
     * @param id 分类视频排序主键
     * @return 分类视频排序
     */
    @Override
    public CategoryVideoSort selectCategoryVideoSortById(Long id)
    {
        return categoryVideoSortMapper.selectCategoryVideoSortById(id);
    }

    /**
     * 查询分类视频排序列表
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 分类视频排序
     */
    @Override
    public List<CategoryVideoSort> selectCategoryVideoSortList(CategoryVideoSort categoryVideoSort)
    {
        return categoryVideoSortMapper.selectCategoryVideoSortList(categoryVideoSort);
    }

    /**
     * 根据分类ID查询排序视频列表（包含视频详情）
     * 
     * @param categoryId 分类ID
     * @return 排序视频列表
     */
    @Override
    public List<CategoryVideoSort> selectSortedVideosByCategory(Long categoryId)
    {
        List<CategoryVideoSort> result = categoryVideoSortMapper.selectSortedVideosByCategory(categoryId);
        
        // 为每个视频生成签名封面URL，与视频管理列表保持一致
        for (CategoryVideoSort item : result) {
            if (item.getVideo() != null && item.getVideo().getCoverImage() != null && !item.getVideo().getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = videoUrlGeneratorService.generateCoverUrl(item.getVideo());
                    if (signedCoverUrl != null) {
                        item.getVideo().setCoverImage(signedCoverUrl);
                    }
                } catch (Exception e) {
                    logger.warn("❌ 生成视频{}封面签名URL失败: {}", item.getVideo().getId(), e.getMessage());
                }
            }
        }
        
        return result;
    }

    /**
     * 检查分类是否有手动排序（带缓存）
     * 
     * @param categoryId 分类ID
     * @return 是否有手动排序
     */
    @Override
    @Cacheable(value = "categoryVideoSort", key = "'hasSort_' + #categoryId", unless = "#result == false")
    public boolean hasManualSort(Long categoryId) 
    {
        if (categoryId == null) {
            return false;
        }
        int count = categoryVideoSortMapper.countByCategoryId(categoryId);
        logger.debug("🔍 检查分类{}手动排序: count={}", categoryId, count);
        return count > 0;
    }

    /**
     * 获取分类的混合排序视频（带缓存）
     * 
     * @param categoryId 分类ID
     * @param offset 偏移量
     * @param pageSize 页大小
     * @return 混合排序的视频列表
     */
    @Override
    @Cacheable(value = "categoryVideoSort", key = "'category_mixed_' + (#categoryId != null ? #categoryId : 'all') + '_' + #offset + '_' + #pageSize", unless = "#result == null")
    public List<Video> getCategoryMixedSortVideos(Long categoryId, int offset, int pageSize) 
    {
        // 清理可能残留的分页上下文，避免与XML中的 limit 叠加导致 “LIMIT ?, ? LIMIT ?”
        try {
            PageHelper.clearPage();
        } catch (Throwable ignored) {}

        logger.info("🔀 执行分类混合排序查询: categoryId={}, offset={}, pageSize={}", categoryId, offset, pageSize);
        
        // 执行混合排序逻辑
        return executeCategoryMixedSort(categoryId, offset, pageSize);
    }

    /**
     * 执行分类混合排序逻辑
     */
    private List<Video> executeCategoryMixedSort(Long categoryId, int offset, int pageSize) 
    {
        // 第1步：查询手动排序视频（可能为空）
        List<Video> manualVideos = categoryVideoSortMapper.selectManualSortVideosByCategory(categoryId, 0, offset + pageSize);
        logger.info("🎯 查询到{}个手动排序视频", manualVideos.size());
        
        // 调试：检查返回的视频数据
        for (Video video : manualVideos) {
            logger.info("🔍 手动排序视频{}: title={}, coverImage={}", video.getId(), video.getTitle(), video.getCoverImage());
        }
        
        // 第2步：如果手动排序视频不够，补充默认排序视频
        if (manualVideos.size() < offset + pageSize) {
            Set<Long> excludeIds = manualVideos.stream().map(Video::getId).collect(Collectors.toSet());
            int needMore = (offset + pageSize) - manualVideos.size();
            
            List<Video> defaultVideos = categoryVideoSortMapper.selectDefaultSortVideosByCategory(categoryId, excludeIds, 0, needMore);
            logger.debug("🎯 补充{}个默认排序视频", defaultVideos.size());
            
            manualVideos.addAll(defaultVideos);
        }
        
        // 第3步：内存分页
        int start = Math.min(offset, manualVideos.size());
        int end = Math.min(offset + pageSize, manualVideos.size());
        
        // 重要：创建新的ArrayList避免SubList序列化问题
        List<Video> result = new ArrayList<>(manualVideos.subList(start, end));
        logger.info("✅ 混合排序完成: 返回{}条视频记录", result.size());
        
        return result;
    }

    /**
     * 新增分类视频排序
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 结果
     */
    @Override
    public int insertCategoryVideoSort(CategoryVideoSort categoryVideoSort)
    {
        int result = categoryVideoSortMapper.insertCategoryVideoSort(categoryVideoSort);
        if (result > 0) {
            clearCategoryVideoSortCache(categoryVideoSort.getCategoryId());
        }
        return result;
    }

    /**
     * 修改分类视频排序
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 结果
     */
    @Override
    public int updateCategoryVideoSort(CategoryVideoSort categoryVideoSort)
    {
        int result = categoryVideoSortMapper.updateCategoryVideoSort(categoryVideoSort);
        if (result > 0) {
            clearCategoryVideoSortCache(categoryVideoSort.getCategoryId());
        }
        return result;
    }

    /**
     * 批量删除分类视频排序
     * 
     * @param ids 需要删除的分类视频排序主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteCategoryVideoSortByIds(Long[] ids)
    {
        // 先查询要删除的记录，获取分类ID用于清理缓存
        Set<Long> categoryIds = java.util.Arrays.stream(ids)
            .map(id -> selectCategoryVideoSortById(id))
            .filter(sort -> sort != null)
            .map(CategoryVideoSort::getCategoryId)
            .collect(Collectors.toSet());
            
        int result = categoryVideoSortMapper.deleteCategoryVideoSortByIds(ids);
        
        if (result > 0) {
            // 清理相关分类的缓存
            for (Long categoryId : categoryIds) {
                clearCategoryVideoSortCache(categoryId);
            }
        }
        
        return result;
    }

    /**
     * 删除分类视频排序信息
     * 
     * @param id 分类视频排序主键
     * @return 结果
     */
    @Override
    public int deleteCategoryVideoSortById(Long id)
    {
        CategoryVideoSort categoryVideoSort = selectCategoryVideoSortById(id);
        int result = categoryVideoSortMapper.deleteCategoryVideoSortById(id);
        
        if (result > 0 && categoryVideoSort != null) {
            clearCategoryVideoSortCache(categoryVideoSort.getCategoryId());
        }
        
        return result;
    }

    /**
     * 批量设置分类视频排序
     * 
     * @param categoryId 分类ID
     * @param videoSorts 视频排序列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchSetCategoryVideoSort(Long categoryId, List<CategoryVideoSort> videoSorts)
    {
        if (categoryId == null || CollectionUtils.isEmpty(videoSorts)) {
            return 0;
        }
        
        logger.info("🔄 批量设置分类{}视频排序: {}条记录", categoryId, videoSorts.size());
        
        // 先删除该分类的所有排序记录
        categoryVideoSortMapper.deleteByCategoryId(categoryId);
        
        // 批量插入新的排序记录
        int result = categoryVideoSortMapper.batchInsertCategoryVideoSort(videoSorts);
        
        if (result > 0) {
            // 事务提交后清理缓存
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    logger.info("📝 分类视频排序事务提交成功，开始清理缓存: categoryId={}", categoryId);
                    clearCategoryVideoSortCache(categoryId);
                }
            });
        }
        
        return result;
    }

    /**
     * 移除分类视频排序
     * 
     * @param categoryId 分类ID
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int removeCategoryVideoSort(Long categoryId, Long videoId)
    {
        int result = categoryVideoSortMapper.deleteByCategoryIdAndVideoId(categoryId, videoId);
        if (result > 0) {
            clearCategoryVideoSortCache(categoryId);
        }
        return result;
    }

    /**
     * 获取分类下可添加排序的视频列表（排除已排序的）
     * 
     * @param categoryId 分类ID
     * @param title 视频标题（可选）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 可添加的视频列表
     */
    @Override
    public List<Video> getAvailableVideosForSort(Long categoryId, String title, Integer pageNum, Integer pageSize)
    {
        // 设置默认分页参数
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 30 : pageSize;
        
        logger.info("🔍 查询分类{}可添加排序的视频: title={}, pageNum={}, pageSize={}", 
                   categoryId, title, safePageNum, safePageSize);
        
        // 获取已排序的视频ID
        List<CategoryVideoSort> sortedVideos = selectSortedVideosByCategory(categoryId);
        Set<Long> sortedVideoIds = sortedVideos.stream()
            .map(CategoryVideoSort::getVideoId)
            .collect(Collectors.toSet());
        
        logger.info("📋 分类{}已排序视频数量: {}", categoryId, sortedVideoIds.size());
        
        // 使用新的SQL查询，在数据库层面排除已排序的视频，然后分页
        PageHelper.startPage(safePageNum, safePageSize);
        List<Video> availableVideos = videoMapper.selectAvailableVideosForSort(categoryId, title, sortedVideoIds);
        
        // 为每个视频生成签名封面URL，与视频管理列表保持一致
        for (Video video : availableVideos) {
            if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
                try {
                    String signedCoverUrl = videoUrlGeneratorService.generateCoverUrl(video);
                    if (signedCoverUrl != null) {
                        video.setCoverImage(signedCoverUrl);
                    }
                } catch (Exception e) {
                    logger.warn("❌ 生成视频{}封面签名URL失败: {}", video.getId(), e.getMessage());
                }
            }
        }
        
        logger.info("✅ 分类{}可添加排序视频查询完成，返回{}条记录", categoryId, availableVideos.size());
        return availableVideos;
    }

    /**
     * 清理分类视频排序缓存（精确清理特定分类）
     */
    private void clearCategoryVideoSortCache(Long categoryId) 
    {
        if (categoryId != null) {
            logger.info("🔄 精确清理分类{}视频排序缓存和视频列表缓存", categoryId);
            // 🎯 使用精确清理，清理特定分类的排序缓存
            cacheRefreshService.clearSpecificCategoryVideoSortCache(categoryId);
            // 🎯 同时清理视频列表缓存，确保前端显示最新排序
            cacheRefreshService.clearCategoryVideoCache(categoryId);
        }
    }
}
