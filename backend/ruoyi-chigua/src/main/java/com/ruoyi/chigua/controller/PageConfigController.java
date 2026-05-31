package com.ruoyi.chigua.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.PageConfig;
import com.ruoyi.chigua.service.IPageConfigService;
import com.ruoyi.chigua.service.WebRichTextProcessorService;
import com.ruoyi.chigua.service.ChiguaUrlService;

/**
 * 页面配置Controller
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
@RestController
@RequestMapping("/chigua/pageconfig")
public class PageConfigController extends BaseController
{
    @Autowired
    private IPageConfigService pageConfigService;

    @Autowired
    private WebRichTextProcessorService webRichTextProcessorService;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询页面配置列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(PageConfig pageConfig)
    {
        startPage();
        List<PageConfig> list = pageConfigService.selectPageConfigList(pageConfig);
        return getDataTable(list);
    }

    /**
     * 导出页面配置列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:export')")
    @Log(title = "页面配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PageConfig pageConfig)
    {
        List<PageConfig> list = pageConfigService.selectPageConfigList(pageConfig);
        ExcelUtil<PageConfig> util = new ExcelUtil<PageConfig>(PageConfig.class);
        util.exportExcel(response, list, "页面配置数据");
    }

    /**
     * 获取页面配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:query')")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable("configId") Long configId)
    {
        PageConfig cfg = pageConfigService.selectPageConfigById(configId);
        if (cfg == null) {
            return success(cfg);
        }
        // 富文本类型：添加签名URL
        if (com.ruoyi.common.utils.StringUtils.isNotEmpty(cfg.getRichContent())) {
            try {
                String processed = webRichTextProcessorService.processRichTextForAdminEditor(cfg.getRichContent());
                PageConfig copy = new PageConfig();
                org.springframework.beans.BeanUtils.copyProperties(cfg, copy);
                copy.setRichContent(processed);
                return success(copy);
            } catch (Exception ignore) { }
        }
        // 图片类型：basicContent 存的是 R2 key，返回时用 Worker 签名
        if ("image".equals(cfg.getConfigType()) && com.ruoyi.common.utils.StringUtils.isNotEmpty(cfg.getBasicContent())) {
            try {
                String resourcePath = cfg.getBasicContent();
                String workerUrl = chiguaUrlService.generateWorkerUrl(resourcePath, ChiguaUrlService.ResourceType.IMAGE);
                PageConfig copy = new PageConfig();
                org.springframework.beans.BeanUtils.copyProperties(cfg, copy);
                copy.setBasicContent(workerUrl);
                return success(copy);
            } catch (Exception ignore) { }
        }
        return success(cfg);
    }

    /**
     * 根据配置键值获取页面配置
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:query')")
    @GetMapping(value = "/key/{configKey}")
    public AjaxResult getInfoByKey(@PathVariable("configKey") String configKey)
    {
        PageConfig cfg = pageConfigService.selectPageConfigByKey(configKey);
        if (cfg != null && com.ruoyi.common.utils.StringUtils.isNotEmpty(cfg.getRichContent())) {
            try {
                // 管理端编辑器需要带 &decrypt=true
                String processed = webRichTextProcessorService.processRichTextForAdminEditor(cfg.getRichContent());
                PageConfig copy = new PageConfig();
                org.springframework.beans.BeanUtils.copyProperties(cfg, copy);
                copy.setRichContent(processed);
                return success(copy);
            } catch (Exception ignore) { }
        }
        return success(cfg);
    }

    /**
     * 根据配置类型获取页面配置列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:query')")
    @GetMapping(value = "/type/{configType}")
    public AjaxResult getListByType(@PathVariable("configType") String configType)
    {
        List<PageConfig> list = pageConfigService.selectPageConfigListByType(configType);
        return success(list);
    }

    /**
     * 根据配置分类获取页面配置列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:query')")
    @GetMapping(value = "/category/{configCategory}")
    public AjaxResult getListByCategory(@PathVariable("configCategory") String configCategory)
    {
        List<PageConfig> list = pageConfigService.selectPageConfigListByCategory(configCategory);
        return success(list);
    }

    /**
     * 新增页面配置
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:add')")
    @Log(title = "页面配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PageConfig pageConfig)
    {
        if (!pageConfigService.checkConfigKeyUnique(pageConfig))
        {
            return error("新增页面配置'" + pageConfig.getConfigName() + "'失败，配置键值已存在");
        }
        pageConfig.setCreateBy(getUsername());
        return toAjax(pageConfigService.insertPageConfig(pageConfig));
    }

    /**
     * 修改页面配置
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:edit')")
    @Log(title = "页面配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PageConfig pageConfig)
    {
        if (!pageConfigService.checkConfigKeyUnique(pageConfig))
        {
            return error("修改页面配置'" + pageConfig.getConfigName() + "'失败，配置键值已存在");
        }
        pageConfig.setUpdateBy(getUsername());
        return toAjax(pageConfigService.updatePageConfig(pageConfig));
    }

    /**
     * 删除页面配置
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:remove')")
    @Log(title = "页面配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        return toAjax(pageConfigService.deletePageConfigByIds(configIds));
    }

    /**
     * 获取当前配置版本号
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:query')")
    @GetMapping("/version")
    public AjaxResult getCurrentVersion()
    {
        String version = pageConfigService.getCurrentConfigVersion();
        return success(version);
    }

    /**
     * 强制刷新配置缓存
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:edit')")
    @Log(title = "页面配置", businessType = BusinessType.OTHER)
    @PostMapping("/refresh")
    public AjaxResult refreshCache()
    {
        pageConfigService.refreshConfigCache();
        return success("缓存刷新成功");
    }

    /**
     * 清理配置缓存
     */
    @PreAuthorize("@ss.hasPermi('chigua:pageconfig:edit')")
    @Log(title = "页面配置", businessType = BusinessType.OTHER)
    @PostMapping("/clear")
    public AjaxResult clearCache()
    {
        pageConfigService.refreshConfigCache();
        return success("缓存清理成功");
    }
}