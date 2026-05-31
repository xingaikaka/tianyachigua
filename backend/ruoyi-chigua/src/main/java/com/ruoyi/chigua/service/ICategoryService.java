package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.Category;

/**
 * 分类管理Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ICategoryService 
{
    /**
     * 查询分类（带缓存，供前台/内部调用）
     * 
     * @param id 分类主键
     * @return 分类
     */
    public Category selectCategoryById(Long id);

    /**
     * 查询分类（不走缓存，供管理后台编辑表单使用）
     * 确保管理员看到的始终是数据库最新值，避免因缓存旧数据导致字段被覆盖
     *
     * @param id 分类主键
     * @return 分类
     */
    public Category selectCategoryByIdForAdmin(Long id);

    /**
     * 查询分类列表
     * 
     * @param category 分类
     * @return 分类集合
     */
    public List<Category> selectCategoryList(Category category);

    /**
     * 查询分类列表（前台缓存版本）
     * 仅供前台 chigua-web 使用，提供缓存功能
     * 
     * @return 分类列表
     */
    public List<Category> selectCategoryListForFrontend();

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
     * 批量删除分类
     * 
     * @param ids 需要删除的分类主键集合
     * @return 结果
     */
    public int deleteCategoryByIds(Long[] ids);

    /**
     * 删除分类信息
     * 
     * @param id 分类主键
     * @return 结果
     */
    public int deleteCategoryById(Long id);

    /**
     * 校验分类名称是否唯一
     * 
     * @param category 分类信息
     * @return 结果
     */
    public boolean checkCategoryNameUnique(Category category);

    /**
     * 查询分类是否存在视频
     * 
     * @param id 分类ID
     * @return 结果 true 存在 false 不存在
     */
    public boolean checkCategoryExistVideo(Long id);
} 