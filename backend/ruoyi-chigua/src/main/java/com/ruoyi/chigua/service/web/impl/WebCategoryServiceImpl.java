package com.ruoyi.chigua.service.web.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.vo.web.WebCategoryVO;
import com.ruoyi.chigua.domain.vo.web.WebCategoryListResponse;
import com.ruoyi.chigua.domain.vo.web.WebVideoVO;
import com.ruoyi.chigua.domain.vo.web.WebShortVideoVO;
import com.ruoyi.chigua.mapper.CategoryMapper;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.service.web.IWebCategoryService;
import com.ruoyi.chigua.service.ICategoryService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.constant.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.DigestUtils;

/**
 * Web分类Service业务层处理
 * 
 * @author chigua
 * @date 2024-12-20
 */
@Service
public class WebCategoryServiceImpl implements IWebCategoryService 
{
    private static final Logger logger = LoggerFactory.getLogger(WebCategoryServiceImpl.class);
    
    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    // ✅ 自我注入：用于调用带缓存的方法（解决Spring AOP代理问题）
    @Autowired
    private IWebCategoryService self;

    @Autowired
    private com.ruoyi.chigua.service.WebRichTextProcessorService webRichTextProcessorService;
    
    @Autowired
    private com.ruoyi.chigua.service.IVideoService videoService;

    @Autowired
    private com.ruoyi.chigua.service.VideoUrlGeneratorService videoUrlGeneratorService;

    /**
     * 查询Web分类列表
     * 
     * @return Web分类列表
     */
    @Override
    public WebCategoryListResponse selectWebCategoryList()
    {
        // 使用专门的前台缓存方法
        List<Category> categories = categoryService.selectCategoryListForFrontend();

        // 转换为WebCategoryVO
        List<WebCategoryVO> categoryVOs = categories.stream()
                .map(this::convertToWebVO)
                .collect(Collectors.toList());

        String version = computeCategoryListVersion(categories);

        return new WebCategoryListResponse(categoryVOs, version);
    }

    @Override
    public String getCategoryListVersion() {
        List<Category> categories = categoryService.selectCategoryListForFrontend();
        return computeCategoryListVersion(categories);
    }

    /**
     * 根据分类ID查询Web分类
     * 
     * @param categoryId 分类ID
     * @return Web分类
     */
    @Override
    public WebCategoryVO selectWebCategoryById(Long categoryId)
    {
        Category category = categoryService.selectCategoryById(categoryId);
        if (category != null && Integer.valueOf(1).equals(category.getStatus())) {
            return convertToWebVO(category);
        }
        return null;
    }

    /**
     * 转换为WebCategoryVO
     * 
     * @param category 分类对象
     * @return Web分类VO
     */
    private WebCategoryVO convertToWebVO(Category category)
    {
        WebCategoryVO vo = new WebCategoryVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getName());
        vo.setIcon(""); // Category实体中没有icon字段，设为空字符串
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        vo.setCreateTime(category.getCreatedAt());
        // 透传是否推荐
        vo.setIsRecommended(category.getIsRecommended());
        
        // TODO: 可以在这里添加统计视频数量的逻辑
        vo.setVideoCount(0);
        
        return vo;
    }

    private String computeCategoryListVersion(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return "empty";
        }

        String payload = categories.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(category -> category.getId() == null ? 0L : category.getId()))
                .map(category -> {
                    long id = category.getId() != null ? category.getId() : 0L;
                    long updatedAt = category.getUpdatedAt() != null ? category.getUpdatedAt().getTime() : 0L;
                    int sortOrder = category.getSortOrder() != null ? category.getSortOrder() : 0;
                    int status = category.getStatus() != null ? category.getStatus() : 0;
                    return id + "-" + updatedAt + "-" + sortOrder + "-" + status;
                })
                .collect(Collectors.joining("|"));

        return DigestUtils.md5DigestAsHex(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据分类获取视频列表
     * 
     * @param categoryId 分类ID，为null时查询所有分类
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    @Override
    public TableDataInfo selectWebVideoList(Long categoryId, Integer pageNum, Integer pageSize)
    {
        // 重定向到优化版实现，统一使用新版缓存系统
        return selectWebVideoListOptimized(categoryId, null, null, null, null, null, pageNum, pageSize);
    }

    /**
     * 根据分类获取视频列表（优化版，仅必需字段）
     * 🔧 分页模式：按照短视频方式处理，返回包含 firstVideoUrl 的数据
     * 
     * @param categoryId 分类ID，为null时查询所有分类
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 优化版视频列表（包含 firstVideoUrl）
     */
    @Override
    public TableDataInfo selectWebVideoListOptimized(Long categoryId, String title, Long tagId, List<Long> tagIds, String author, List<String> authors, Integer pageNum, Integer pageSize)
    {
        logger.debug("视频列表查询: categoryId={}, pageNum={}, pageSize={}", categoryId, pageNum, pageSize);
        
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (safePageNum - 1) * safePageSize;
        
        // 通过代理对象调用带缓存的方法
        List<Video> baseList = self.getCachedBaseVideos(categoryId, title, tagId, tagIds, author, authors, offset, safePageSize);
        long total = self.getCachedVideoTotal(categoryId, title, tagId, tagIds, author, authors);
        
        logger.debug("数据获取完成: total={}, 当前页{}条", total, baseList.size());
        
        if (baseList.isEmpty()) {
            logger.debug("视频查询结果为空, total={}", total);
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            rspData.setRows(new ArrayList<>());
            rspData.setTotal(total);
            return rspData;
        }
        
        // 批量获取所有视频的标签
        List<Long> videoIds = baseList.stream().map(Video::getId).collect(Collectors.toList());
        Map<Long, List<Tag>> videoTagsMap = videoService.selectTagsByVideoIdsBatch(videoIds);
        logger.debug("批量查询标签完成: {}个视频", videoIds.size());
        
        // 构造VO，按照短视频方式处理（包含 firstVideoUrl）
        List<WebShortVideoVO> result = new ArrayList<>();
        
        for (Video v : baseList) {
            String firstUrl = getFirstVideoUrl(v);
            if (firstUrl == null) {
                continue;
            }
            
            try {
                String signedUrl = generateSignedVideoUrl(firstUrl);
                if (signedUrl != null) {
                    WebShortVideoVO vo = createShortVideoVO(v, signedUrl);
                    
                    // 从批量查询结果中获取标签
                    List<Tag> tags = videoTagsMap.get(v.getId());
                    if (tags != null && !tags.isEmpty()) {
                        List<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
                        List<Long> videoTagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
                        // 构建标签对象列表（包含isHot字段）
                        List<WebShortVideoVO.TagInfo> tagInfoList = tags.stream()
                            .map(tag -> new WebShortVideoVO.TagInfo(tag.getId(), tag.getName(), tag.getIsHot()))
                            .collect(Collectors.toList());
                        vo.setTags(tagNames);
                        vo.setTagIds(videoTagIds);
                        vo.setTagList(tagInfoList);
                    } else {
                        // 确保即使没有标签也返回空数组，而不是null
                        vo.setTags(new ArrayList<>());
                        vo.setTagIds(new ArrayList<>());
                        vo.setTagList(new ArrayList<>());
                    }
                    
                    result.add(vo);
                }
            } catch (Exception e) {
                logger.warn("❌ 处理视频失败: videoId={}, err={}", v.getId(), e.getMessage());
            }
        }
        
        int skippedCount = baseList.size() - result.size();
        if (skippedCount > 0) {
            logger.warn("⚠️ [分页] 本页 {} 条视频因无法获取URL被丢弃, categoryId={}, offset={}, total={}", 
                        skippedCount, categoryId, offset, total);
        }
        logger.debug("视频查询完成: 返回{}条记录，总数{}", result.size(), total);
        
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(result);
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 生成视频列表查询的缓存 key
     * 
     * @param categoryId 分类ID
     * @param title 标题
     * @param tagId 标签ID
     * @param tagIds 标签ID列表
     * @param author 作者
     * @param authors 作者列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 缓存key
     */
    private String buildVideoListCacheKey(Long categoryId, String title, Long tagId, List<Long> tagIds, 
                                          String author, List<String> authors, Integer pageNum, Integer pageSize) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("cat_").append(categoryId != null ? categoryId : "all");
        
        if (title != null && !title.isEmpty()) {
            keyBuilder.append("_t_").append(title.hashCode());
        }
        if (tagId != null) {
            keyBuilder.append("_tag_").append(tagId);
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            keyBuilder.append("_tags_").append(tagIds.hashCode());
        }
        if (author != null && !author.isEmpty()) {
            keyBuilder.append("_a_").append(author.hashCode());
        }
        if (authors != null && !authors.isEmpty()) {
            keyBuilder.append("_as_").append(authors.hashCode());
        }
        if (pageNum != null) {
            keyBuilder.append("_p").append(pageNum);
        }
        if (pageSize != null) {
            keyBuilder.append("_s").append(pageSize);
        }
        
        return keyBuilder.toString();
    }

    /**
     * 获取缓存的视频基础数据（不含签名URL）
     * ✅ 简化版：分页级别缓存
     * 
     * @param categoryId 分类ID
     * @param title 标题
     * @param tagId 标签ID
     * @param tagIds 标签ID列表
     * @param author 作者
     * @param authors 作者列表
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 视频列表
     */
    @Cacheable(value = "videoListOptimized", 
               key = "'videos_' + (#categoryId != null ? #categoryId : 'all') + " +
                     "'_t' + (#title != null ? #title : '') + " +
                     "'_tag' + (#tagId != null ? #tagId : '') + " +
                     "'_tags' + (#tagIds != null ? #tagIds.toString() : '') + " +
                     "'_a' + (#author != null ? #author : '') + " +
                     "'_as' + (#authors != null ? #authors.toString() : '') + " +
                     "'_p' + (#offset / #pageSize + 1) + '_s' + #pageSize",
               unless = "#result == null || #result.isEmpty()")
    public List<Video> getCachedBaseVideos(Long categoryId, String title, Long tagId, List<Long> tagIds, 
                                           String author, List<String> authors, int offset, int pageSize) {
        logger.debug("[缓存MISS] 查询视频: categoryId={}, offset={}, pageSize={}", categoryId, offset, pageSize);
        
        // 检查是否有过滤条件
        boolean hasFilter = (title != null && !title.isEmpty()) || 
                           tagId != null || 
                           (tagIds != null && !tagIds.isEmpty()) || 
                           (author != null && !author.isEmpty()) || 
                           (authors != null && !authors.isEmpty());
        
        if (hasFilter) {
            return videoMapper.selectShortVideoPage(categoryId, title, tagId, tagIds, author, authors, offset, pageSize);
        }
        return videoMapper.selectCategoryVideoPage(categoryId, offset, pageSize);
    }

    /**
     * 获取缓存的视频总数
     * ✅ 简化版：总数缓存
     * 
     * @param categoryId 分类ID
     * @param title 标题
     * @param tagId 标签ID
     * @param tagIds 标签ID列表
     * @param author 作者
     * @param authors 作者列表
     * @return 总数
     */
    @Cacheable(value = "videoListOptimized", 
               key = "'total_' + (#categoryId != null ? #categoryId : 'all') + " +
                     "'_t' + (#title != null ? #title : '') + " +
                     "'_tag' + (#tagId != null ? #tagId : '') + " +
                     "'_tags' + (#tagIds != null ? #tagIds.toString() : '') + " +
                     "'_a' + (#author != null ? #author : '') + " +
                     "'_as' + (#authors != null ? #authors.toString() : '')",
               unless = "#result == null")
    public long getCachedVideoTotal(Long categoryId, String title, Long tagId, List<Long> tagIds, 
                                    String author, List<String> authors) {
        logger.debug("[缓存MISS] 统计视频总数: categoryId={}", categoryId);
        
        // 检查是否有过滤条件
        boolean hasFilter = (title != null && !title.isEmpty()) || 
                           tagId != null || 
                           (tagIds != null && !tagIds.isEmpty()) || 
                           (author != null && !author.isEmpty()) || 
                           (authors != null && !authors.isEmpty());
        
        if (hasFilter) {
            return videoMapper.countShortVideoPage(categoryId, title, tagId, tagIds, author, authors);
        }
        return videoMapper.countCategoryVideoPage(categoryId);
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
        
        // 🔐 获取Web前端处理后的富文本内容（实时生成签名URL，避免过期）
        if (video.getVideoContent() != null && !video.getVideoContent().trim().isEmpty()) {
            try {
                // 🚀 新策略：实时处理富文本，不缓存包含签名URL的内容
                String processedContent = processRichTextWithFreshUrls(video.getVideoContent());
                vo.setVideoContent(processedContent);
            } catch (Exception e) {
                logger.warn("❌ 处理视频{}Web富文本内容失败: {}", video.getId(), e.getMessage());
                vo.setVideoContent(video.getVideoContent()); // 降级到原始内容
            }
        }
        
        // 🔐 为Web端设置封面图片URL（如果缓存中已有签名URL则直接使用）
        if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
            String coverImage = video.getCoverImage();
            //logger.info("🖼️ 处理视频{}封面: coverImage={}", video.getId(), coverImage);
            
            // 检查是否已经是签名URL（包含signature参数）
            if (coverImage.contains("signature=")) {
                // 已经是签名URL，去除&decrypt=true参数，Web端会自行处理解密
                String webCoverUrl = coverImage.replace("&decrypt=true", "");
                vo.setCoverImageUrl(webCoverUrl);
                //logger.info("✅ 视频{}使用已有签名URL: {}", video.getId(), webCoverUrl);
            } else {
                // 原始路径 → 走带 region 区分的二级缓存（signedUrlCache::cover_*_CN/_OS）
                try {
                    String coverImagePath = video.getCoverImage();
                    if (coverImagePath != null && !coverImagePath.trim().isEmpty()) {
                        String signedCoverUrl = videoService.getCachedCoverSignedUrl(video.getId(), coverImagePath);
                        if (signedCoverUrl != null) {
                            vo.setCoverImageUrl(signedCoverUrl);
                        } else {
                            logger.warn("⚠️ 视频{}生成签名URL返回null，使用原始路径", video.getId());
                            vo.setCoverImageUrl(coverImagePath);
                        }
                    } else {
                        logger.warn("⚠️ 视频{}getCoverImage()返回null或空，检查coverImage字段", video.getId());
                        vo.setCoverImageUrl(null);
                    }
                } catch (Exception e) {
                    logger.warn("❌ 为视频{}生成Web封面URL失败: {}", video.getId(), e.getMessage());
                    vo.setCoverImageUrl(video.getCoverImage());
                }
            }
        }
        
        vo.setDuration(video.getDuration());
        vo.setViewCount(video.getViewCount());
        vo.setLikeCount(video.getLikeCount());
        vo.setCategoryId(video.getCategoryId());
        vo.setStatus(video.getStatus());
        vo.setPublishedAt(video.getPublishedAt());
        vo.setCreateTime(video.getCreatedAt());
        vo.setUpdateTime(video.getUpdatedAt());
        
        // 获取主分类名称
        if (video.getCategoryId() != null) {
            Category category = categoryService.selectCategoryById(video.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        
        // 获取所有关联的分类（使用缓存）
        try {
            List<Category> categories = videoService.selectCategoriesByVideoId(video.getId());
            if (categories != null && !categories.isEmpty()) {
                List<WebVideoVO.WebCategoryInfo> webCategories = new ArrayList<>();
                for (Category category : categories) {
                    webCategories.add(new WebVideoVO.WebCategoryInfo(category.getId(), category.getName()));
                }
                vo.setCategories(webCategories);
            }
        } catch (Exception e) {
            logger.warn("❌ 获取视频{}分类列表失败: {}", video.getId(), e.getMessage());
        }
        
        // 获取所有关联的标签（使用缓存）
        try {
            List<Tag> tags = videoService.selectTagsByVideoId(video.getId());
            if (tags != null && !tags.isEmpty()) {
                List<WebVideoVO.WebTagInfo> webTags = new ArrayList<>();
                for (Tag tag : tags) {
                    webTags.add(new WebVideoVO.WebTagInfo(tag.getId(), tag.getName(), tag.getColor()));
                }
                vo.setTags(webTags);
            }
        } catch (Exception e) {
            logger.warn("❌ 获取视频{}标签列表失败: {}", video.getId(), e.getMessage());
        }
        
        return vo;
    }

    /**
     * 查询视频详情（带缓存）
     * 
     * @param videoId 视频ID
     * @return 视频详情
     */
    /**
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 WebVideoVO。
     * 原因：WebVideoVO 里包含已签名的 CDN URL，整体缓存会导致 TTL 内
     * 不同 region 的访客拿到同一份 URL，破坏 IP→CDN 的实时路由。
     *
     * 缓存策略改为：
     * - 底层基础数据各自缓存（videoRelations 分类/标签、adjacentVideos 相邻视频、
     *   signedUrlCache 封面签名按 _CN/_OS 区分）
     * - 本方法每次执行，所有签名 URL 在出口处实时按访客 IP 重新签
     */
    @Override
    public WebVideoVO selectWebVideoDetail(Long videoId)
    {

        // 查询视频基本信息（使用缓存）
        Video video = videoService.selectVideoById(videoId);
        if (video == null || video.getStatus() != 1) {
            logger.warn("❌ 视频不存在或未发布: videoId={}", videoId);
            return null;
        }
        
        // 转换为WebVideoVO
        WebVideoVO webVideo = convertToWebVideoVO(video);
        
        // 🔄 使用优化后的相邻视频查询
        try {
            logger.info("🔍 获取相邻视频信息 (优化版本): videoId={}", videoId);
            
            WebVideoVO.AdjacentVideoInfo previousVideo = selectPreviousVideo(videoId, null, null, null);
            WebVideoVO.AdjacentVideoInfo nextVideo = selectNextVideo(videoId, null, null, null);
            
            webVideo.setPreviousVideo(previousVideo);
            webVideo.setNextVideo(nextVideo);
            
            logger.info("✅ 相邻视频信息已添加 (优化版本): previous={}, next={}", 
                previousVideo != null ? previousVideo.getId() : "null",
                nextVideo != null ? nextVideo.getId() : "null");
        } catch (Exception e) {
            logger.warn("⚠️ 获取相邻视频信息失败，但不影响主要功能: {}", e.getMessage());
            // 不影响主要功能，继续返回视频详情
            webVideo.setPreviousVideo(null);
            webVideo.setNextVideo(null);
        }
        
        logger.info("✅ 获取视频详情成功: videoId={}, title={}", videoId, video.getTitle());
        return webVideo;
    }

    /**
     * 查询热门推荐视频列表 - 使用混合缓存策略
     * 参考详情页面和列表页面的成功模式，分离基础数据缓存和URL实时生成
     */
    @Override
    public TableDataInfo selectWebHotRecommendedVideoList(Integer pageNum, Integer pageSize)
    {
        logger.debug("获取热门推荐视频列表: pageNum={}, pageSize={}", pageNum, pageSize);
        
        // 🚀 第一步：获取缓存的基础数据（不包含签名URL）
        List<Video> cachedVideos = getCachedHotRecommendedVideosBasic(pageNum, pageSize);
        
        // 🔗 第二步：为每个视频实时生成签名URL（参考列表页面模式）
        for (Video video : cachedVideos) {
            if (video.getCoverImage() != null && !video.getCoverImage().trim().isEmpty()) {
                try {
                    // 检查是否已经是签名URL（参考详情页面的智能检测）
                    if (!video.getCoverImage().contains("signature=")) {
                        // 实时生成签名URL（前端专用，不带decrypt=true）
                        String signedCoverUrl = videoUrlGeneratorService.generateCoverUrlForWeb(video);
                        if (signedCoverUrl != null) {
                            video.setCoverImage(signedCoverUrl);
                            //logger.debug("🔗 热门推荐视频{}生成签名URL: {}", video.getId(), signedCoverUrl);
                        }
                    } else {
                        //logger.debug("✅ 热门推荐视频{}已有签名URL，直接使用", video.getId());
                    }
                } catch (Exception e) {
                    logger.warn("❌ 为热门推荐视频{}生成签名URL失败: {}", video.getId(), e.getMessage());
                }
            }
        }
        
        // 🔄 第三步：转换为WebVideoVO（复用现有逻辑）
        List<WebVideoVO> webVideoList = cachedVideos.stream()
            .map(this::convertToWebVideoVO)
            .collect(Collectors.toList());
        
        // 创建分页结果
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(webVideoList);
        rspData.setTotal((long) webVideoList.size());
        
        logger.debug("热门推荐视频列表返回{}条记录", webVideoList.size());
        return rspData;
    }

    /**
     * 短视频基础列表缓存（不含签名URL，仅DB实体）
     * key 与旧 shortVideoList 完全一致，仅前缀改为 basic_，TTL 由 shortVideoList 配置继承
     */
    @Cacheable(value = "shortVideoList",
               key = "'basic_' + (#categoryId != null ? #categoryId : 'all')" +
                     " + '_t' + (#title != null ? #title : '')" +
                     " + '_tag' + (#tagId != null ? #tagId : 'all')" +
                     " + '_tags' + (#tagIds != null ? #tagIds.toString() : '')" +
                     " + '_a' + (#author != null ? #author : '')" +
                     " + '_as' + (#authors != null ? #authors.toString() : '')" +
                     " + '_p' + #pageNum + '_s' + #pageSize",
               unless = "#result == null || #result.isEmpty()")
    public List<Video> getCachedShortVideoBaseList(Long categoryId, String title, Long tagId, List<Long> tagIds,
                                                    String author, List<String> authors, Integer pageNum, Integer pageSize)
    {
        int offset = (pageNum - 1) * pageSize;
        return videoMapper.selectShortVideoPage(categoryId, title, tagId, tagIds, author, authors, offset, pageSize);
    }

    /**
     * 短视频总数缓存（独立 key，不依赖 pageNum/pageSize）
     */
    @Cacheable(value = "shortVideoList",
               key = "'total_' + (#categoryId != null ? #categoryId : 'all')" +
                     " + '_t' + (#title != null ? #title : '')" +
                     " + '_tag' + (#tagId != null ? #tagId : 'all')" +
                     " + '_tags' + (#tagIds != null ? #tagIds.toString() : '')" +
                     " + '_a' + (#author != null ? #author : '')" +
                     " + '_as' + (#authors != null ? #authors.toString() : '')",
               unless = "#result == null")
    public long getCachedShortVideoTotal(Long categoryId, String title, Long tagId, List<Long> tagIds,
                                          String author, List<String> authors)
    {
        return videoMapper.countShortVideoPage(categoryId, title, tagId, tagIds, author, authors);
    }

    /**
     * 短视频模式：按分类或标签查询短视频列表（优化版 - 解决N+1问题并添加缓存）
     *
     * 注意：本方法不再使用 @Cacheable 整体缓存返回的 TableDataInfo，避免 URL 被
     * 缓存死后跨 region 污染。改为：基础数据走 getCachedShortVideoBaseList /
     * getCachedShortVideoTotal 缓存，签名URL在出口处实时按访客 IP 重新生成。
     */
    @Override
    public TableDataInfo selectWebShortVideoList(Long categoryId, String title, Long tagId, List<Long> tagIds, String author, List<String> authors, Integer pageNum, Integer pageSize)
    {
        logger.debug("短视频查询: categoryId={}, pageNum={}, pageSize={}", categoryId, pageNum, pageSize);
        
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        // 1) 取基础数据（包含 first_video_url，但不含签名URL，可缓存）
        List<Video> baseList = self.getCachedShortVideoBaseList(categoryId, title, tagId, tagIds, author, authors, safePageNum, safePageSize);
        if (baseList == null) {
            baseList = new ArrayList<>();
        }

        // 2) 🔢 总数查询（独立缓存，不与签名URL耦合）
        long total = self.getCachedShortVideoTotal(categoryId, title, tagId, tagIds, author, authors);
        logger.debug("短视频计数: total={}, 当前页{}条", total, baseList.size());
        
        if (baseList.isEmpty()) {
            logger.info("📭 短视频查询结果为空，但 total={} (可能是分页超出范围或数据不符合条件)", total);
            // 返回空结果，但包含正确的 total 值
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            rspData.setRows(new ArrayList<>());
            rspData.setTotal(total);
            return rspData;
        }

        // 3) 批量获取所有视频的标签 - 解决N+1问题的关键优化
        List<Long> videoIds = baseList.stream().map(Video::getId).collect(Collectors.toList());
        Map<Long, List<Tag>> videoTagsMap = videoService.selectTagsByVideoIdsBatch(videoIds);
        
        logger.debug("🔍 批量查询标签完成: {}个视频，获取到{}个视频的标签数据", 
                   videoIds.size(), videoTagsMap.size());
        // 详细日志：检查每个视频的标签情况（仅在 DEBUG 级别输出，避免生产环境日志过多）
        if (logger.isDebugEnabled()) {
            for (Long videoId : videoIds) {
                List<Tag> tags = videoTagsMap.get(videoId);
                if (tags != null && !tags.isEmpty()) {
                    logger.debug("  视频ID {} 有 {} 个标签: {}", videoId, tags.size(), 
                               tags.stream().map(Tag::getName).collect(Collectors.joining(", ")));
                } else {
                    logger.debug("  视频ID {} 没有标签", videoId);
                }
            }
        }

        // 4) 构造VO，优先使用 first_video_url；如缺失则回退富文本解析（灰度期兜底）
        List<WebShortVideoVO> result = new ArrayList<>();

        for (Video v : baseList) {
            String firstUrl = getFirstVideoUrl(v);
            if (firstUrl == null) {
                continue;
            }
            
            try {
                String signedUrl = generateSignedVideoUrl(firstUrl);
                if (signedUrl != null) {
                    WebShortVideoVO vo = createShortVideoVO(v, signedUrl);
                    
                    // 从批量查询结果中获取标签 - 无额外数据库查询
                    List<Tag> tags = videoTagsMap.get(v.getId());
                    if (tags != null && !tags.isEmpty()) {
                        List<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
                        List<Long> videoTagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
                        // 构建标签对象列表（包含isHot字段）
                        List<WebShortVideoVO.TagInfo> tagInfoList = tags.stream()
                            .map(tag -> new WebShortVideoVO.TagInfo(tag.getId(), tag.getName(), tag.getIsHot()))
                            .collect(Collectors.toList());
                        vo.setTags(tagNames);
                        vo.setTagIds(videoTagIds);
                        vo.setTagList(tagInfoList);
                    } else {
                        // 确保即使没有标签也返回空数组，而不是null
                        vo.setTags(new ArrayList<>());
                        vo.setTagIds(new ArrayList<>());
                        vo.setTagList(new ArrayList<>());
                    }
                    
                    result.add(vo);
                }
            } catch (Exception e) {
                logger.warn("❌ 处理短视频失败: videoId={}, err={}", v.getId(), e.getMessage());
            }
        }

        logger.debug("短视频查询完成: 返回{}条记录，总数{}", result.size(), total);
        
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(result);
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 获取第一个视频URL（优先使用first_video_url字段）
     */
    private String getFirstVideoUrl(Video video) {
        String firstUrl = video.getFirstVideoUrl();
        if (firstUrl == null || firstUrl.trim().isEmpty()) {
            firstUrl = extractFirstVideoUrl(video.getVideoContent());
        }
        return firstUrl;
    }

    /**
     * 生成签名视频URL
     */
    private String generateSignedVideoUrl(String firstUrl) {
        try {
            ChiguaUrlService.ResourceType resourceType = firstUrl.toLowerCase().endsWith(".m3u8") 
                ? ChiguaUrlService.ResourceType.STREAM 
                : ChiguaUrlService.ResourceType.VIDEO;
            return chiguaUrlService.generateUrl(firstUrl, resourceType, false);
        } catch (Exception e) {
            logger.warn("❌ 生成视频签名URL失败: url={}, err={}", firstUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 创建短视频VO对象
     */
    private WebShortVideoVO createShortVideoVO(Video video, String signedUrl) {
        WebShortVideoVO vo = new WebShortVideoVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setPublishedAt(video.getPublishedAt());
        vo.setFirstVideoUrl(signedUrl);
        
        // 设置热门和推荐标识
        vo.setIsHot(video.getIsHot());
        vo.setIsRecommended(video.getIsRecommended());
        
        // 处理封面图片（使用前端专用方法，不带decrypt=true，前端会自动解密）
        String signedCover = null;
        try { 
            signedCover = videoUrlGeneratorService.generateCoverUrlForWeb(video); 
        } catch (Exception ignore) {}
        
        if (signedCover != null) {
            vo.setCoverImageUrl(signedCover);
        } else {
            String coverPath = video.getCoverUrl();
            if (coverPath == null || coverPath.trim().isEmpty()) { 
                coverPath = video.getCoverImage(); 
            }
            vo.setCoverImageUrl(coverPath);
        }
        // firstFrameUrl：优先使用数据库 first_frame_url 字段（需签名），没有则 fallback 到封面图
        String firstFrame = video.getFirstFrameUrl();
        if (firstFrame != null && !firstFrame.trim().isEmpty()) {
            try {
                String signedFirstFrame = chiguaUrlService.generateUrl(firstFrame, ChiguaUrlService.ResourceType.COVER, false);
                vo.setFirstFrameUrl(signedFirstFrame != null ? signedFirstFrame : vo.getCoverImageUrl());
            } catch (Exception ignore) {
                vo.setFirstFrameUrl(vo.getCoverImageUrl());
            }
        } else {
            vo.setFirstFrameUrl(vo.getCoverImageUrl());
        }
        
        return vo;
    }

    /**
     * 创建空的短视频查询结果
     */
    private TableDataInfo createEmptyShortVideoResult() {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(new ArrayList<>());
        rspData.setTotal(0L);
        return rspData;
    }

    /**
     * 解析富文本中的第一个视频URL
     */
    private String extractFirstVideoUrl(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        try {
            // 匹配 <video src="..."> 或 <source src="...">
            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("<video[^>]*src=\\\"([^\\\"]+)\\\"[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m1 = p1.matcher(content);
            if (m1.find()) {
                return normalizeUrl(m1.group(1));
            }
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("<source[^>]*src=\\\"([^\\\"]+)\\\"[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m2 = p2.matcher(content);
            if (m2.find()) {
                return normalizeUrl(m2.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String normalizeUrl(String src) {
        if (src == null) return null;
        String s = src.trim();
        // 如果是完整 http(s) URL，直接返回
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        // 去掉前导斜杠，保持 R2 资源相对路径格式
        if (s.startsWith("/")) return s.substring(1);
        return s;
    }

    /**
     * 获取缓存的热门推荐视频基础数据（不包含签名URL）
     * 这部分数据可以长期缓存，因为不包含会过期的签名URL
     */
    @Cacheable(value = "hotRecommendedVideosBasic", key = "'hot_basic_' + #pageNum + '_' + #pageSize", unless = "#result == null")
    public List<Video> getCachedHotRecommendedVideosBasic(Integer pageNum, Integer pageSize)
    {
        logger.debug("[缓存MISS] 获取热门推荐基础数据: pageNum={}, pageSize={}", pageNum, pageSize);
        
        // 设置分页参数
        PageHelper.startPage(pageNum, pageSize);
        
        // 查询条件：状态为已发布(1) 且 (是热门=1 或 是推荐=1)
        Video queryVideo = new Video();
        queryVideo.setStatus(1); // 只查询已发布的视频
        queryVideo.setIsHot(1);  // 热门视频
        
        // 🎯 关键：使用带缓存的基础查询方法（不生成签名URL，在外层统一处理）
        List<Video> hotVideos = videoService.getCachedVideoListBasic(queryVideo);
        
        // 如果热门视频不足，补充推荐视频
        if (hotVideos.size() < pageSize) {
            Video recommendedQuery = new Video();
            recommendedQuery.setStatus(1); // 只查询已发布的视频
            recommendedQuery.setIsRecommended(1); // 推荐视频
            
            List<Video> recommendedVideos = videoService.getCachedVideoListBasic(recommendedQuery);
            
            // 合并列表，避免重复
            for (Video recommended : recommendedVideos) {
                boolean exists = hotVideos.stream().anyMatch(hot -> hot.getId().equals(recommended.getId()));
                if (!exists && hotVideos.size() < pageSize) {
                    hotVideos.add(recommended);
                }
            }
        }
        
        // 限制返回数量
        if (hotVideos.size() > pageSize) {
            hotVideos = hotVideos.subList(0, pageSize);
        }
        
        //logger.info("✅ 热门推荐基础数据缓存完成: {}条记录", hotVideos.size());
        return hotVideos;
    }

    /**
     * 实时处理富文本内容，生成新鲜的签名URL（不缓存）
     * 解决富文本中图片签名过期的问题
     */
    private String processRichTextWithFreshUrls(String richTextContent) {
        if (!org.springframework.util.StringUtils.hasText(richTextContent)) {
            return richTextContent;
        }

        try {
            //logger.debug("🔄 实时处理富文本内容，生成新鲜签名URL");
            
            String processedContent = richTextContent;
            
            // 处理data-resource-key模式的图片
            processedContent = processDataResourceKeysWithFreshUrls(processedContent);
            
            // 处理相对路径的图片
            processedContent = processRelativePathsWithFreshUrls(processedContent);
            
            //logger.debug("✅ 富文本实时处理完成");
            return processedContent;
            
        } catch (Exception e) {
            logger.error("❌ 富文本实时处理失败", e);
            return richTextContent; // 降级返回原始内容
        }
    }

    /**
     * 处理data-resource-key模式，实时生成签名URL
     */
    private String processDataResourceKeysWithFreshUrls(String content) {
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img([^>]*data-resource-key=\"([^\"]+)\"[^>]*)>"
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String resourcePath = matcher.group(2);

            try {
                // 实时生成签名URL
                String signedUrl = chiguaUrlService.generateUrl(resourcePath, ChiguaUrlService.ResourceType.IMAGE, false);
                
                // 替换data-resource-key为src属性
                String newAttributes = imgAttributes.replace(
                    "data-resource-key=\"" + resourcePath + "\"",
                    "src=\"" + signedUrl + "\""
                );
                
                String replacement = "<img" + newAttributes + ">";
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                
               // logger.debug("🔗 实时生成图片签名URL: {} -> {}", resourcePath, signedUrl);
            } catch (Exception e) {
                logger.warn("❌ 为资源{}生成签名URL失败: {}", resourcePath, e.getMessage());
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 处理相对路径，实时生成签名URL
     */
    private String processRelativePathsWithFreshUrls(String content) {
        String result = content;
        
        // 1. 处理img标签的src属性
        result = processImageSrcWithFreshUrls(result);
        
        // 2. 处理video标签的src属性
        result = processVideoSrcWithFreshUrls(result);
        
        // 3. 处理source标签的src属性
        result = processSourceSrcWithFreshUrls(result);
        
        // 4. 处理video标签的poster属性
        result = processVideoPosterWithFreshUrls(result);
        
        return result;
    }
    
    /**
     * 处理图片src属性
     */
    private String processImageSrcWithFreshUrls(String content) {
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
            "<img([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = imgPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String imgAttributes = matcher.group(1);
            String src = matcher.group(2);

            // 只处理相对路径
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                try {
                    // 实时生成签名URL
                    String signedUrl = chiguaUrlService.generateUrl(src, ChiguaUrlService.ResourceType.IMAGE, false);
                    
                    String newAttributes = imgAttributes.replace(
                        "src=\"" + src + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + src + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<img" + newAttributes + ">";
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    
                    //logger.debug("🔗 实时生成相对路径图片签名URL: {} -> {}", src, signedUrl);
                } catch (Exception e) {
                    logger.warn("❌ 为相对路径{}生成签名URL失败: {}", src, e.getMessage());
                    // 保持原始标签
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            } else {
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理video标签的src属性
     */
    private String processVideoSrcWithFreshUrls(String content) {
        java.util.regex.Pattern videoPattern = java.util.regex.Pattern.compile(
            "<video([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = videoPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String videoAttributes = matcher.group(1);
            String src = matcher.group(2);

            // 只处理相对路径
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                try {
                    // 根据文件扩展名确定资源类型
                    ChiguaUrlService.ResourceType resourceType = src.toLowerCase().contains(".m3u8") 
                        ? ChiguaUrlService.ResourceType.STREAM 
                        : ChiguaUrlService.ResourceType.VIDEO;
                    
                    // 实时生成签名URL
                    String signedUrl = chiguaUrlService.generateUrl(src, resourceType, false);
                    
                    String newAttributes = videoAttributes.replace(
                        "src=\"" + src + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + src + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<video" + newAttributes + ">";
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    
                    logger.debug("🎬 实时生成视频签名URL: {} -> {}", src, signedUrl);
                } catch (Exception e) {
                    logger.warn("❌ 为视频路径{}生成签名URL失败: {}", src, e.getMessage());
                    // 保持原始标签
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            } else {
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理source标签的src属性
     */
    private String processSourceSrcWithFreshUrls(String content) {
        java.util.regex.Pattern sourcePattern = java.util.regex.Pattern.compile(
            "<source([^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*)>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = sourcePattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String sourceAttributes = matcher.group(1);
            String src = matcher.group(2);

            // 只处理相对路径
            if (!src.startsWith("http://") && !src.startsWith("https://") && !src.startsWith("blob:")) {
                try {
                    // 根据文件扩展名确定资源类型
                    ChiguaUrlService.ResourceType resourceType = src.toLowerCase().contains(".m3u8") 
                        ? ChiguaUrlService.ResourceType.STREAM 
                        : ChiguaUrlService.ResourceType.VIDEO;
                    
                    // 实时生成签名URL
                    String signedUrl = chiguaUrlService.generateUrl(src, resourceType, false);
                    
                    String newAttributes = sourceAttributes.replace(
                        "src=\"" + src + "\"",
                        "src=\"" + signedUrl + "\""
                    ).replace(
                        "src='" + src + "'",
                        "src='" + signedUrl + "'"
                    );
                    
                    String replacement = "<source" + newAttributes + ">";
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                    
                    //logger.debug("🎬 实时生成source签名URL: {} -> {}", src, signedUrl);
                } catch (Exception e) {
                    logger.warn("❌ 为source路径{}生成签名URL失败: {}", src, e.getMessage());
                    // 保持原始标签
                    matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            } else {
                // 保持原始标签
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 获取上一篇视频（优化的直接查询方式，带缓存）
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选）
     * @return 上一篇视频信息
     */
    @Cacheable(value = "adjacentVideos", key = "'prev_' + #currentVideoId + '_' + (#categoryId != null ? #categoryId : 'all') + '_' + (#searchKeyword != null ? #searchKeyword : 'none') + '_' + (#sortType != null ? #sortType : 'default')", unless = "#result == null")
    public WebVideoVO.AdjacentVideoInfo selectPreviousVideo(Long currentVideoId, Long categoryId, String searchKeyword, String sortType) {
        logger.info("🔍 获取上一篇视频 (优化查询): currentVideoId={}, categoryId={}, searchKeyword={}, sortType={}", 
            currentVideoId, categoryId, searchKeyword, sortType);
        
        try {
            Video previousVideo = videoMapper.selectPreviousVideoOptimized(currentVideoId, categoryId, searchKeyword, sortType);
            if (previousVideo != null) {
                logger.info("✅ 找到上一篇视频: id={}, title={}", previousVideo.getId(), previousVideo.getTitle());
                return new WebVideoVO.AdjacentVideoInfo(previousVideo.getId(), previousVideo.getTitle());
            } else {
                logger.info("ℹ️ 没有找到上一篇视频");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 获取上一篇视频失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取下一篇视频（优化的直接查询方式，带缓存）
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选）
     * @return 下一篇视频信息
     */
    @Cacheable(value = "adjacentVideos", key = "'next_' + #currentVideoId + '_' + (#categoryId != null ? #categoryId : 'all') + '_' + (#searchKeyword != null ? #searchKeyword : 'none') + '_' + (#sortType != null ? #sortType : 'default')", unless = "#result == null")
    public WebVideoVO.AdjacentVideoInfo selectNextVideo(Long currentVideoId, Long categoryId, String searchKeyword, String sortType) {
        logger.info("🔍 获取下一篇视频 (优化查询): currentVideoId={}, categoryId={}, searchKeyword={}, sortType={}", 
            currentVideoId, categoryId, searchKeyword, sortType);
        
        try {
            Video nextVideo = videoMapper.selectNextVideoOptimized(currentVideoId, categoryId, searchKeyword, sortType);
            if (nextVideo != null) {
                logger.info("✅ 找到下一篇视频: id={}, title={}", nextVideo.getId(), nextVideo.getTitle());
                return new WebVideoVO.AdjacentVideoInfo(nextVideo.getId(), nextVideo.getTitle());
            } else {
                logger.info("ℹ️ 没有找到下一篇视频");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 获取下一篇视频失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 基于列表查询获取相邻视频（核心方法）
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID
     * @param searchKeyword 搜索关键词
     * @param sortType 排序类型
     * @return 包含上一篇和下一篇视频的Map
     */
    private Map<String, WebVideoVO.AdjacentVideoInfo> getAdjacentVideosFromList(
            Long currentVideoId, Long categoryId, String searchKeyword, String sortType) {
        
        logger.info("📋 开始基于列表查询获取相邻视频: currentVideoId={}", currentVideoId);
        
        // 1. 构建与列表页面完全相同的查询条件
        Video queryParams = new Video();
        queryParams.setStatus(1); // 只查询有效视频
        
        if (categoryId != null) {
            queryParams.setCategoryId(categoryId);
            logger.info("🏷️ 设置分类条件: categoryId={}", categoryId);
        }
        
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            queryParams.setTitle(searchKeyword.trim());
            logger.info("🔍 设置搜索条件: keyword={}", searchKeyword.trim());
        }
        
        // 注意：sortType在当前的selectVideoList中不支持，它总是按 published_at DESC, id DESC 排序
        // 如果需要支持不同排序，需要修改VideoMapper.xml或创建新的查询方法
        
        try {
            // 2. 查询视频列表（使用与前端完全相同的查询逻辑）
            List<Video> videoList = videoService.selectVideoListWithSignedUrls(queryParams);
            logger.info("📊 查询到视频列表数量: {}", videoList.size());
            
            if (videoList.isEmpty()) {
                logger.warn("⚠️ 查询结果为空，无法获取相邻视频");
                return createEmptyAdjacentResult();
            }
            
            // 3. 在结果中找到当前视频的位置
            int currentIndex = -1;
            for (int i = 0; i < videoList.size(); i++) {
                if (videoList.get(i).getId().equals(currentVideoId)) {
                    currentIndex = i;
                    break;
                }
            }
            
            logger.info("📍 当前视频在列表中的位置: index={}", currentIndex);
            
            if (currentIndex == -1) {
                logger.warn("⚠️ 当前视频不在查询结果中，可能已被删除或不满足筛选条件");
                return createEmptyAdjacentResult();
            }
            
            // 4. 获取相邻视频
            WebVideoVO.AdjacentVideoInfo previousVideo = null;
            WebVideoVO.AdjacentVideoInfo nextVideo = null;
            
            // 上一篇视频（列表中的前一个）
            if (currentIndex > 0) {
                Video prev = videoList.get(currentIndex - 1);
                previousVideo = new WebVideoVO.AdjacentVideoInfo(prev.getId(), prev.getTitle());
                logger.info("⬅️ 找到上一篇视频: id={}, title={}", prev.getId(), prev.getTitle());
            } else {
                logger.info("⬅️ 已经是第一个视频，没有上一篇");
            }
            
            // 下一篇视频（列表中的后一个）
            if (currentIndex < videoList.size() - 1) {
                Video next = videoList.get(currentIndex + 1);
                nextVideo = new WebVideoVO.AdjacentVideoInfo(next.getId(), next.getTitle());
                logger.info("➡️ 找到下一篇视频: id={}, title={}", next.getId(), next.getTitle());
            } else {
                logger.info("➡️ 已经是最后一个视频，没有下一篇");
            }
            
            // 5. 返回结果
            Map<String, WebVideoVO.AdjacentVideoInfo> result = new HashMap<>();
            result.put("previous", previousVideo);
            result.put("next", nextVideo);
            
            logger.info("✅ 相邻视频获取完成: previous={}, next={}", 
                previousVideo != null ? previousVideo.getId() : "null",
                nextVideo != null ? nextVideo.getId() : "null");
            
            return result;
            
        } catch (Exception e) {
            logger.error("❌ 基于列表查询获取相邻视频失败: {}", e.getMessage(), e);
            return createEmptyAdjacentResult();
        }
    }
    
    /**
     * 创建空的相邻视频结果
     */
    private Map<String, WebVideoVO.AdjacentVideoInfo> createEmptyAdjacentResult() {
        Map<String, WebVideoVO.AdjacentVideoInfo> result = new HashMap<>();
        result.put("previous", null);
        result.put("next", null);
        return result;
    }

    // 保持向后兼容的重载方法
    @Override
    public WebVideoVO.AdjacentVideoInfo selectPreviousVideo(Long currentVideoId) {
        return selectPreviousVideo(currentVideoId, null, null, null);
    }

    @Override
    public WebVideoVO.AdjacentVideoInfo selectNextVideo(Long currentVideoId) {
        return selectNextVideo(currentVideoId, null, null, null);
    }

    /**
     * 获取相邻视频（上一篇和下一篇）
     * 
     * @param currentVideoId 当前视频ID
     * @return 相邻视频信息
     */
    @Override
    public WebVideoVO.AdjacentVideosVO selectAdjacentVideos(Long currentVideoId) {
        logger.info("🔍 获取相邻视频: currentVideoId={}", currentVideoId);
        
        try {
            WebVideoVO.AdjacentVideoInfo previousVideo = selectPreviousVideo(currentVideoId, null, null, null);
            WebVideoVO.AdjacentVideoInfo nextVideo = selectNextVideo(currentVideoId, null, null, null);
            
            WebVideoVO.AdjacentVideosVO result = new WebVideoVO.AdjacentVideosVO(previousVideo, nextVideo);
            
            logger.info("✅ 获取相邻视频成功: previous={}, next={}", 
                previousVideo != null ? previousVideo.getId() : "null",
                nextVideo != null ? nextVideo.getId() : "null");
            
            return result;
        } catch (Exception e) {
            logger.error("❌ 获取相邻视频失败: {}", e.getMessage(), e);
            return new WebVideoVO.AdjacentVideosVO(null, null);
        }
    }
    
    /**
     * 处理video标签的poster属性
     */
    private String processVideoPosterWithFreshUrls(String content) {
        java.util.regex.Pattern posterPattern = java.util.regex.Pattern.compile(
            "<video([^>]*?)poster=\"([^\"]*?)\"([^>]*?)>", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        java.util.regex.Matcher matcher = posterPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String beforePoster = matcher.group(1);
            String posterSrc = matcher.group(2);
            String afterPoster = matcher.group(3);
            
            // 跳过已经是完整URL的poster
            if (posterSrc.startsWith("http://") || posterSrc.startsWith("https://") || posterSrc.startsWith("blob:")) {
                continue;
            }
            
            try {
                // 使用ChiguaUrlService生成签名URL
                String signedUrl = chiguaUrlService.generateUrl(posterSrc, ChiguaUrlService.ResourceType.COVER, false);
                if (signedUrl != null) {
                    String replacement = "<video" + beforePoster + "poster=\"" + signedUrl + "\"" + afterPoster + ">";
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
                   // logger.debug("🎬 实时生成poster签名URL: {} -> {}", posterSrc, signedUrl);
                }
            } catch (Exception e) {
                logger.warn("❌ 为poster生成签名URL失败: {}", posterSrc, e);
            }
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取分类下所有去重的标签
     * 
     * @param categoryId 分类ID
     * @return 标签列表
     */
    @Override
    public List<Tag> selectDistinctTagsByCategory(Long categoryId)
    {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        try {
            List<Tag> tags = videoMapper.selectDistinctTagsByCategory(categoryId);
            logger.info("🏷️ 获取分类{}的去重标签成功，共{}个标签", categoryId, tags != null ? tags.size() : 0);
            return tags != null ? tags : new ArrayList<>();
        } catch (Exception e) {
            logger.error("❌ 获取分类{}的去重标签失败", categoryId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取分类下所有去重的作者
     * 
     * @param categoryId 分类ID
     * @return 作者列表（字符串列表）
     */
    @Override
    public List<String> selectDistinctAuthorsByCategory(Long categoryId)
    {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        try {
            List<String> authors = videoMapper.selectDistinctAuthorsByCategory(categoryId);
            logger.info("👤 获取分类{}的去重作者成功，共{}个作者", categoryId, authors != null ? authors.size() : 0);
            return authors != null ? authors : new ArrayList<>();
        } catch (Exception e) {
            logger.error("❌ 获取分类{}的去重作者失败", categoryId, e);
            return new ArrayList<>();
        }
    }

} 
