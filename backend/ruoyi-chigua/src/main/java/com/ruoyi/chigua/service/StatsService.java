package com.ruoyi.chigua.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.vo.TopVideoItem;
import com.ruoyi.chigua.domain.vo.CategoryClickItem;

public interface StatsService {
    Map<String, Object> getOverview(LocalDate date);
    List<TopVideoItem> getTopVideos(LocalDate date, String range, int limit);
    
    /**
     * 获取分类点击排行
     * @param period day|week|month
     * @param limit 返回数量
     */
    List<CategoryClickItem> getCategoryRanking(String period, int limit);
    
    /**
     * 获取时序数据
     * @param metric dau|uv|pv|searches|category_clicks|play_starts
     * @param period day|month
     * @return { labels: [], values: [] }
     */
    Map<String, Object> getTimeSeries(String metric, String period);

    /**
     * 将指定日期（通常为昨日）的统计从Redis聚合落库。
     */
    void aggregateDailyToMySQL(LocalDate date);

    /**
     * 清理指定Redis key
     * @param key Redis key
     * @return 是否成功清理
     */
    boolean cleanupRedisKey(String key);

    /**
     * 批量清理指定日期的Redis统计数据
     * @param date 日期
     * @return 清理的key数量
     */
    int cleanupRedisStatsForDate(LocalDate date);
    
    // ========== 新增Top100相关统计接口 ==========
    
    /**
     * 获取Top100综合统计面板数据
     * @param startDate 查询开始日期
     * @param endDate 查询结束日期
     * @return 包含各种Top100统计的综合数据
     */
    Map<String, Object> getTop100Dashboard(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取7日内活跃视频统计
     * @param date 查询结束日期
     * @return 活跃视频相关统计
     */
    Map<String, Object> getActiveVideoStats7Days(LocalDate date);
    
    /**
     * 获取播放量分布统计（7日内数据）
     * @param date 查询结束日期
     * @return 不同播放量区间的视频分布
     */
    Map<String, Object> getPlayCountDistribution7Days(LocalDate date);
    
    /**
     * 获取播放量趋势数据（固定7天）
     * @param date 查询结束日期
     * @return 近7天的播放量趋势
     */
    Map<String, Object> getPlayTrend7Days(LocalDate date);
    
    /**
     * 获取新视频排行榜（发布7天内）
     * @param date 查询日期
     * @param limit 返回数量
     * @return 新视频排行榜
     */
    List<TopVideoItem> getNewVideoRanking(LocalDate date, int limit);
    
    /**
     * 获取分类播放量统计（7日内数据）
     * @param date 查询结束日期
     * @return 各分类播放量占比
     */
    List<Map<String, Object>> getCategoryPlayStats7Days(LocalDate date);
    
    /**
     * 清理7天前的播放记录数据
     * @return 清理的记录数
     */
    int cleanupOldPlayRecords();
    
    /**
     * 获取每日播放趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日播放趋势数据
     */
    List<Map<String, Object>> getDailyPlayTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定日期区间的留存数据列表
     */
    List<Map<String, Object>> getRetentionSeries(LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定区间的留存汇总指标
     */
    Map<String, Object> getRetentionSummary(LocalDate startDate, LocalDate endDate);

    /**
     * 获取搜索关键词排行
     * @param date       单日查询（优先读 Redis，降级读 MySQL）；与 startDate/endDate 二选一
     * @param startDate  日期范围起始（查 MySQL 聚合）
     * @param endDate    日期范围结束
     * @param limit      返回数量上限
     * @return 关键词列表，每项含 keyword 和 count
     */
    List<Map<String, Object>> getSearchKeywords(LocalDate date, LocalDate startDate, LocalDate endDate, int limit);

    /**
     * 获取指定日期按小时的活跃事件量（24小时分布）
     * @param date 查询日期，默认今日
     * @return 24条记录，每条含 hour(0-23) 和 count
     */
    List<Map<String, Object>> getHourlyActivity(LocalDate date);

    /**
     * 获取零结果搜索关键词排行
     * @param date  查询日期，读 Redis ZSet search:kw:noresult:yyyyMMdd
     * @param limit 返回数量上限
     * @return 关键词列表，每项含 keyword 和 count
     */
    List<Map<String, Object>> getNoResultKeywords(LocalDate date, int limit);

    /**
     * 获取内容健康度分布
     * @param days 统计近N天的数据
     * @return 包含 zeroPlays/lowPlays/midPlays/highPlays/totalActive 的统计map
     */
    Map<String, Object> getContentHealth(int days);
}

