package com.ruoyi.chigua.controller;

import com.ruoyi.chigua.config.ChiguaProperties;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.service.StatsService;
import com.ruoyi.chigua.domain.vo.TopVideoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/stats")
public class StatsAdminController {

    private static final Logger logger = LoggerFactory.getLogger(StatsAdminController.class);

    @Resource
    private StatsService statsService;

    @Resource
    private ChiguaProperties chiguaProperties;

    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(defaultValue = "today") String date) {
        LocalDate d = LocalDate.now();
        if ("yesterday".equalsIgnoreCase(date)) {
            d = d.minusDays(1);
        }
        Map<String, Object> data = statsService.getOverview(d);
        return AjaxResult.success(data);
    }

    @GetMapping("/videos/top")
    public AjaxResult topVideos(@RequestParam(required = false) String date,
                                @RequestParam(defaultValue = "today") String range,
                                @RequestParam(defaultValue = "100") int limit) {
        LocalDate d = date == null ? LocalDate.now() : LocalDate.parse(date);
        List<TopVideoItem> list = statsService.getTopVideos(d, range, limit);
        return AjaxResult.success(list);
    }

    // 分类点击排行（period: day|week|month, limit: 返回数量）
    @GetMapping("/category/ranking")
    public AjaxResult categoryRanking(@RequestParam(defaultValue = "day") String period,
                                      @RequestParam(defaultValue = "10") int limit) {
        return AjaxResult.success(statsService.getCategoryRanking(period, limit));
    }

    // 时序数据：metric=dau|uv|pv|searches|category_clicks|play_starts, period=day|month
    @GetMapping("/timeseries")
    public AjaxResult timeSeries(@RequestParam String metric,
                                 @RequestParam(defaultValue = "day") String period,
                                 @RequestParam(required = false) Integer points) {
        Map<String, Object> data = statsService.getTimeSeries(metric, period);
        return AjaxResult.success(data);
    }

    // 手动触发汇总（默认汇总昨日）
    @PostMapping("/aggregate")
    public AjaxResult aggregate(@RequestParam(required = false) String date,
                                @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdminToken(token);
        LocalDate d = date == null ? LocalDate.now().minusDays(1) : LocalDate.parse(date);
        statsService.aggregateDailyToMySQL(d);
        return AjaxResult.success();
    }
    
    // ========== 新增Top100相关统计接口 ==========
    
    /**
     * 获取Top100综合统计面板
     */
    @GetMapping("/top100/dashboard")
    public AjaxResult top100Dashboard(@RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate) {
        LocalDate start = startDate == null ? LocalDate.now() : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        Map<String, Object> data = statsService.getTop100Dashboard(start, end);
        return AjaxResult.success(data);
    }
    
    /**
     * 获取活跃视频统计（7日内数据）
     */
    @GetMapping("/videos/active")
    public AjaxResult activeVideoStats(@RequestParam(required = false) String date) {
        LocalDate d = date == null ? LocalDate.now() : LocalDate.parse(date);
        Map<String, Object> data = statsService.getActiveVideoStats7Days(d);
        return AjaxResult.success(data);
    }
    

    
    /**
     * 获取分类播放量统计（7日内数据）
     */
    @GetMapping("/category/plays")
    public AjaxResult categoryPlayStats(@RequestParam(required = false) String date) {
        LocalDate d = date == null ? LocalDate.now() : LocalDate.parse(date);
        List<Map<String, Object>> data = statsService.getCategoryPlayStats7Days(d);
        return AjaxResult.success(data);
    }

    /**
     * 获取用户留存数据
     */
    @GetMapping("/retention")
    public AjaxResult retention(@RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        LocalDate start = startDate == null ? end.minusDays(29) : LocalDate.parse(startDate);
        Map<String, Object> result = new HashMap<>();
        result.put("series", statsService.getRetentionSeries(start, end));
        result.put("summary", statsService.getRetentionSummary(start, end));
        return AjaxResult.success(result);
    }
    
    /**
     * 手动清理7天前的播放记录
     */
    @PostMapping("/cleanup/play-records")
    public AjaxResult cleanupPlayRecords(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdminToken(token);
        int deletedCount = statsService.cleanupOldPlayRecords();
        return AjaxResult.success("清理完成，删除了 " + deletedCount + " 条记录");
    }
    
    /**
     * 获取快速上升视频排行
     */
    @GetMapping("/videos/rising")
    public AjaxResult risingVideos(@RequestParam(required = false) String date,
                                   @RequestParam(defaultValue = "20") int limit) {
        LocalDate d = date == null ? LocalDate.now() : LocalDate.parse(date);
        List<TopVideoItem> list = statsService.getTopVideos(d, "7d", limit);
        return AjaxResult.success(list);
    }
    
    /**
     * 获取每日播放趋势（固定最近7天）
     */
    @GetMapping("/daily-play-trend")
    public AjaxResult getDailyPlayTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        List<Map<String, Object>> data = statsService.getDailyPlayTrend(startDate, endDate);
        return AjaxResult.success(data);
    }

    /**
     * 搜索关键词排行
     * - 单日（date 参数）：优先读 Redis 实时数据，降级读 MySQL
     * - 日期范围（startDate + endDate）：查 MySQL 聚合
     */
    @GetMapping("/search/keywords")
    public AjaxResult searchKeywords(@RequestParam(required = false) String date,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate,
                                     @RequestParam(defaultValue = "200") int limit) {
        LocalDate d = date != null ? LocalDate.parse(date) : null;
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        // 三个参数都为空时默认查今日
        if (d == null && start == null) {
            d = LocalDate.now();
        }
        List<Map<String, Object>> data = statsService.getSearchKeywords(d, start, end, limit);
        return AjaxResult.success(data);
    }

    /**
     * 24小时活跃时段分布
     * @param date 查询日期，默认今日
     */
    @GetMapping("/hourly-activity")
    public AjaxResult hourlyActivity(@RequestParam(required = false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return AjaxResult.success(statsService.getHourlyActivity(d));
    }

    /**
     * 零结果搜索关键词排行（读 Redis，数据保留 45 天）
     * @param date  查询日期，默认今日
     * @param limit 返回数量，默认 20
     */
    @GetMapping("/search/no-result-keywords")
    public AjaxResult noResultKeywords(@RequestParam(required = false) String date,
                                       @RequestParam(defaultValue = "20") int limit) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return AjaxResult.success(statsService.getNoResultKeywords(d, limit));
    }

    /**
     * 内容库健康度分布
     * @param days 统计近N天，默认 30
     */
    @GetMapping("/content/health")
    public AjaxResult contentHealth(@RequestParam(defaultValue = "30") int days) {
        return AjaxResult.success(statsService.getContentHealth(days));
    }

    private void requireAdminToken(String token) {
        String expected = chiguaProperties.getSecurity() != null ? chiguaProperties.getSecurity().getStatsAdminToken() : null;
        if (expected == null || expected.isEmpty() || "change-me".equals(expected)) {
            logger.warn("统计接口未配置安全令牌，跳过校验，请尽快在配置中设置 chigua.security.stats-admin-token");
            return;
        }
        if (!expected.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无效的统计接口令牌");
        }
    }
}
