package com.ruoyi.chigua.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.chigua.domain.AdStatistics;
import com.ruoyi.chigua.domain.query.AdStatisticsQuery;

/**
 * 广告统计 数据层
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
public interface AdStatisticsMapper
{
    /**
     * 查询广告统计
     * 
     * @param id 广告统计主键
     * @return 广告统计
     */
    public AdStatistics selectAdStatisticsById(Long id);

    /**
     * 根据广告ID和日期查询统计记录
     * 
     * @param adId 广告ID
     * @param statDate 统计日期
     * @return 广告统计
     */
    public AdStatistics selectByAdIdAndDate(@Param("adId") Long adId, @Param("statDate") Date statDate);

    /**
     * 查询广告统计列表
     * 
     * @param query 查询参数
     * @return 广告统计集合
     */
    public List<AdStatistics> selectAdStatisticsList(AdStatisticsQuery query);

    /**
     * 新增广告统计
     * 
     * @param adStatistics 广告统计
     * @return 结果
     */
    public int insertAdStatistics(AdStatistics adStatistics);

    /**
     * 修改广告统计
     * 
     * @param adStatistics 广告统计
     * @return 结果
     */
    public int updateAdStatistics(AdStatistics adStatistics);

    /**
     * 删除广告统计
     * 
     * @param id 广告统计主键
     * @return 结果
     */
    public int deleteAdStatisticsById(Long id);

    /**
     * 批量删除广告统计
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAdStatisticsByIds(Long[] ids);

    /**
     * 获取指定时间段内的所有用户（去重）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    public List<String> selectDistinctUsers(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 按广告ID分组统计指定时间段内的点击量
     * 
     * @param query 查询参数
     * @return 统计结果
     */
    public List<AdStatistics> selectStatisticsSummary(AdStatisticsQuery query);

    /**
     * 获取广告在指定时间段内的用户点击详情
     * 
     * @param adId 广告ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户点击详情
     */
    public List<AdStatistics> selectUserClickDetails(@Param("adId") Long adId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 插入或更新广告点击统计（处理并发问题）
     */
    public int insertOrUpdateStatistics(@Param("adId") Long adId, @Param("adTitle") String adTitle, 
                                       @Param("adType") String adType, @Param("position") String position, 
                                       @Param("statDate") Date statDate);

    /**
     * 插入或更新广告曝光统计（处理并发问题）
     */
    public int insertOrUpdateImpression(@Param("adId") Long adId, @Param("adTitle") String adTitle,
                                        @Param("adType") String adType, @Param("position") String position,
                                        @Param("statDate") Date statDate);

    /**
     * 图表：每日点击/曝光趋势（按日期汇总）
     */
    public List<Map<String, Object>> selectDailyTrend(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 图表：按广告类型聚合点击量
     */
    public List<Map<String, Object>> selectClicksByType(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 图表：按广告位置聚合点击量
     */
    public List<Map<String, Object>> selectClicksByPosition(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 图表：Top N 广告点击排行
     */
    public List<Map<String, Object>> selectTopAds(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("limit") int limit);

    /**
     * 获取指定广告在时间段内各广告标题（即广告名）的去重列表，用于筛选下拉
     */
    public List<String> selectDistinctAdNames(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
