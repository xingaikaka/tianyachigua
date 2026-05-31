package com.ruoyi.chigua.mapper;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.chigua.domain.CategoryVideoSort;
import com.ruoyi.chigua.domain.Video;

/**
 * 分类视频排序Mapper接口
 * 
 * @author ruoyi
 * @date 2025-01-18
 */
public interface CategoryVideoSortMapper 
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
     * 统计分类下手动排序视频数量
     * 
     * @param categoryId 分类ID
     * @return 数量
     */
    public int countByCategoryId(Long categoryId);

    /**
     * 统计分类下默认排序视频数量（排除指定视频）
     * 
     * @param categoryId 分类ID
     * @param excludeIds 要排除的视频ID集合
     * @return 数量
     */
    public int countDefaultSortVideosByCategory(@Param("categoryId") Long categoryId, 
                                               @Param("excludeIds") Set<Long> excludeIds);

    /**
     * 查询分类下手动排序的视频
     * 
     * @param categoryId 分类ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 视频列表
     */
    public List<Video> selectManualSortVideosByCategory(@Param("categoryId") Long categoryId, 
                                                       @Param("offset") int offset, 
                                                       @Param("limit") int limit);

    /**
     * 查询分类下默认排序的视频（排除指定视频）
     * 
     * @param categoryId 分类ID
     * @param excludeIds 要排除的视频ID集合
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 视频列表
     */
    public List<Video> selectDefaultSortVideosByCategory(@Param("categoryId") Long categoryId, 
                                                        @Param("excludeIds") Set<Long> excludeIds,
                                                        @Param("offset") int offset, 
                                                        @Param("limit") int limit);

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
     * 删除分类视频排序
     * 
     * @param id 分类视频排序主键
     * @return 结果
     */
    public int deleteCategoryVideoSortById(Long id);

    /**
     * 批量删除分类视频排序
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCategoryVideoSortByIds(Long[] ids);

    /**
     * 删除分类下的所有排序记录
     * 
     * @param categoryId 分类ID
     * @return 结果
     */
    public int deleteByCategoryId(Long categoryId);

    /**
     * 删除指定分类和视频的排序记录
     * 
     * @param categoryId 分类ID
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteByCategoryIdAndVideoId(@Param("categoryId") Long categoryId, @Param("videoId") Long videoId);

    /**
     * 批量插入分类视频排序
     * 
     * @param categoryVideoSorts 排序记录列表
     * @return 结果
     */
    public int batchInsertCategoryVideoSort(@Param("list") List<CategoryVideoSort> categoryVideoSorts);
}
