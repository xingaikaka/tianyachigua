package com.ruoyi.chigua.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.service.StatsService;

/**
 * 统计任务测试Controller
 * 用于测试统计数据聚合和清理功能
 * 
 * @author chigua
 */
@RestController
@RequestMapping("/chigua/stats/task")
public class StatsTaskTestController 
{
    @Autowired
    private StatsService statsService;

    /**
     * 手动触发统计数据聚合（测试用）
     */
    @PostMapping("/aggregate")
    public AjaxResult manualAggregate(@RequestParam(value = "date", required = false) String dateStr) 
    {
        try {
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            
            statsService.aggregateDailyToMySQL(date);
            
            return AjaxResult.success("统计数据聚合完成，日期: " + date);
        } catch (Exception e) {
            return AjaxResult.error("统计数据聚合失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发Redis数据清理（测试用）
     */
    @PostMapping("/cleanup")
    public AjaxResult manualCleanup(@RequestParam(value = "date", required = false) String dateStr) 
    {
        try {
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now().minusDays(7);
            
            int cleanedCount = statsService.cleanupRedisStatsForDate(date);
            
            return AjaxResult.success("Redis数据清理完成，日期: " + date + "，清理了 " + cleanedCount + " 个key");
        } catch (Exception e) {
            return AjaxResult.error("Redis数据清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定日期的统计概览（测试用）
     */
    @GetMapping("/overview")
    public AjaxResult getStatsOverview(@RequestParam(value = "date", required = false) String dateStr) 
    {
        try {
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            
            java.util.Map<String, Object> overview = statsService.getOverview(date);
            
            return AjaxResult.success(overview);
        } catch (Exception e) {
            return AjaxResult.error("获取统计概览失败: " + e.getMessage());
        }
    }

    /**
     * 清理单个Redis key（测试用）
     */
    @PostMapping("/cleanup-key")
    public AjaxResult cleanupSingleKey(@RequestParam("key") String key) 
    {
        try {
            boolean success = statsService.cleanupRedisKey(key);
            
            if (success) {
                return AjaxResult.success("Redis key清理成功: " + key);
            } else {
                return AjaxResult.error("Redis key清理失败: " + key);
            }
        } catch (Exception e) {
            return AjaxResult.error("Redis key清理异常: " + e.getMessage());
        }
    }

    /**
     * 获取时序数据（测试用）
     */
    @GetMapping("/timeseries")
    public AjaxResult getTimeSeries(
            @RequestParam(value = "metric", defaultValue = "dau") String metric,
            @RequestParam(value = "period", defaultValue = "day") String period) 
    {
        try {
            java.util.Map<String, Object> timeSeries = statsService.getTimeSeries(metric, period);
            
            return AjaxResult.success(timeSeries);
        } catch (Exception e) {
            return AjaxResult.error("获取时序数据失败: " + e.getMessage());
        }
    }
}
