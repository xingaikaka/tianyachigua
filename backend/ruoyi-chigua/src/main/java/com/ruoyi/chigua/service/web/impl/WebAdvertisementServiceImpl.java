package com.ruoyi.chigua.service.web.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.domain.Advertisement;
import com.ruoyi.chigua.domain.vo.web.WebAdvertisementVO;
import com.ruoyi.chigua.mapper.AdvertisementMapper;
import com.ruoyi.chigua.service.web.IWebAdvertisementService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.service.IAdStatisticsService;

/**
 * Web广告Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-21
 */
@Service
public class WebAdvertisementServiceImpl implements IWebAdvertisementService 
{
    private static final Logger logger = LoggerFactory.getLogger(WebAdvertisementServiceImpl.class);

    @Autowired
    private AdvertisementMapper advertisementMapper;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    @Autowired
    private IAdStatisticsService adStatisticsService;

    /**
     * 根据广告位置获取广告
     */
    @Override
    public List<WebAdvertisementVO> selectAdsByPosition(String position)
    {
        logger.info("📍 获取广告位置({})的广告", position);
        
        List<Advertisement> advertisements = advertisementMapper.selectAdsByPosition(position);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条位置({})广告", result.size(), position);
        return result;
    }

    /**
     * 根据广告类型获取广告
     */
    @Override
    public List<WebAdvertisementVO> selectAdsByType(String adType)
    {
        logger.info("🏷️ 获取广告类型({})的广告", adType);
        
        List<Advertisement> advertisements = advertisementMapper.selectAdsByType(adType);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条类型({})广告", result.size(), adType);
        return result;
    }

    /**
     * 获取分类列表顶部横幅广告
     */
    @Override
    public List<WebAdvertisementVO> selectCategoryAds(Long categoryId)
    {
        logger.info("📂 获取分类({})列表顶部横幅广告", categoryId);
        
        List<Advertisement> advertisements = advertisementMapper.selectWebCategoryAds(categoryId);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条分类顶部广告", result.size());
        return result;
    }

    /**
     * 获取分类列表底部横幅广告
     */
    @Override
    public List<WebAdvertisementVO> selectCategoryBottomAds(Long categoryId)
    {
        logger.info("📂⬇️ 获取分类({})列表底部横幅广告", categoryId);
        
        List<Advertisement> advertisements = advertisementMapper.selectWebCategoryBottomAds(categoryId);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条分类底部广告", result.size());
        return result;
    }

    /**
     * 增加广告点击统计
     */
    @Override
    public int incrementClickCount(Long adId)
    {
        logger.info("👆 记录广告({})点击统计到统计表", adId);
        
        try {
            // 获取广告信息
            Advertisement advertisement = advertisementMapper.selectAdvertisementById(adId);
            if (advertisement == null) {
                logger.warn("⚠️ 广告不存在: adId={}", adId);
                return 0;
            }
            
            // 记录到统计表（不再更新原广告表的click_count）
            return adStatisticsService.recordClick(advertisement);
        } catch (Exception e) {
            logger.error("❌ 记录广告点击统计失败: adId={}, error={}", adId, e.getMessage());
            return 0;
        }
    }

    /**
     * 增加广告展示统计
     */
    @Override
    public int incrementImpressionCount(Long adId)
    {
        logger.info("👁️ 增加广告({})展示统计", adId);
        // 更新主表 impression_count（无日期维度，用于快速汇总）
        advertisementMapper.updateImpressionCount(adId);
        // 同时写入 ad_statistics（有日期维度，用于趋势图）
        try {
            Advertisement advertisement = advertisementMapper.selectAdvertisementById(adId);
            if (advertisement != null) {
                adStatisticsService.recordImpression(advertisement);
            }
        } catch (Exception e) {
            logger.error("❌ 记录广告曝光到统计表失败: adId={}, error={}", adId, e.getMessage());
        }
        return 1;
    }

    /**
     * 根据应用类型获取Logo广告
     */
    @Override
    public List<WebAdvertisementVO> selectLogoAdsByAppType(String appType)
    {
        logger.info("📱 获取应用类型({})的Logo广告", appType);
        
        List<Advertisement> advertisements = advertisementMapper.selectLogoAdsByAppType(appType);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                // 为Logo广告添加图标名称
                vo.setIconName(ad.getIconName());
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条应用类型({})Logo广告", result.size(), appType);
        return result;
    }

    /**
     * 转换为Web广告VO
     */
    private WebAdvertisementVO convertToWebAdvertisementVO(Advertisement advertisement)
    {
        if (advertisement == null) {
            return null;
        }
        
        try {
            WebAdvertisementVO vo = new WebAdvertisementVO();
            vo.setId(advertisement.getId());
            vo.setTitle(advertisement.getTitle());
            vo.setDescription(advertisement.getDescription());
            vo.setLinkUrl(advertisement.getLinkUrl());
            vo.setSortOrder(advertisement.getSortOrder());
            vo.setClickCount(advertisement.getClickCount());
            vo.setImpressionCount(advertisement.getImpressionCount());
            vo.setCreateTime(advertisement.getCreatedAt());

            // 广告图片：提取 R2 资源 key 后重新用当前 CDN 配置签名
            // 兼容三种情况：
            //   1. 相对路径（如 images/xxx.jpg）→ 直接签名
            //   2. 旧版签名 URL（含 key= 参数）→ 提取 key 后重新签名（确保走当前 CDN）
            //   3. 真正的外部链接（无 key=）→ 原样返回
            String rawImageUrl = advertisement.getImageUrl();
            if (rawImageUrl != null && !rawImageUrl.isEmpty()) {
                if (!rawImageUrl.startsWith("http://") && !rawImageUrl.startsWith("https://")) {
                    // 情况1：相对路径
                    rawImageUrl = chiguaUrlService.generateUrl(rawImageUrl, ChiguaUrlService.ResourceType.IMAGE);
                } else if (rawImageUrl.contains("key=")) {
                    // 情况2：旧版签名URL，提取 key 参数后重新签名
                    try {
                        String resourceKey = null;
                        String query = new java.net.URL(rawImageUrl).getQuery();
                        if (query != null) {
                            for (String param : query.split("&")) {
                                if (param.startsWith("key=")) {
                                    resourceKey = java.net.URLDecoder.decode(param.substring(4), "UTF-8");
                                    break;
                                }
                            }
                        }
                        if (resourceKey != null && !resourceKey.isEmpty()) {
                            rawImageUrl = chiguaUrlService.generateUrl(resourceKey, ChiguaUrlService.ResourceType.IMAGE);
                        }
                    } catch (Exception ex) {
                        logger.warn("广告图片URL提取key失败，保持原值: {}", ex.getMessage());
                    }
                }
                // 情况3：真正的外部链接，原样返回
            }
            vo.setImageUrl(rawImageUrl);

            return vo;
        } catch (Exception e) {
            logger.error("❌ 转换广告VO失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取详情页面广告（顶部+底部）
     */
    @Override
    public Map<String, List<WebAdvertisementVO>> selectDetailPageAds(Long categoryId)
    {
        logger.info("📑 获取详情页面广告: categoryId={}", categoryId);
        
        // 查询详情页面的顶部(position=3)和底部(position=4)广告
        List<String> positions = Arrays.asList("3", "4");
        List<Advertisement> advertisements = advertisementMapper.selectAdsByPositionsAndCategory(positions, categoryId);
        
        // 按位置分组
        Map<String, List<WebAdvertisementVO>> result = new HashMap<>();
        List<WebAdvertisementVO> topAds = new ArrayList<>();
        List<WebAdvertisementVO> bottomAds = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                if ("3".equals(ad.getPosition())) {
                    topAds.add(vo);
                } else if ("4".equals(ad.getPosition())) {
                    bottomAds.add(vo);
                }
            }
        }
        
        result.put("topAds", topAds);
        result.put("bottomAds", bottomAds);
        
        logger.info("✅ 获取详情页面广告成功: 顶部{}条, 底部{}条", topAds.size(), bottomAds.size());
        return result;
    }

    /**
     * 根据分类ID获取短视频广告
     */
    @Override
    public List<WebAdvertisementVO> selectShortVideoAdsByCategory(Long categoryId)
    {
        logger.info("🎬 获取分类({})的短视频广告", categoryId);
        
        List<Advertisement> advertisements = advertisementMapper.selectShortVideoAdsByCategory(categoryId);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条分类({})短视频广告", result.size(), categoryId);
        return result;
    }

    /**
     * 根据分类ID获取分页模式广告
     */
    @Override
    public List<WebAdvertisementVO> selectPagedAdsByCategory(Long categoryId)
    {
        logger.info("📄 获取分类({})的分页模式广告", categoryId);
        
        List<Advertisement> advertisements = advertisementMapper.selectPagedAdsByCategory(categoryId);
        List<WebAdvertisementVO> result = new ArrayList<>();
        
        for (Advertisement ad : advertisements) {
            WebAdvertisementVO vo = convertToWebAdvertisementVO(ad);
            if (vo != null) {
                result.add(vo);
            }
        }
        
        logger.info("✅ 获取到 {} 条分类({})分页模式广告", result.size(), categoryId);
        return result;
    }
} 