package com.ruoyi.chigua.service.web;

import java.util.List;
import com.ruoyi.chigua.domain.vo.web.WebAdvertisementVO;

/**
 * Web广告Service接口
 * 
 * @author ruoyi
 * @date 2025-01-21
 */
public interface IWebAdvertisementService 
{
    /**
     * 根据广告位置获取广告
     * 
     * @param position 广告位置
     * @return 广告列表
     */
    public List<WebAdvertisementVO> selectAdsByPosition(String position);

    /**
     * 根据广告类型获取广告
     * 
     * @param adType 广告类型
     * @return 广告列表
     */
    public List<WebAdvertisementVO> selectAdsByType(String adType);

    /**
     * 获取分类列表顶部横幅广告
     * 
     * @param categoryId 分类ID
     * @return 广告列表
     */
    public List<WebAdvertisementVO> selectCategoryAds(Long categoryId);

    /**
     * 获取分类列表底部横幅广告
     * 
     * @param categoryId 分类ID
     * @return 广告列表
     */
    public List<WebAdvertisementVO> selectCategoryBottomAds(Long categoryId);

    /**
     * 根据应用类型获取Logo广告
     * 
     * @param appType 应用类型
     * @return 广告列表
     */
    public List<WebAdvertisementVO> selectLogoAdsByAppType(String appType);

    /**
     * 增加广告点击统计
     * 
     * @param adId 广告ID
     * @return 结果
     */
    public int incrementClickCount(Long adId);

    /**
     * 增加广告展示统计
     * 
     * @param adId 广告ID
     * @return 结果
     */
    public int incrementImpressionCount(Long adId);

    /**
     * 获取详情页面广告（顶部+底部）
     * 
     * @param categoryId 分类ID
     * @return 详情页面广告数据
     */
    public java.util.Map<String, java.util.List<WebAdvertisementVO>> selectDetailPageAds(Long categoryId);

    /**
     * 根据分类ID获取短视频广告
     * 
     * @param categoryId 分类ID
     * @return 短视频广告列表
     */
    public List<WebAdvertisementVO> selectShortVideoAdsByCategory(Long categoryId);

    /**
     * 根据分类ID获取分页模式广告
     * 
     * @param categoryId 分类ID
     * @return 分页模式广告列表
     */
    public List<WebAdvertisementVO> selectPagedAdsByCategory(Long categoryId);
} 