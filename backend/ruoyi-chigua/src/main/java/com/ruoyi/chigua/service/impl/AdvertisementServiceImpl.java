package com.ruoyi.chigua.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.mapper.AdvertisementMapper;
import com.ruoyi.chigua.service.IAdvertisementService;
import com.ruoyi.chigua.service.CacheRefreshService;

/**
 * 广告管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class AdvertisementServiceImpl implements IAdvertisementService 
{
    @Autowired
    private AdvertisementMapper advertisementMapper;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 查询广告
     * 
     * @param id 广告主键
     * @return 广告
     */
    @Override
    public Advertisement selectAdvertisementById(Long id)
    {
        return advertisementMapper.selectAdvertisementById(id);
    }

    /**
     * 查询广告列表
     * 
     * @param advertisement 广告
     * @return 广告
     */
    @Override
    public List<Advertisement> selectAdvertisementList(Advertisement advertisement)
    {
        return advertisementMapper.selectAdvertisementList(advertisement);
    }

    /**
     * 新增广告
     * 
     * @param advertisement 广告
     * @return 结果
     */
    @Override
    public int insertAdvertisement(Advertisement advertisement)
    {
        int result = advertisementMapper.insertAdvertisement(advertisement);
        if (result > 0) {
            cacheRefreshService.refreshAdvertisementCache();
        }
        return result;
    }

    /**
     * 修改广告
     * 
     * @param advertisement 广告
     * @return 结果
     */
    @Override
    public int updateAdvertisement(Advertisement advertisement)
    {
        int result = advertisementMapper.updateAdvertisement(advertisement);
        if (result > 0) {
            cacheRefreshService.refreshAdvertisementCache();
        }
        return result;
    }

    /**
     * 批量删除广告
     * 
     * @param ids 需要删除的广告主键
     * @return 结果
     */
    @Override
    public int deleteAdvertisementByIds(Long[] ids)
    {
        int result = advertisementMapper.deleteAdvertisementByIds(ids);
        if (result > 0) {
            cacheRefreshService.refreshAdvertisementCache();
        }
        return result;
    }

    /**
     * 删除广告信息
     * 
     * @param id 广告主键
     * @return 结果
     */
    @Override
    public int deleteAdvertisementById(Long id)
    {
        int result = advertisementMapper.deleteAdvertisementById(id);
        if (result > 0) {
            cacheRefreshService.refreshAdvertisementCache();
        }
        return result;
    }

    /**
     * 校验广告标题是否唯一
     * 
     * @param advertisement 广告信息
     * @return 结果
     */
    @Override
    public boolean checkAdvertisementTitleUnique(Advertisement advertisement)
    {
        Long id = StringUtils.isNull(advertisement.getId()) ? -1L : advertisement.getId();
        Advertisement info = advertisementMapper.checkAdvertisementTitleUnique(advertisement.getTitle());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 按位置查询有效广告
     * 
     * @param position 广告位置
     * @return 广告集合
     */
    @Override
    public List<Advertisement> selectAdvertisementByPosition(String position)
    {
        return advertisementMapper.selectAdvertisementByPosition(position);
    }

    /**
     * 按分类查询有效广告
     * 
     * @param categoryId 分类ID
     * @return 广告集合
     */
    @Override
    public List<Advertisement> selectAdvertisementByCategory(Long categoryId)
    {
        return advertisementMapper.selectAdvertisementByCategory(categoryId);
    }

    /**
     * 广告点击统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    @Override
    public int clickAdvertisement(Long id)
    {
        return advertisementMapper.updateClickCount(id);
    }

    /**
     * 广告展示统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    @Override
    public int impressionAdvertisement(Long id)
    {
        return advertisementMapper.updateImpressionCount(id);
    }

    // ===== 定时任务相关方法实现 =====
    
    /**
     * 查询即将到期的广告
     */
    @Override
    public List<Advertisement> selectExpiringAdvertisements(int days)
    {
        return advertisementMapper.selectExpiringAdvertisements(days);
    }

    /**
     * 查询已过期但仍启用的广告
     */
    @Override
    public List<Advertisement> selectExpiredAdvertisements()
    {
        return advertisementMapper.selectExpiredAdvertisements();
    }

    /**
     * 批量禁用过期广告
     */
    @Override
    public int disableExpiredAdvertisements()
    {
        return advertisementMapper.disableExpiredAdvertisements();
    }

    /**
     * 根据分类ID获取短视频广告
     */
    @Override
    public List<Advertisement> selectShortVideoAdsByCategory(Long categoryId)
    {
        return advertisementMapper.selectShortVideoAdsByCategory(categoryId);
    }

    /**
     * 根据分类ID获取分页模式广告
     */
    @Override
    public List<Advertisement> selectPagedAdsByCategory(Long categoryId)
    {
        return advertisementMapper.selectPagedAdsByCategory(categoryId);
    }
} 