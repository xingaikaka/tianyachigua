package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.Advertisement;

/**
 * 广告管理Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IAdvertisementService 
{
    /**
     * 查询广告
     * 
     * @param id 广告主键
     * @return 广告
     */
    public Advertisement selectAdvertisementById(Long id);

    /**
     * 查询广告列表
     * 
     * @param advertisement 广告
     * @return 广告集合
     */
    public List<Advertisement> selectAdvertisementList(Advertisement advertisement);

    /**
     * 新增广告
     * 
     * @param advertisement 广告
     * @return 结果
     */
    public int insertAdvertisement(Advertisement advertisement);

    /**
     * 修改广告
     * 
     * @param advertisement 广告
     * @return 结果
     */
    public int updateAdvertisement(Advertisement advertisement);

    /**
     * 批量删除广告
     * 
     * @param ids 需要删除的广告主键集合
     * @return 结果
     */
    public int deleteAdvertisementByIds(Long[] ids);

    /**
     * 删除广告信息
     * 
     * @param id 广告主键
     * @return 结果
     */
    public int deleteAdvertisementById(Long id);

    /**
     * 校验广告标题是否唯一
     * 
     * @param advertisement 广告信息
     * @return 结果
     */
    public boolean checkAdvertisementTitleUnique(Advertisement advertisement);

    /**
     * 按位置查询有效广告
     * 
     * @param position 广告位置
     * @return 广告集合
     */
    public List<Advertisement> selectAdvertisementByPosition(String position);

    /**
     * 按分类查询有效广告
     * 
     * @param categoryId 分类ID
     * @return 广告集合
     */
    public List<Advertisement> selectAdvertisementByCategory(Long categoryId);

    /**
     * 广告点击统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    public int clickAdvertisement(Long id);

    /**
     * 广告展示统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    public int impressionAdvertisement(Long id);

    /**
     * 根据分类ID获取短视频广告
     * 
     * @param categoryId 分类ID
     * @return 短视频广告集合
     */
    public List<Advertisement> selectShortVideoAdsByCategory(Long categoryId);

    /**
     * 根据分类ID获取分页模式广告
     * 
     * @param categoryId 分类ID
     * @return 分页模式广告集合
     */
    public List<Advertisement> selectPagedAdsByCategory(Long categoryId);

    // ===== 定时任务相关方法 =====
    
    /**
     * 查询即将到期的广告
     * 
     * @param days 天数（查询多少天内到期的广告）
     * @return 广告集合
     */
    public List<Advertisement> selectExpiringAdvertisements(int days);

    /**
     * 查询已过期但仍启用的广告
     * 
     * @return 广告集合
     */
    public List<Advertisement> selectExpiredAdvertisements();

    /**
     * 批量禁用过期广告
     * 
     * @return 影响的行数
     */
    public int disableExpiredAdvertisements();
} 