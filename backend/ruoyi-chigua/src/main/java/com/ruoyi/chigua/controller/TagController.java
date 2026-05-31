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
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.service.ITagService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.constant.UserConstants;

/**
 * 标签Controller
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@RestController
@RequestMapping("/chigua/tag")
public class TagController extends BaseController
{
    @Autowired
    private ITagService tagService;

    /**
     * 查询标签列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:list')")
    @GetMapping("/list")
    public TableDataInfo list(Tag tag)
    {
        startPage();
        startOrderBy();
        List<Tag> list = tagService.selectTagList(tag);
        return getDataTable(list);
    }

    /**
     * 导出标签列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:export')")
    @Log(title = "标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Tag tag)
    {
        List<Tag> list = tagService.selectTagList(tag);
        ExcelUtil<Tag> util = new ExcelUtil<Tag>(Tag.class);
        util.exportExcel(response, list, "标签数据");
    }

    /**
     * 获取标签详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(tagService.selectTagById(id));
    }

    /**
     * 新增标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:add')")
    @Log(title = "标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Tag tag)
    {
        if (!tagService.checkTagNameUnique(tag))
        {
            return AjaxResult.error("新增标签'" + tag.getName() + "'失败，标签名称已存在");
        }
        return toAjax(tagService.insertTag(tag));
    }

    /**
     * 修改标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:edit')")
    @Log(title = "标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Tag tag)
    {
        if (!tagService.checkTagNameUnique(tag))
        {
            return AjaxResult.error("修改标签'" + tag.getName() + "'失败，标签名称已存在");
        }
        return toAjax(tagService.updateTag(tag));
    }

    /**
     * 删除标签
     */
    @PreAuthorize("@ss.hasPermi('chigua:tag:remove')")
    @Log(title = "标签", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(tagService.deleteTagByIds(ids));
    }
} 