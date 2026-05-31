package com.ruoyi.chigua.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.chigua.domain.Advertisement;

/**
 * 广告管理 数据层
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface AdvertisementMapper
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
     * 校验广告标题是否唯一
     * 
     * @param title 广告标题
     * @return 广告信息
     */
    public Advertisement checkAdvertisementTitleUnique(String title);

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
     * 删除广告
     * 
     * @param id 广告主键
     * @return 结果
     */
    public int deleteAdvertisementById(Long id);

    /**
     * 批量删除广告
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAdvertisementByIds(Long[] ids);

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
     * 更新点击统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    public int updateClickCount(Long id);

    /**
     * 更新展示统计
     * 
     * @param id 广告ID
     * @return 结果
     */
    public int updateImpressionCount(Long id);

    /**
     * 根据广告位置查询广告
     * 
     * @param position 广告位置
     * @return 广告集合
     */
    public List<Advertisement> selectAdsByPosition(String position);

    /**
     * 根据广告类型查询广告
     * 
     * @param adType 广告类型
     * @return 广告集合
     */
    public List<Advertisement> selectAdsByType(String adType);

    /**
     * 查询分类列表顶部横幅广告
     * 
     * @param categoryId 分类ID
     * @return 广告集合
     */
    public List<Advertisement> selectWebCategoryAds(Long categoryId);

    /**
     * 查询分类列表底部横幅广告
     * 
     * @param categoryId 分类ID
     * @return 广告集合
     */
    public List<Advertisement> selectWebCategoryBottomAds(Long categoryId);

    /**
     * 根据应用类型查询Logo广告
     * 
     * @param appType 应用类型
     * @return 广告集合
     */
    public List<Advertisement> selectLogoAdsByAppType(String appType);

    /**
     * 根据多个位置和分类查询广告
     * 
     * @param positions 广告位置列表
     * @param categoryId 分类ID
     * @return 广告集合
     */
    public List<Advertisement> selectAdsByPositionsAndCategory(@Param("positions") List<String> positions, @Param("categoryId") Long categoryId);

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