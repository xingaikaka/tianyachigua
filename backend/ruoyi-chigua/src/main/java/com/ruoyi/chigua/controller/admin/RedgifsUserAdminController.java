package com.ruoyi.chigua.controller.admin;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.chigua.service.ChiguaUrlService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.RedgifsUser;
import com.ruoyi.chigua.service.IRedgifsUserService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * RedGifs用户管理Controller（管理后台）
 * 
 * @author ruoyi
 * @date 2026-02-13
 */
@RestController
@RequestMapping("/chigua/redgifs/user")
public class RedgifsUserAdminController extends BaseController
{
    @Autowired
    private IRedgifsUserService redgifsUserService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询RedGifs用户列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(RedgifsUser redgifsUser)
    {
        startPage();
        List<RedgifsUser> list = redgifsUserService.selectRedGifsUserList(redgifsUser);
        
        logger.info("查询到 {} 个用户", list.size());
        
        // 为每个用户的头像生成签名URL
        for (RedgifsUser user : list) {
            String originalUrl = user.getProfileImageUrl();
            logger.info("用户 {} 原始头像URL: {}", user.getUsername(), originalUrl);
            processUserUrls(user);
            logger.info("用户 {} 处理后头像URL: {}", user.getUsername(), user.getProfileImageUrl());
        }
        
        return getDataTable(list);
    }

    /**
     * 导出RedGifs用户列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:export')")
    @Log(title = "RedGifs用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RedgifsUser redgifsUser)
    {
        List<RedgifsUser> list = redgifsUserService.selectRedGifsUserList(redgifsUser);
        ExcelUtil<RedgifsUser> util = new ExcelUtil<RedgifsUser>(RedgifsUser.class);
        util.exportExcel(response, list, "RedGifs用户数据");
    }

    /**
     * 获取RedGifs用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        RedgifsUser user = redgifsUserService.selectRedGifsUserById(id);
        if (user != null) {
            processUserUrls(user);
        }
        return success(user);
    }

    /**
     * 新增RedGifs用户
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:add')")
    @Log(title = "RedGifs用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.insertRedGifsUser(redgifsUser));
    }

    /**
     * 修改RedGifs用户
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @Log(title = "RedGifs用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.updateRedGifsUser(redgifsUser));
    }

    /**
     * 修改用户状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @Log(title = "RedGifs用户状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@RequestBody RedgifsUser redgifsUser)
    {
        redgifsUser.setStatus(redgifsUser.getStatus());
        return toAjax(redgifsUserService.updateRedGifsUser(redgifsUser));
    }

    /**
     * 修改用户推荐状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @Log(title = "RedGifs用户推荐", businessType = BusinessType.UPDATE)
    @PutMapping("/recommended")
    public AjaxResult updateRecommended(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.updateRedGifsUser(redgifsUser));
    }

    /**
     * 修改用户排序权重
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @Log(title = "RedGifs用户排序", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult updateSort(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.updateRedGifsUser(redgifsUser));
    }

    /**
     * 删除RedGifs用户
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:remove')")
    @Log(title = "RedGifs用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(redgifsUserService.deleteRedGifsUserByIds(ids));
    }
    
    /**
     * 为用户头像生成 Worker 签名（管理后台始终走 tycgimage1.org，服务端解密）
     */
    private void processUserUrls(RedgifsUser user) {
        try {
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()
                    && !user.getProfileImageUrl().startsWith("http")) {
                user.setProfileImageUrl(chiguaUrlService.generateWorkerUrl(
                    user.getProfileImageUrl(), ChiguaUrlService.ResourceType.COVER));
            }
        } catch (Exception e) {
            logger.warn("为用户{}生成URL签名失败: {}", user.getUsername(), e.getMessage());
        }
    }
}
