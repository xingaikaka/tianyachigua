package com.ruoyi.chigua.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.AdStatistics;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.domain.query.AdStatisticsQuery;
import com.ruoyi.chigua.domain.vo.AdStatisticsVO;

/**
 * 广告统计Service接口
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
public interface IAdStatisticsService 
{
    /**
     * 记录广告点击
     * 
     * @param advertisement 广告信息
     * @return 结果
     */
    public int recordClick(Advertisement advertisement);

    /**
     * 查询广告统计
     * 
     * @param id 广告统计主键
     * @return 广告统计
     */
    public AdStatistics selectAdStatisticsById(Long id);

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
     * 批量删除广告统计
     * 
     * @param ids 需要删除的广告统计主键集合
     * @return 结果
     */
    public int deleteAdStatisticsByIds(Long[] ids);

    /**
     * 删除广告统计信息
     * 
     * @param id 广告统计主键
     * @return 结果
     */
    public int deleteAdStatisticsById(Long id);

    /**
     * 获取指定时间段内的所有用户（去重）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    public List<String> selectDistinctUsers(Date startDate, Date endDate);

    /**
     * 查询广告统计汇总数据（用于统计页面显示）
     * 
     * @param query 查询参数
     * @return 统计VO列表
     */
    public List<AdStatisticsVO> selectStatisticsForDisplay(AdStatisticsQuery query);

    /**
     * 获取广告在指定时间段内的每日点击详情
     */
    public List<AdStatistics> selectUserClickDetails(Long adId, Date startDate, Date endDate);

    /**
     * 记录广告曝光
     */
    public int recordImpression(Advertisement advertisement);

    /**
     * 获取时间段内有记录的广告名列表（用于筛选下拉）
     */
    public List<String> selectDistinctAdNames(Date startDate, Date endDate);

    /**
     * 图表：每日点击/曝光趋势
     */
    public List<Map<String, Object>> getChartDailyTrend(Date startDate, Date endDate);

    /**
     * 图表：按广告类型聚合点击量
     */
    public List<Map<String, Object>> getChartByType(Date startDate, Date endDate);

    /**
     * 图表：按广告位置聚合点击量
     */
    public List<Map<String, Object>> getChartByPosition(Date startDate, Date endDate);

    /**
     * 图表：Top N 广告点击排行
     */
    public List<Map<String, Object>> getChartTopAds(Date startDate, Date endDate, int limit);
}
