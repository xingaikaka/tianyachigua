package com.ruoyi.chigua.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.mapper.CategoryMapper;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.CacheRefreshService;
import org.springframework.cache.annotation.Cacheable;
import com.github.pagehelper.PageHelper;

/**
 * 分类管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class CategoryServiceImpl implements ICategoryService 
{
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 查询分类（带缓存，供前台/内部调用）
     */
    @Override
    @Cacheable(value = "categoryDetail", key = "#id", unless = "#result == null")
    public Category selectCategoryById(Long id)
    {
        return categoryMapper.selectCategoryById(id);
    }

    /**
     * 查询分类（不走缓存，供管理后台编辑表单使用）
     * 避免管理员看到旧缓存数据后保存，导致 isTelegram 等字段被意外覆盖
     */
    @Override
    public Category selectCategoryByIdForAdmin(Long id)
    {
        return categoryMapper.selectCategoryById(id);
    }

    /**
     * 查询分类列表
     * 
     * @param category 分类
     * @return 分类
     */
    @Override
    public List<Category> selectCategoryList(Category category)
    {
        // 后台管理不使用缓存，确保数据实时性
        return categoryMapper.selectCategoryList(category);
    }

    /**
     * 查询分类列表（前台缓存版本）
     * 仅供前台 chigua-web 使用，提供缓存功能
     * 
     * @return 分类列表
     */
    @Cacheable(value = "categoryList", key = "'frontend_all'", unless = "#result == null or #result.isEmpty()")
    public List<Category> selectCategoryListForFrontend()
    {
        // 前台使用缓存，查询所有启用的分类
        Category category = new Category();
        category.setStatus(1); // 只查询启用的分类
        return categoryMapper.selectCategoryList(category);
    }

    /**
     * 新增分类
     * 
     * @param category 分类
     * @return 结果
     */
    @Override
    public int insertCategory(Category category)
    {
        int result = categoryMapper.insertCategory(category);
        
        // 新增分类后刷新缓存
        if (result > 0) {
            cacheRefreshService.refreshCategoryCache();
        }
        
        return result;
    }

    /**
     * 修改分类
     * 
     * @param category 分类
     * @return 结果
     */
    @Override
    public int updateCategory(Category category)
    {
        int result = categoryMapper.updateCategory(category);
        
        // 修改分类后刷新缓存
        if (result > 0) {
            cacheRefreshService.refreshCategoryCache();
        }
        
        return result;
    }

    /**
     * 批量删除分类
     * 
     * @param ids 需要删除的分类主键
     * @return 结果
     */
    @Override
    public int deleteCategoryByIds(Long[] ids)
    {
        int result = categoryMapper.deleteCategoryByIds(ids);
        
        // 删除后刷新分类缓存
        if (result > 0) {
            cacheRefreshService.refreshCategoryCache();
        }
        
        return result;
    }

    /**
     * 删除分类信息
     * 
     * @param id 分类主键
     * @return 结果
     */
    @Override
    public int deleteCategoryById(Long id)
    {
        return categoryMapper.deleteCategoryById(id);
    }

    /**
     * 校验分类名称是否唯一
     * 
     * @param category 分类信息
     * @return 结果
     */
    @Override
    public boolean checkCategoryNameUnique(Category category)
    {
        Long id = StringUtils.isNull(category.getId()) ? -1L : category.getId();
        
        // 清理任何可能残留的分页参数
        try {
            PageHelper.clearPage();
            Category info = categoryMapper.checkCategoryNameUnique(category.getName());
            if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
            {
                return false;
            }
        } finally {
            // 确保分页参数被完全清理
            PageHelper.clearPage();
        }
        return true;
    }

    /**
     * 查询分类是否存在视频
     * 
     * @param id 分类ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkCategoryExistVideo(Long id)
    {
        int result = categoryMapper.checkCategoryExistVideo(id);
        return result > 0;
    }
} 