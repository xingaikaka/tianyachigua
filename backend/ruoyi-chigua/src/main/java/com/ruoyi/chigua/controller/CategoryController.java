package com.ruoyi.chigua.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.service.ICategoryService;

/**
 * 分类管理Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/category")
public class CategoryController extends BaseController
{
    @Autowired
    private ICategoryService categoryService;

    /**
     * 查询分类列表（后台管理）
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(Category category)
    {
        startPage();
        List<Category> list = categoryService.selectCategoryList(category);
        return getDataTable(list);
    }

    /**
     * 查询分类列表（前台公开接口，带缓存）
     */
    @GetMapping("/frontend/list")
    public AjaxResult frontendList()
    {
        List<Category> list = categoryService.selectCategoryListForFrontend();
        return success(list);
    }

    /**
     * 导出分类列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:export')")
    @Log(title = "分类管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Category category)
    {
        List<Category> list = categoryService.selectCategoryList(category);
        ExcelUtil<Category> util = new ExcelUtil<Category>(Category.class);
        util.exportExcel(response, list, "分类数据");
    }

    /**
     * 获取分类详细信息（不走缓存，确保管理后台编辑表单始终读取最新数据）
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(categoryService.selectCategoryByIdForAdmin(id));
    }

    /**
     * 新增分类
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:add')")
    @Log(title = "分类管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Category category)
    {
        try {
            if (!categoryService.checkCategoryNameUnique(category))
            {
                return error("新增分类'" + category.getName() + "'失败，分类名称已存在");
            }
            return toAjax(categoryService.insertCategory(category));
        } finally {
            // 确保新增操作后分页参数被完全清理
            clearPage();
        }
    }

    /**
     * 修改分类
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:edit')")
    @Log(title = "分类管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Category category)
    {
        try {
            if (!categoryService.checkCategoryNameUnique(category))
            {
                return error("修改分类'" + category.getName() + "'失败，分类名称已存在");
            }
            AjaxResult result = toAjax(categoryService.updateCategory(category));
            return result;
        } finally {
            // 确保修改操作后分页参数被完全清理
            clearPage();
        }
    }

    /**
     * 删除分类
     */
    @PreAuthorize("@ss.hasPermi('chigua:category:remove')")
    @Log(title = "分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        for (Long id : ids)
        {
            if (categoryService.checkCategoryExistVideo(id))
            {
                return error("分类存在视频,不允许删除");
            }
        }
        return toAjax(categoryService.deleteCategoryByIds(ids));
    }
} 