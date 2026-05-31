package com.ruoyi.chigua.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.chigua.domain.AdStatistics;
import com.ruoyi.chigua.domain.query.AdStatisticsQuery;
import com.ruoyi.chigua.domain.vo.AdStatisticsVO;
import com.ruoyi.chigua.service.IAdStatisticsService;

/**
 * 广告统计Controller
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
@RestController
@RequestMapping("/chigua/advertisement/statistics")
public class AdStatisticsController extends BaseController
{
    @Autowired
    private IAdStatisticsService adStatisticsService;

    /**
     * 查询广告统计列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/list")
    public TableDataInfo list(AdStatisticsQuery query)
    {
        startPage();
        List<AdStatisticsVO> list = adStatisticsService.selectStatisticsForDisplay(query);
        return getDataTable(list);
    }

    /**
     * 导出广告统计列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @Log(title = "广告统计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AdStatisticsQuery query)
    {
        List<AdStatisticsVO> list = adStatisticsService.selectStatisticsForDisplay(query);
        ExcelUtil<AdStatisticsVO> util = new ExcelUtil<AdStatisticsVO>(AdStatisticsVO.class);
        util.exportExcel(response, list, "广告统计数据");
    }

    /**
     * 获取广告统计详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(adStatisticsService.selectAdStatisticsById(id));
    }

    /**
     * 获取时间段内有记录的广告名列表（用于筛选下拉）
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/ad-names")
    public AjaxResult getAdNames(@RequestParam(required = false) Date startDate,
                                 @RequestParam(required = false) Date endDate)
    {
        if (startDate == null || endDate == null) {
            endDate = new Date();
            startDate = new Date(endDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        }
        List<String> names = adStatisticsService.selectDistinctAdNames(startDate, endDate);
        return success(names);
    }

    /**
     * 图表：每日点击/曝光趋势
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/chart/trend")
    public AjaxResult chartTrend(@RequestParam(required = false) Date startDate,
                                 @RequestParam(required = false) Date endDate)
    {
        if (startDate == null || endDate == null) {
            endDate = new Date();
            startDate = new Date(endDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        }
        List<Map<String, Object>> data = adStatisticsService.getChartDailyTrend(startDate, endDate);
        return success(data);
    }

    /**
     * 图表：按广告类型聚合点击量
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/chart/by-type")
    public AjaxResult chartByType(@RequestParam(required = false) Date startDate,
                                  @RequestParam(required = false) Date endDate)
    {
        if (startDate == null || endDate == null) {
            endDate = new Date();
            startDate = new Date(endDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        }
        List<Map<String, Object>> data = adStatisticsService.getChartByType(startDate, endDate);
        return success(data);
    }

    /**
     * 图表：按广告位置聚合点击量
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/chart/by-position")
    public AjaxResult chartByPosition(@RequestParam(required = false) Date startDate,
                                      @RequestParam(required = false) Date endDate)
    {
        if (startDate == null || endDate == null) {
            endDate = new Date();
            startDate = new Date(endDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        }
        List<Map<String, Object>> data = adStatisticsService.getChartByPosition(startDate, endDate);
        return success(data);
    }

    /**
     * 图表：Top N 广告点击排行
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/chart/top-ads")
    public AjaxResult chartTopAds(@RequestParam(required = false) Date startDate,
                                  @RequestParam(required = false) Date endDate,
                                  @RequestParam(defaultValue = "10") int limit)
    {
        if (startDate == null || endDate == null) {
            endDate = new Date();
            startDate = new Date(endDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        }
        List<Map<String, Object>> data = adStatisticsService.getChartTopAds(startDate, endDate, limit);
        return success(data);
    }

    /**
     * 获取广告用户点击详情
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @GetMapping("/details/{adId}")
    public AjaxResult getClickDetails(@PathVariable("adId") Long adId,
                                     @RequestParam Date startDate,
                                     @RequestParam Date endDate)
    {
        List<AdStatistics> details = adStatisticsService.selectUserClickDetails(adId, startDate, endDate);
        return success(details);
    }

    /**
     * 新增广告统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @Log(title = "广告统计", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AdStatistics adStatistics)
    {
        return toAjax(adStatisticsService.insertAdStatistics(adStatistics));
    }

    /**
     * 修改广告统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @Log(title = "广告统计", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AdStatistics adStatistics)
    {
        return toAjax(adStatisticsService.updateAdStatistics(adStatistics));
    }

    /**
     * 删除广告统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:advertisement:statistics')")
    @Log(title = "广告统计", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(adStatisticsService.deleteAdStatisticsByIds(ids));
    }
}
