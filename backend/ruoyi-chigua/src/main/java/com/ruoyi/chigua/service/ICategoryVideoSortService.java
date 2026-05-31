package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.CategoryVideoSort;
import com.ruoyi.chigua.domain.Video;

/**
 * 分类视频排序Service接口
 * 
 * @author ruoyi
 * @date 2025-01-18
 */
public interface ICategoryVideoSortService 
{
    /**
     * 查询分类视频排序
     * 
     * @param id 分类视频排序主键
     * @return 分类视频排序
     */
    public CategoryVideoSort selectCategoryVideoSortById(Long id);

    /**
     * 查询分类视频排序列表
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 分类视频排序集合
     */
    public List<CategoryVideoSort> selectCategoryVideoSortList(CategoryVideoSort categoryVideoSort);

    /**
     * 根据分类ID查询排序视频列表（包含视频详情）
     * 
     * @param categoryId 分类ID
     * @return 排序视频列表
     */
    public List<CategoryVideoSort> selectSortedVideosByCategory(Long categoryId);

    /**
     * 检查分类是否有手动排序（带缓存）
     * 
     * @param categoryId 分类ID
     * @return 是否有手动排序
     */
    public boolean hasManualSort(Long categoryId);

    /**
     * 获取分类的混合排序视频（带缓存）
     * 
     * @param categoryId 分类ID
     * @param offset 偏移量
     * @param pageSize 页大小
     * @return 混合排序的视频列表
     */
    public List<Video> getCategoryMixedSortVideos(Long categoryId, int offset, int pageSize);

    /**
     * 新增分类视频排序
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 结果
     */
    public int insertCategoryVideoSort(CategoryVideoSort categoryVideoSort);

    /**
     * 修改分类视频排序
     * 
     * @param categoryVideoSort 分类视频排序
     * @return 结果
     */
    public int updateCategoryVideoSort(CategoryVideoSort categoryVideoSort);

    /**
     * 批量删除分类视频排序
     * 
     * @param ids 需要删除的分类视频排序主键集合
     * @return 结果
     */
    public int deleteCategoryVideoSortByIds(Long[] ids);

    /**
     * 删除分类视频排序信息
     * 
     * @param id 分类视频排序主键
     * @return 结果
     */
    public int deleteCategoryVideoSortById(Long id);

    /**
     * 批量设置分类视频排序
     * 
     * @param categoryId 分类ID
     * @param videoSorts 视频排序列表
     * @return 结果
     */
    public int batchSetCategoryVideoSort(Long categoryId, List<CategoryVideoSort> videoSorts);

    /**
     * 移除分类视频排序
     * 
     * @param categoryId 分类ID
     * @param videoId 视频ID
     * @return 结果
     */
    public int removeCategoryVideoSort(Long categoryId, Long videoId);

    /**
     * 获取分类下可添加排序的视频列表（排除已排序的）
     * 
     * @param categoryId 分类ID
     * @param title 视频标题（可选）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 可添加的视频列表
     */
    public List<Video> getAvailableVideosForSort(Long categoryId, String title, Integer pageNum, Integer pageSize);
}
