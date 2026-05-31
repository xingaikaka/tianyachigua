package com.ruoyi.chigua.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.Comment;
import com.ruoyi.chigua.service.ICommentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 评论Controller
 * 
 * @author chigua
 * @date 2025-01-22
 */
@RestController
@RequestMapping("/system/comment")
public class CommentController extends BaseController
{
    @Autowired
    private ICommentService commentService;

    /**
     * 查询评论列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:list')")
    @GetMapping("/list")
    public TableDataInfo list(Comment comment)
    {
        startPage();
        List<Comment> list = commentService.selectCommentList(comment);
        return getDataTable(list);
    }

    /**
     * 查询待审核评论列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:audit')")
    @GetMapping("/pending")
    public TableDataInfo pendingList(Comment comment)
    {
        startPage();
        List<Comment> list = commentService.selectPendingCommentList(comment);
        return getDataTable(list);
    }

    /**
     * 获取评论统计信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:list')")
    @GetMapping("/statistics")
    public AjaxResult statistics(Comment comment)
    {
        Comment statistics = commentService.getCommentStatistics(comment);
        Map<String, Object> data = new HashMap<>();
        data.put("total", statistics.getId());
        data.put("pending", statistics.getVideoId());
        data.put("approved", statistics.getParentId());
        data.put("rejected", statistics.getReplyCount());
        return AjaxResult.success(data);
    }

    /**
     * 导出评论列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:export')")
    @Log(title = "评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Comment comment)
    {
        List<Comment> list = commentService.selectCommentList(comment);
        ExcelUtil<Comment> util = new ExcelUtil<Comment>(Comment.class);
        util.exportExcel(response, list, "评论数据");
    }

    /**
     * 获取评论详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(commentService.selectCommentById(id));
    }

    /**
     * 新增评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:add')")
    @Log(title = "评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Comment comment)
    {
        return toAjax(commentService.insertComment(comment));
    }

    /**
     * 修改评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:edit')")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Comment comment)
    {
        return toAjax(commentService.updateComment(comment));
    }

    /**
     * 删除评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:remove')")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(commentService.deleteCommentByIds(ids));
    }

    /**
     * 审核评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:audit')")
    @Log(title = "评论审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public AjaxResult audit(@PathVariable("id") Long id, @RequestBody Map<String, Object> params)
    {
        Integer status = (Integer) params.get("status");
        String auditRemark = (String) params.get("auditRemark");
        Long auditUserId = SecurityUtils.getUserId();
        
        return toAjax(commentService.auditComment(id, status, auditUserId, auditRemark));
    }

    /**
     * 批量审核评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:audit')")
    @Log(title = "批量审核评论", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-audit")
    public AjaxResult batchAudit(@RequestBody Map<String, Object> params)
    {
        Long[] ids = ((List<?>) params.get("ids")).stream()
            .mapToLong(id -> Long.valueOf(id.toString()))
            .boxed()
            .toArray(Long[]::new);
        Integer status = (Integer) params.get("status");
        String auditRemark = (String) params.get("auditRemark");
        Long auditUserId = SecurityUtils.getUserId();
        
        return toAjax(commentService.batchAuditComments(ids, status, auditUserId, auditRemark));
    }

    /**
     * 置顶/取消置顶评论
     */
    @PreAuthorize("@ss.hasPermi('chigua:comment:edit')")
    @Log(title = "评论置顶", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/sticky")
    public AjaxResult sticky(@PathVariable("id") Long id, @RequestBody Map<String, Object> params)
    {
        Integer isSticky = (Integer) params.get("isSticky");
        return toAjax(commentService.updateCommentSticky(id, isSticky));
    }
} 