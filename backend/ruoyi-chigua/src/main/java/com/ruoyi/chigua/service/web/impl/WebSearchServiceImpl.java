package com.ruoyi.chigua.service.web.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.vo.web.WebVideoVO;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.web.IWebSearchService;
import com.ruoyi.common.core.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * Web搜索Service业务层处理
 * 
 * @author chigua
 * @date 2025-01-22
 */
@Service
public class WebSearchServiceImpl implements IWebSearchService 
{
    private static final Logger logger = LoggerFactory.getLogger(WebSearchServiceImpl.class);
    
    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private IVideoService videoService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private com.ruoyi.chigua.service.WebRichTextProcessorService webRichTextProcessorService;

    /**
     * 自我注入：用于在同类方法间调用带 @Cacheable 的方法（解决Spring AOP代理问题）
     */
    @Autowired
    private IWebSearchService self;

    /**
     * 搜索基础列表缓存（不含签名URL，仅DB实体）
     */
    @Override
    @Cacheable(value = "searchResult",
               key = "'search_basic_v2_' + (#keyword == null ? '' : #keyword.trim().toLowerCase()) + '_' + (#pageNum == null || #pageNum < 1 ? 1 : #pageNum) + '_' + (#pageSize == null || #pageSize < 1 ? 20 : #pageSize)",
               unless = "#result == null")
    public List<Video> getCachedSearchVideoBaseList(String keyword, Integer pageNum, Integer pageSize)
    {
        String normalizedKeyword = normalizeKeyword(keyword);
        Video video = new Video();
        video.setStatus(1);
        video.setTitle(normalizedKeyword);
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (safePageNum - 1) * safePageSize;

        try {
            List<Video> videos = videoMapper.selectVideoListForSearchPaged(video, offset, safePageSize);
            if ((videos == null || videos.isEmpty()) && offset == 0) {
                long total = videoMapper.countVideoListForSearch(video);
                if (total > 0) {
                    logger.warn("⚠️ 显式分页返回空结果但总数为{}，启用回退：非分页查询+内存切片", total);
                    List<Video> all = videoMapper.selectVideoListForSearch(video);
                    if (all != null && !all.isEmpty()) {
                        int end = Math.min(safePageSize, all.size());
                        videos = new ArrayList<>(all.subList(0, end));
                    }
                }
            }
            return videos == null ? new ArrayList<>() : new ArrayList<>(videos);
        } catch (Exception e) {
            logger.error("❌ 显式分页搜索失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索总数缓存（独立 key，不依赖分页参数）
     */
    @Override
    @Cacheable(value = "searchResult",
               key = "'search_total_v2_' + (#keyword == null ? '' : #keyword.trim().toLowerCase())",
               unless = "#result == null")
    public long getCachedSearchVideoTotal(String keyword)
    {
        String normalizedKeyword = normalizeKeyword(keyword);
        Video video = new Video();
        video.setStatus(1);
        video.setTitle(normalizedKeyword);
        try {
            return videoMapper.countVideoListForSearch(video);
        } catch (Exception e) {
            logger.error("❌ 搜索计数失败: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 根据关键词搜索视频
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 TableDataInfo，避免封面 URL 跨 region 污染。
     * 改为：基础数据走 getCachedSearchVideoBaseList / getCachedSearchVideoTotal 缓存，
     * 封面签名URL在 convertToWebVideoVO 中通过 getCachedCoverSignedUrl 实时按 region 缓存。
     */
    @Override
    public TableDataInfo searchVideosByKeyword(String keyword, Integer pageNum, Integer pageSize)
    {
        String normalizedKeyword = normalizeKeyword(keyword);
        logger.info("🔍 开始搜索视频: keyword={}, pageNum={}, pageSize={}", normalizedKeyword, pageNum, pageSize);

        long total = self.getCachedSearchVideoTotal(keyword);
        List<Video> videos = total > 0
                ? self.getCachedSearchVideoBaseList(keyword, pageNum, pageSize)
                : java.util.Collections.emptyList();
        if (videos == null) {
            videos = java.util.Collections.emptyList();
        }
        logger.info("🎯 数据库查询完成: 找到{}条记录", videos.size());

        // 出口实时签 URL（封面通过 getCachedCoverSignedUrl 走 region 区分缓存）
        List<WebVideoVO> webVideos = videos.stream()
                .map(this::convertToWebVideoVO)
                .collect(Collectors.toList());
        
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("搜索成功");
        rspData.setRows(webVideos);
        rspData.setTotal(total);
        
        logger.info("✅ 搜索完成: 关键词={}, 总记录数={}, 当前页记录数={}", 
            keyword, total, webVideos.size());
        
        return rspData;
    }

    /**
     * 转换为WebVideoVO
     * 
     * @param video 视频对象
     * @return Web视频VO
     */
    private WebVideoVO convertToWebVideoVO(Video video)
    {
        WebVideoVO vo = new WebVideoVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setAuthor(video.getAuthor());
        
        // 搜索列表不返回富文本，避免大字段与CPU消耗
        vo.setVideoContent(null);
        
        vo.setDuration(video.getDuration());
        vo.setViewCount(video.getViewCount());
        vo.setLikeCount(video.getLikeCount());
        vo.setCategoryId(video.getCategoryId());
        vo.setStatus(video.getStatus());
        vo.setPublishedAt(video.getPublishedAt());
        vo.setCreateTime(video.getCreatedAt());
        vo.setUpdateTime(video.getUpdatedAt());
        
        // 封面：保留现有签名逻辑；如需进一步降耗，可直接返回存储路径
        if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
            try {
                String coverPath = video.getCoverUrl() != null ? video.getCoverUrl() : video.getCoverImage();
                String signedCoverUrl = videoService.getCachedCoverSignedUrl(video.getId(), coverPath);
                vo.setCoverImageUrl(signedCoverUrl != null ? signedCoverUrl.replace("&decrypt=true", "") : coverPath);
            } catch (Exception e) {
                vo.setCoverImageUrl(video.getCoverImage());
            }
        }
        
        // 主分类名称：直接使用SQL已带回的字段，避免N+1
        vo.setCategoryName(video.getCategoryName());
        
        // 搜索列表不再做标签N+1，保持轻量
        
        return vo;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed;
    }
}
