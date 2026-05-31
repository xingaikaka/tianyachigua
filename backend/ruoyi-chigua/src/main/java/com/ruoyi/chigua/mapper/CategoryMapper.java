package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.Category;

/**
 * 分类管理 数据层
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface CategoryMapper
{
    /**
     * 查询分类
     * 
     * @param id 分类主键
     * @return 分类
     */
    public Category selectCategoryById(Long id);

    /**
     * 查询分类列表
     * 
     * @param category 分类
     * @return 分类集合
     */
    public List<Category> selectCategoryList(Category category);

    /**
     * 校验分类名称是否唯一
     * 
     * @param name 分类名称
     * @return 分类信息
     */
    public Category checkCategoryNameUnique(String name);

    /**
     * 新增分类
     * 
     * @param category 分类
     * @return 结果
     */
    public int insertCategory(Category category);

    /**
     * 修改分类
     * 
     * @param category 分类
     * @return 结果
     */
    public int updateCategory(Category category);

    /**
     * 删除分类
     * 
     * @param id 分类主键
     * @return 结果
     */
    public int deleteCategoryById(Long id);

    /**
     * 批量删除分类
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCategoryByIds(Long[] ids);

    /**
     * 查询分类是否存在视频
     * 
     * @param id 分类ID
     * @return 结果
     */
    public int checkCategoryExistVideo(Long id);
} 