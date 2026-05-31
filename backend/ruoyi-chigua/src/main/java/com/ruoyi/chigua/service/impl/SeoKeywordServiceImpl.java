package com.ruoyi.chigua.service.impl;

import com.ruoyi.chigua.domain.SeoKeyword;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.mapper.SeoKeywordMapper;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.service.ISeoKeywordService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * SEO关键词Service实现
 * 
 * @author ruoyi
 * @date 2026-01-14
 */
@Service
public class SeoKeywordServiceImpl implements ISeoKeywordService {

    private static final Logger logger = LoggerFactory.getLogger(SeoKeywordServiceImpl.class);

    @Autowired
    private SeoKeywordMapper seoKeywordMapper;

    @Autowired
    private VideoMapper videoMapper;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    @Override
    public AjaxResult getKeywordDetailWithVideos(Long id, Integer pageNum, Integer pageSize) {
        try {
            // 1. 获取关键词信息
            SeoKeyword keyword = seoKeywordMapper.selectById(id);
            if (keyword == null || keyword.getStatus() != 1) {
                logger.warn("关键词不存在或已禁用: id={}", id);
                return AjaxResult.error("关键词不存在或已禁用");
            }

            logger.info("🔍 SEO关键词查询: id={}, keyword={}", id, keyword.getKeyword());

            // 2. 智能搜索匹配的视频
            Map<String, Object> searchResult = smartSearchVideos(keyword.getKeyword(), pageNum, pageSize);
            
            List<Video> videos = (List<Video>) searchResult.get("videos");
            Integer total = (Integer) searchResult.get("total");
            String matchType = (String) searchResult.get("matchType");

            // 3. 返回数据（视频URL由VideoMapper查询时已自动处理）
            Map<String, Object> result = new HashMap<>();
            result.put("keywordInfo", keyword);
            result.put("list", videos);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("matchType", matchType);

            logger.info("✅ SEO关键词查询成功: keyword={}, matchType={}, count={}", 
                keyword.getKeyword(), matchType, videos != null ? videos.size() : 0);

            return AjaxResult.success(result);

        } catch (Exception e) {
            logger.error("❌ SEO关键词查询失败: id={}, error={}", id, e.getMessage(), e);
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public AjaxResult getByKeywordWithVideos(String keyword, Integer pageNum, Integer pageSize) {
        if (StringUtils.isEmpty(keyword)) {
            return AjaxResult.error("关键词不能为空");
        }

        SeoKeyword seoKeyword = seoKeywordMapper.selectByKeyword(keyword);
        if (seoKeyword == null) {
            logger.warn("关键词不存在: keyword={}", keyword);
            return AjaxResult.error("关键词不存在");
        }

        return getKeywordDetailWithVideos(seoKeyword.getId(), pageNum, pageSize);
    }

    /**
     * 智能搜索视频（三层匹配策略）
     * 
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    private Map<String, Object> smartSearchVideos(String keyword, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        List<Video> videos = null;
        int total = 0;
        String matchType = "exact";

        // 第1层：精确匹配（关键词完整出现在标题或描述中）
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("pageNum", (pageNum - 1) * pageSize);
            params.put("pageSize", pageSize);
            
            videos = videoMapper.searchByKeyword(params);
            if (videos != null && !videos.isEmpty()) {
                total = videoMapper.countByKeyword(keyword);
                matchType = "exact";
                logger.info("✅ 精确匹配成功: keyword={}, count={}", keyword, videos.size());
            }
        } catch (Exception e) {
            logger.error("精确匹配失败: {}", e.getMessage());
        }

        // 第2层：模糊匹配（拆分关键词，OR匹配）
        if (videos == null || videos.isEmpty()) {
            try {
                List<String> keywords = splitKeyword(keyword);
                if (!keywords.isEmpty()) {
                    logger.info("🔍 尝试模糊匹配: 原始={}, 拆分={}", keyword, keywords);
                    
                    Map<String, Object> params = new HashMap<>();
                    params.put("keywords", keywords);
                    params.put("pageNum", (pageNum - 1) * pageSize);
                    params.put("pageSize", pageSize);
                    
                    videos = videoMapper.searchByKeywords(params);
                    if (videos != null && !videos.isEmpty()) {
                        total = videoMapper.countByKeywords(keywords);
                        matchType = "fuzzy";
                        logger.info("✅ 模糊匹配成功: keywords={}, count={}", keywords, videos.size());
                    }
                }
            } catch (Exception e) {
                logger.error("模糊匹配失败: {}", e.getMessage());
            }
        }

        // 第3层：随机视频兜底
        if (videos == null || videos.isEmpty()) {
            try {
                logger.info("⚠️ 无匹配结果，返回随机视频兜底");
                videos = videoMapper.selectHotVideos(pageSize);
                total = videos != null ? videos.size() : 0;
                matchType = "recommended";
            } catch (Exception e) {
                logger.error("随机视频兜底失败: {}", e.getMessage());
                videos = new ArrayList<>();
                total = 0;
            }
        }

        // 为视频生成签名URL
        if (videos != null && !videos.isEmpty()) {
            for (Video video : videos) {
                try {
                    // 为封面图片生成签名URL
                    String coverPath = video.getCoverUrl();
                    if (coverPath == null || coverPath.trim().isEmpty()) {
                        coverPath = video.getCoverImage();
                    }
                    if (coverPath != null && !coverPath.trim().isEmpty()) {
                        String signedCoverUrl = chiguaUrlService.generateUrl(coverPath, 
                            ChiguaUrlService.ResourceType.COVER, false);
                        if (signedCoverUrl != null) {
                            video.setCoverUrl(signedCoverUrl);
                        }
                    }
                } catch (Exception e) {
                    logger.error("❌ 为视频{}生成签名URL失败: {}", video.getId(), e.getMessage());
                }
            }
        }
        
        result.put("videos", videos);
        result.put("total", total);
        result.put("matchType", matchType);
        return result;
    }

    /**
     * 拆分关键词（提取有意义的词）
     * 
     * @param keyword 原始关键词
     * @return 拆分后的关键词列表
     */
    private List<String> splitKeyword(String keyword) {
        List<String> result = new ArrayList<>();
        
        // 定义无意义词（停用词）
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很",
            "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "免费", "在线", "观看",
            "网站", "视频", "高清", "播放", "下载", "最新", "全集", "完整版", "无码", "高清", "国语", "中文"
        ));
        
        // 简单拆分：按字符长度提取子串
        if (keyword.length() >= 2) {
            // 提取2-4字的子串
            for (int len = 4; len >= 2; len--) {
                for (int i = 0; i <= keyword.length() - len; i++) {
                    String sub = keyword.substring(i, i + len);
                    if (!stopWords.contains(sub) && !result.contains(sub)) {
                        result.add(sub);
                        if (result.size() >= 5) { // 最多5个关键词
                            return result;
                        }
                    }
                }
            }
        }
        
        // 如果没有提取到任何词，返回原关键词
        if (result.isEmpty() && !stopWords.contains(keyword)) {
            result.add(keyword);
        }
        
        return result;
    }

    @Override
    public AjaxResult getHotKeywords(Integer limit) {
        try {
            List<SeoKeyword> keywords = seoKeywordMapper.selectHotKeywords(limit);
            return AjaxResult.success(keywords);
        } catch (Exception e) {
            logger.error("获取热门关键词失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取热门关键词失败");
        }
    }

    @Override
    public AjaxResult getRandomKeywords(Integer limit) {
        try {
            List<SeoKeyword> keywords = seoKeywordMapper.selectRandomKeywords(limit);
            return AjaxResult.success(keywords);
        } catch (Exception e) {
            logger.error("获取随机关键词失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取随机关键词失败");
        }
    }

    @Override
    public AjaxResult incrementSearchCount(Long id) {
        try {
            seoKeywordMapper.incrementSearchCount(id);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("更新搜索次数失败: id={}, error={}", id, e.getMessage());
            // 静默失败，不影响用户体验
            return AjaxResult.success();
        }
    }

    @Override
    public AjaxResult incrementClickCount(Long id) {
        try {
            seoKeywordMapper.incrementClickCount(id);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("更新点击次数失败: id={}, error={}", id, e.getMessage());
            // 静默失败
            return AjaxResult.success();
        }
    }

    @Override
    public List<SeoKeyword> getAllActiveKeywords() {
        return seoKeywordMapper.selectAllActive();
    }

    @Override
    public int countActiveKeywords() {
        return seoKeywordMapper.countActive();
    }

    @Override
    @Cacheable(value = "seoKeywordSitemap",
            key = "'shard:' + #baseUrl + ':' + #shardNumber + ':' + #batchSize",
            unless = "#result == null")
    public String generateKeywordSitemapShard(String baseUrl, int shardNumber, int batchSize) {
        if (shardNumber < 1 || batchSize < 1) {
            return emptyUrlset();
        }
        int offset = (shardNumber - 1) * batchSize;
        List<SeoKeyword> page = seoKeywordMapper.selectActiveForSitemap(offset, batchSize);
        if (page == null || page.isEmpty()) {
            return emptyUrlset();
        }
        String today = LocalDate.now().toString();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (SeoKeyword keyword : page) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/keyword/").append(keyword.getId()).append("</loc>\n");
            xml.append("    <lastmod>").append(today).append("</lastmod>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>");
        logger.info("✅ keyword sitemap 分片 #{} 生成成功（缓存未命中），{} URLs", shardNumber, page.size());
        return xml.toString();
    }

    private String emptyUrlset() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
            + "</urlset>";
    }
}

