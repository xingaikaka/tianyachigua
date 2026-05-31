package com.ruoyi.chigua.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.chigua.domain.AdStatistics;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.domain.query.AdStatisticsQuery;
import com.ruoyi.chigua.domain.vo.AdStatisticsVO;
import com.ruoyi.chigua.mapper.AdStatisticsMapper;
import com.ruoyi.chigua.mapper.AdvertisementMapper;
import com.ruoyi.chigua.service.IAdStatisticsService;

/**
 * 广告统计Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
@Service
public class AdStatisticsServiceImpl implements IAdStatisticsService 
{
    private static final Logger logger = LoggerFactory.getLogger(AdStatisticsServiceImpl.class);

    @Autowired
    private AdStatisticsMapper adStatisticsMapper;

    @Autowired
    private AdvertisementMapper advertisementMapper;

    /**
     * 记录广告点击（核心方法）
     */
    @Override
    @Transactional
    public int recordClick(Advertisement advertisement)
    {
        logger.info("📊 记录广告点击统计: adId={}, title={}", advertisement.getId(), advertisement.getTitle());
        
        try {
            Date today = DateUtils.getNowDate();
            
            // 使用 INSERT ... ON DUPLICATE KEY UPDATE 来处理并发问题
            int result = adStatisticsMapper.insertOrUpdateStatistics(
                advertisement.getId(),
                advertisement.getTitle(),
                advertisement.getAdType(),
                advertisement.getPosition(),
                today
            );
            
            logger.info("✅ 广告点击统计成功: adId={}, title={}", advertisement.getId(), advertisement.getTitle());
            return result;
            
        } catch (Exception e) {
            logger.error("❌ 记录广告点击统计失败: adId={}, error={}", advertisement.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 查询广告统计
     */
    @Override
    public AdStatistics selectAdStatisticsById(Long id)
    {
        return adStatisticsMapper.selectAdStatisticsById(id);
    }

    /**
     * 查询广告统计列表
     */
    @Override
    public List<AdStatistics> selectAdStatisticsList(AdStatisticsQuery query)
    {
        return adStatisticsMapper.selectAdStatisticsList(query);
    }

    /**
     * 新增广告统计
     */
    @Override
    public int insertAdStatistics(AdStatistics adStatistics)
    {
        return adStatisticsMapper.insertAdStatistics(adStatistics);
    }

    /**
     * 修改广告统计
     */
    @Override
    public int updateAdStatistics(AdStatistics adStatistics)
    {
        return adStatisticsMapper.updateAdStatistics(adStatistics);
    }

    /**
     * 批量删除广告统计
     */
    @Override
    public int deleteAdStatisticsByIds(Long[] ids)
    {
        return adStatisticsMapper.deleteAdStatisticsByIds(ids);
    }

    /**
     * 删除广告统计信息
     */
    @Override
    public int deleteAdStatisticsById(Long id)
    {
        return adStatisticsMapper.deleteAdStatisticsById(id);
    }

    /**
     * 获取指定时间段内的所有用户（去重）
     */
    @Override
    public List<String> selectDistinctUsers(Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectDistinctUsers(startDate, endDate);
    }

    /**
     * 查询广告统计汇总数据（用于统计页面显示）
     */
    @Override
    public List<AdStatisticsVO> selectStatisticsForDisplay(AdStatisticsQuery query)
    {
        logger.info("📈 查询广告统计汇总数据: {}", query);
        
        List<AdStatisticsVO> result = new ArrayList<>();
        
        // 如果没有指定时间范围，返回空结果
        if (query.getStartDate() == null || query.getEndDate() == null) {
            logger.warn("⚠️ 未指定时间范围，返回空结果");
            return result;
        }
        
        // 1) 直接获取分页后的汇总数据（SQL 已按点击量降序）
        List<AdStatistics> statistics = adStatisticsMapper.selectStatisticsSummary(query);
        com.github.pagehelper.Page<AdStatistics> pageMeta = null;
        if (statistics instanceof com.github.pagehelper.Page) {
            pageMeta = (com.github.pagehelper.Page<AdStatistics>) statistics;
        }

        if (statistics.isEmpty()) {
            logger.info("✅ 指定时间段内无点击数据");
            return result;
        }

        // 2) 一次性批量获取广告基础信息并映射
        Advertisement adQuery = new Advertisement();
        List<Advertisement> advertisements = advertisementMapper.selectAdvertisementList(adQuery);
        Map<Long, Advertisement> adMap = advertisements.stream()
            .collect(Collectors.toMap(Advertisement::getId, ad -> ad, (a, b) -> a));

        // 3) 逐条把汇总结果转为 VO（不再在 Java 层做二次过滤/重排，保持与分页一致）
        // 若存在分页元信息，则构造同页码/页大小的 Page<AdStatisticsVO>，以便 getDataTable 能读取正确 total
        com.github.pagehelper.Page<AdStatisticsVO> pageResult = null;
        if (pageMeta != null) {
            pageResult = new com.github.pagehelper.Page<>(pageMeta.getPageNum(), pageMeta.getPageSize());
            pageResult.setTotal(pageMeta.getTotal());
        }

        for (AdStatistics stat : statistics) {
            Advertisement ad = adMap.get(stat.getAdId());
            if (ad == null) {
                continue;
            }

            AdStatisticsVO vo = new AdStatisticsVO();
            vo.setId(ad.getId());
            vo.setTitle(ad.getTitle());
            vo.setDescription(ad.getDescription());
            vo.setAdType(ad.getAdType());
            vo.setAppType(ad.getAppType());
            vo.setPosition(ad.getPosition());
            vo.setCategoryId(ad.getCategoryId());
            vo.setImageUrl(ad.getImageUrl());
            vo.setIconName(ad.getIconName());
            vo.setLinkUrl(ad.getLinkUrl());
            vo.setLinkText(ad.getLinkText());
            vo.setSortOrder(ad.getSortOrder());
            vo.setStartDate(ad.getStartDate());
            vo.setEndDate(ad.getEndDate());
            vo.setStatus(ad.getStatus());
            vo.setIsGlobal(ad.getIsGlobal());
            vo.setClickCount(stat.getClickCount());

            String userDetails = generateUserDetails(stat.getAdId(), query.getStartDate(), query.getEndDate());
            vo.setUserDetails(userDetails);
            if (pageResult != null) {
                pageResult.add(vo);
            } else {
                result.add(vo);
            }
        }
        
        if (pageResult != null) {
            logger.info("✅ 查询完成，返回{}条统计记录（分页：total={}, pageNum={}, pageSize={})",
                pageResult.size(), pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
            return pageResult;
        } else {
            logger.info("✅ 查询完成，返回{}条统计记录", result.size());
            return result;
        }
    }

    /**
     * 获取广告在指定时间段内的每日点击详情
     */
    @Override
    public List<AdStatistics> selectUserClickDetails(Long adId, Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectUserClickDetails(adId, startDate, endDate);
    }

    /**
     * 记录广告曝光
     */
    @Override
    @Transactional
    public int recordImpression(Advertisement advertisement)
    {
        logger.info("👁️ 记录广告曝光统计: adId={}, title={}", advertisement.getId(), advertisement.getTitle());
        try {
            Date today = DateUtils.getNowDate();
            return adStatisticsMapper.insertOrUpdateImpression(
                advertisement.getId(),
                advertisement.getTitle(),
                advertisement.getAdType(),
                advertisement.getPosition(),
                today
            );
        } catch (Exception e) {
            logger.error("❌ 记录广告曝光统计失败: adId={}, error={}", advertisement.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 获取时间段内有记录的广告名列表（用于筛选下拉）
     */
    @Override
    public List<String> selectDistinctAdNames(Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectDistinctAdNames(startDate, endDate);
    }

    /**
     * 图表：每日点击/曝光趋势
     */
    @Override
    public List<Map<String, Object>> getChartDailyTrend(Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectDailyTrend(startDate, endDate);
    }

    /**
     * 图表：按广告类型聚合点击量
     */
    @Override
    public List<Map<String, Object>> getChartByType(Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectClicksByType(startDate, endDate);
    }

    /**
     * 图表：按广告位置聚合点击量
     */
    @Override
    public List<Map<String, Object>> getChartByPosition(Date startDate, Date endDate)
    {
        return adStatisticsMapper.selectClicksByPosition(startDate, endDate);
    }

    /**
     * 图表：Top N 广告点击排行
     */
    @Override
    public List<Map<String, Object>> getChartTopAds(Date startDate, Date endDate, int limit)
    {
        return adStatisticsMapper.selectTopAds(startDate, endDate, limit);
    }

    /**
     * 检查广告是否匹配查询条件
     */
    // 过滤逻辑如需保留，应前移至汇总 SQL 层；此处不再二次过滤，避免与分页冲突。

    /**
     * 生成每日点击趋势摘要字符串（按日期展示）
     */
    private String generateUserDetails(Long adId, Date startDate, Date endDate)
    {
        try {
            List<AdStatistics> details = selectUserClickDetails(adId, startDate, endDate);
            if (details.isEmpty()) {
                return "暂无点击记录";
            }
            // 按日期汇总
            Map<String, Integer> dateClickMap = new LinkedHashMap<>();
            for (AdStatistics detail : details) {
                String dateStr = detail.getStatDate() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(detail.getStatDate())
                    : "未知";
                dateClickMap.merge(dateStr, detail.getClickCount(), Integer::sum);
            }
            return dateClickMap.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue() + "次")
                .collect(Collectors.joining("; "));
        } catch (Exception e) {
            logger.error("❌ 生成点击趋势摘要失败: adId={}, error={}", adId, e.getMessage());
            return "获取详情失败";
        }
    }
}
