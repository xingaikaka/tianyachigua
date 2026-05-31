package com.ruoyi.chigua.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 视频对象 videos
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Video extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 视频ID */
    private Long id;

    /** 视频标题 */
    @Excel(name = "视频标题")
    private String title;

    /** 视频副标题 */
    @Excel(name = "视频副标题")
    private String subtitle;

    /** 视频详细描述 */
    @Excel(name = "视频描述")
    private String description;

    /** 视频内容/副文本 */
    @Excel(name = "视频内容")
    private String videoContent;

    /** 首个视频的原始相对路径（m3u8/mp4），用于列表快速读取 */
    private String firstVideoUrl;

    /** 视频首帧图片路径（用于移动端滑动播放首帧占位） */
    private String firstFrameUrl;

    /** 视频封面图片URL */
    @Excel(name = "封面图片")
    private String coverImage;

    /** 视频时长（秒） */
    @Excel(name = "视频时长")
    private Integer duration;

    /** 文件大小（字节） */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** 视频作者 */
    @Excel(name = "视频作者")
    private String author;

    /** 视频来源ID（用于标识爬虫来源的视频唯一标识） */
    @Excel(name = "视频来源ID")
    private String sourceId;

    /** 主分类ID */
    @Excel(name = "主分类ID")
    private Long categoryId;

    /** 观看次数 */
    @Excel(name = "观看次数")
    private Integer viewCount;

    /** 评论数量 */
    @Excel(name = "评论数量")
    private Integer commentCount;

    /** 点赞数量 */
    @Excel(name = "点赞数量")
    private Integer likeCount;

    /** 分享次数 */
    @Excel(name = "分享次数")
    private Integer shareCount;

    /** 播放次数 */
    @Excel(name = "播放次数")
    private Integer playCount;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 是否推荐（1是 0否） */
    @Excel(name = "是否推荐", readConverterExp = "1=是,0=否")
    private Integer isRecommended;

    /** 是否热门（1是 0否） */
    @Excel(name = "是否热门", readConverterExp = "1=是,0=否")
    private Integer isHot;

    /** 状态（0草稿 1已发布 2已下架） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=已发布,2=已下架")
    private Integer status;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "发布时间不能为空")
    private Date publishedAt;

    /** 最后编辑时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后编辑时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastEditedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 关联的分类列表 */
    private List<Category> categories;

    /** 关联的标签列表 */
    private List<Tag> tags;

    /** 分类ID列表（用于前端传递） */
    private List<Long> categoryIds;

    /** 标签ID列表（用于前端传递） */
    private List<Long> tagIds;

    /** 主分类名称（用于显示） */
    private String categoryName;

    /** 图片数量 */
    private Integer imageCount;

    /** 地址数量 */
    private Integer urlCount;

    /** 批量操作的ID数组（用于前端传递） */
    private Long[] ids;

    // ==================== ppvod集成字段 ====================
    
    /** 转码任务ID */
    @Excel(name = "转码任务ID")
    private String transcodeId;

    /** 文件MD5值 */
    @Excel(name = "文件MD5")
    private String md5;

    /** 分享ID */
    @Excel(name = "分享ID")
    private String shareid;

    /** 原始文件名 */
    @Excel(name = "原始文件名")
    private String orgfile;

    /** 相对路径 */
    @Excel(name = "相对路径")
    private String rpath;

    /** 域名 */
    @Excel(name = "域名")
    private String domain;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String path;

    /** 文件后缀 */
    @Excel(name = "文件后缀")
    private String suffix;

    /** 分辨率 */
    @Excel(name = "分辨率")
    private String resolution;

    /** 码率 */
    @Excel(name = "码率")
    private Integer bitrate;

    /** 帧率 */
    @Excel(name = "帧率")
    private Double fps;

    /** 视频宽度 */
    @Excel(name = "视频宽度")
    private Integer width;

    /** 视频高度 */
    @Excel(name = "视频高度")
    private Integer height;

    /** 转码状态（processing处理中 completed完成 failed失败） */
    @Excel(name = "转码状态", readConverterExp = "processing=处理中,completed=完成,failed=失败")
    private String transcodeStatus;

    /** 缩略图列表 */
    private String thumbnails;

    /** 输出格式 */
    private String outputFormats;

    // ==================== SEO字段 ====================
    
    /** URL别名 */
    @Excel(name = "URL别名")
    private String slug;

    /** SEO标题 */
    @Excel(name = "SEO标题")
    private String metaTitle;

    /** SEO描述 */
    @Excel(name = "SEO描述")
    private String metaDescription;

    /** SEO关键词 */
    @Excel(name = "SEO关键词")
    private String metaKeywords;

    // ==================== 内容管理增强字段 ====================
    
    /** 视频类型（short短视频 long长视频） */
    @Excel(name = "视频类型", readConverterExp = "short=短视频,long=长视频")
    private String videoType;

    /** 视频质量（sd标清 hd高清 fhd全高清 4k超高清） */
    @Excel(name = "视频质量", readConverterExp = "sd=标清,hd=高清,fhd=全高清,4k=超高清")
    private String quality;

    /** 是否精选（1是 0否） */
    @Excel(name = "是否精选", readConverterExp = "1=是,0=否")
    private Integer isFeatured;

    /** 允许评论（1是 0否） */
    @Excel(name = "允许评论", readConverterExp = "1=是,0=否")
    private Integer allowComments;

    /** 语言 */
    @Excel(name = "语言")
    private String language;

    /** M3U8播放地址 */
    private String m3u8Url;

    /** 封面图片URL */
    private String coverUrl;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    @NotBlank(message = "视频标题不能为空")
    @Size(min = 0, max = 300, message = "视频标题长度不能超过300个字符")
    public String getTitle() 
    {
        return title;
    }

    public void setSubtitle(String subtitle) 
    {
        this.subtitle = subtitle;
    }

    @Size(min = 0, max = 500, message = "视频副标题长度不能超过500个字符")
    public String getSubtitle() 
    {
        return subtitle;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setVideoContent(String videoContent) 
    {
        this.videoContent = videoContent;
    }

    public String getVideoContent() 
    {
        return videoContent;
    }

    public String getFirstVideoUrl()
    {
        return firstVideoUrl;
    }

    public void setFirstVideoUrl(String firstVideoUrl)
    {
        this.firstVideoUrl = firstVideoUrl;
    }

    public String getFirstFrameUrl()
    {
        return firstFrameUrl;
    }

    public void setFirstFrameUrl(String firstFrameUrl)
    {
        this.firstFrameUrl = firstFrameUrl;
    }

    public void setCoverImage(String coverImage) 
    {
        this.coverImage = coverImage;
    }

    @Size(min = 0, max = 500, message = "封面图片URL长度不能超过500个字符")
    public String getCoverImage() 
    {
        return coverImage;
    }

    public void setDuration(Integer duration) 
    {
        this.duration = duration;
    }

    public Integer getDuration() 
    {
        return duration;
    }

    public void setFileSize(Long fileSize) 
    {
        this.fileSize = fileSize;
    }

    public Long getFileSize() 
    {
        return fileSize;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    @Size(min = 0, max = 100, message = "视频作者长度不能超过100个字符")
    public String getAuthor() 
    {
        return author;
    }

    public void setSourceId(String sourceId) 
    {
        this.sourceId = sourceId;
    }

    @Size(min = 0, max = 255, message = "视频来源ID长度不能超过255个字符")
    public String getSourceId() 
    {
        return sourceId;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setViewCount(Integer viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Integer getViewCount() 
    {
        return viewCount;
    }

    public void setCommentCount(Integer commentCount) 
    {
        this.commentCount = commentCount;
    }

    public Integer getCommentCount() 
    {
        return commentCount;
    }

    public void setLikeCount(Integer likeCount) 
    {
        this.likeCount = likeCount;
    }

    public Integer getLikeCount() 
    {
        return likeCount;
    }

    public void setShareCount(Integer shareCount) 
    {
        this.shareCount = shareCount;
    }

    public Integer getShareCount() 
    {
        return shareCount;
    }

    public void setPlayCount(Integer playCount) 
    {
        this.playCount = playCount;
    }

    public Integer getPlayCount() 
    {
        return playCount;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setIsRecommended(Integer isRecommended) 
    {
        this.isRecommended = isRecommended;
    }

    public Integer getIsRecommended() 
    {
        return isRecommended;
    }

    public void setIsHot(Integer isHot) 
    {
        this.isHot = isHot;
    }

    public Integer getIsHot() 
    {
        return isHot;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setPublishedAt(Date publishedAt) 
    {
        this.publishedAt = publishedAt;
    }

    public Date getPublishedAt() 
    {
        return publishedAt;
    }

    public void setLastEditedAt(Date lastEditedAt) 
    {
        this.lastEditedAt = lastEditedAt;
    }

    public Date getLastEditedAt() 
    {
        return lastEditedAt;
    }

    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) 
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() 
    {
        return updatedAt;
    }

    public List<Category> getCategories() 
    {
        return categories;
    }

    public void setCategories(List<Category> categories) 
    {
        this.categories = categories;
    }

    public List<Tag> getTags() 
    {
        return tags;
    }

    public void setTags(List<Tag> tags) 
    {
        this.tags = tags;
    }

    public List<Long> getCategoryIds() 
    {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) 
    {
        this.categoryIds = categoryIds;
    }

    public List<Long> getTagIds() 
    {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) 
    {
        this.tagIds = tagIds;
    }

    public String getCategoryName() 
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName) 
    {
        this.categoryName = categoryName;
    }

    public Integer getImageCount() 
    {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) 
    {
        this.imageCount = imageCount;
    }

    public Integer getUrlCount() 
    {
        return urlCount;
    }

    public void setUrlCount(Integer urlCount) 
    {
        this.urlCount = urlCount;
    }

    public Long[] getIds() 
    {
        return ids;
    }

    public void setIds(Long[] ids) 
    {
        this.ids = ids;
    }

    // ==================== ppvod集成字段的getter/setter ====================
    
    public String getTranscodeId() 
    {
        return transcodeId;
    }

    public void setTranscodeId(String transcodeId) 
    {
        this.transcodeId = transcodeId;
    }

    public String getMd5() 
    {
        return md5;
    }

    public void setMd5(String md5) 
    {
        this.md5 = md5;
    }

    public String getShareid() 
    {
        return shareid;
    }

    public void setShareid(String shareid) 
    {
        this.shareid = shareid;
    }

    public String getOrgfile() 
    {
        return orgfile;
    }

    public void setOrgfile(String orgfile) 
    {
        this.orgfile = orgfile;
    }

    public String getRpath() 
    {
        return rpath;
    }

    public void setRpath(String rpath) 
    {
        this.rpath = rpath;
    }

    public String getDomain() 
    {
        return domain;
    }

    public void setDomain(String domain) 
    {
        this.domain = domain;
    }

    public String getPath() 
    {
        return path;
    }

    public void setPath(String path) 
    {
        this.path = path;
    }

    public String getSuffix() 
    {
        return suffix;
    }

    public void setSuffix(String suffix) 
    {
        this.suffix = suffix;
    }

    public String getResolution() 
    {
        return resolution;
    }

    public void setResolution(String resolution) 
    {
        this.resolution = resolution;
    }

    public Integer getBitrate() 
    {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) 
    {
        this.bitrate = bitrate;
    }

    public Double getFps() 
    {
        return fps;
    }

    public void setFps(Double fps) 
    {
        this.fps = fps;
    }

    public Integer getWidth() 
    {
        return width;
    }

    public void setWidth(Integer width) 
    {
        this.width = width;
    }

    public Integer getHeight() 
    {
        return height;
    }

    public void setHeight(Integer height) 
    {
        this.height = height;
    }

    public String getTranscodeStatus() 
    {
        return transcodeStatus;
    }

    public void setTranscodeStatus(String transcodeStatus) 
    {
        this.transcodeStatus = transcodeStatus;
    }

    public String getThumbnails() 
    {
        return thumbnails;
    }

    public void setThumbnails(String thumbnails) 
    {
        this.thumbnails = thumbnails;
    }

    public String getOutputFormats() 
    {
        return outputFormats;
    }

    public void setOutputFormats(String outputFormats) 
    {
        this.outputFormats = outputFormats;
    }

    // ==================== SEO字段的getter/setter ====================
    
    public String getSlug() 
    {
        return slug;
    }

    public void setSlug(String slug) 
    {
        this.slug = slug;
    }

    public String getMetaTitle() 
    {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) 
    {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() 
    {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) 
    {
        this.metaDescription = metaDescription;
    }

    public String getMetaKeywords() 
    {
        return metaKeywords;
    }

    public void setMetaKeywords(String metaKeywords) 
    {
        this.metaKeywords = metaKeywords;
    }

    // ==================== 内容管理增强字段的getter/setter ====================
    
    public String getVideoType() 
    {
        return videoType;
    }

    public void setVideoType(String videoType) 
    {
        this.videoType = videoType;
    }

    public String getQuality() 
    {
        return quality;
    }

    public void setQuality(String quality) 
    {
        this.quality = quality;
    }

    public Integer getIsFeatured() 
    {
        return isFeatured;
    }

    public void setIsFeatured(Integer isFeatured) 
    {
        this.isFeatured = isFeatured;
    }

    public Integer getAllowComments() 
    {
        return allowComments;
    }

    public void setAllowComments(Integer allowComments) 
    {
        this.allowComments = allowComments;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }



    /**
     * 获取视频播放URL（资源路径，不包含域名）
     * 类似pornhub项目的设计，只返回路径部分
     */
    public String getVideoUrl() {
        if (this.rpath == null || this.path == null || this.suffix == null) {
            return null;
        }
        return this.rpath + "/" + this.path + "." + this.suffix;
    }

    /**
     * 获取M3U8播放列表URL（资源路径，不包含域名）
     */
    public String getM3u8Url() {
        if (this.rpath == null) {
            return null;
        }
        return this.rpath + "/index.m3u8";
    }

    public void setM3u8Url(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取纯粹的封面图片相对路径（用于URL生成）
     * 只处理coverImage字段，不受coverUrl字段影响
     * 返回相对路径，如：files/images/xxx.jpg
     */
    public String getCoverImagePath() {
        if (this.coverImage != null && !this.coverImage.trim().isEmpty()) {
            String coverImg = this.coverImage.trim();
            
            // 检查并处理完整URL（包含域名的情况）
            if (coverImg.startsWith("http://") || coverImg.startsWith("https://")) {
                // 如果是旧的错误域名，提取路径部分
                if (coverImg.contains("your-domain.workers.dev")) {
                    // 提取 /files/images/... 部分
                    int filesIndex = coverImg.indexOf("/files/");
                    if (filesIndex != -1) {
                        return coverImg.substring(filesIndex + 1); // 返回 files/images/...
                    }
                }
                // 如果是正确的域名，也提取路径部分以保持一致性
                if (coverImg.contains("tycgimage1.org")) {
                    int filesIndex = coverImg.indexOf("/files/");
                    if (filesIndex != -1) {
                        return coverImg.substring(filesIndex + 1); // 返回 files/images/...
                    }
                }
                // 其他完整URL，尝试提取路径部分
                try {
                    java.net.URL url = new java.net.URL(coverImg);
                    String path = url.getPath();
                    if (path.startsWith("/")) {
                        return path.substring(1); // 去掉前导斜杠
                    }
                    return path;
                } catch (Exception e) {
                    // URL解析失败，继续使用原逻辑
                }
            }
            
            // 如果封面图片已经是绝对路径（以/开头），处理路径
            if (coverImg.startsWith("/")) {
                // 如果是 /profile/images/ 开头，去掉 /profile/ 前缀
                if (coverImg.startsWith("/profile/images/")) {
                    return coverImg.substring("/profile/".length()); // 返回 images/...
                }
                // 其他情况去掉前导斜杠
                return coverImg.substring(1);
            }
            
            // 如果已经是相对路径，直接返回
            return coverImg;
        }
        
        return null;
    }

    /**
     * 获取封面图片URL（资源路径）
     * 优先使用coverUrl字段，否则使用coverImage，最后使用thumbnails数组的第一个
     * 类似pornhub项目设计，图片和视频使用相同的rpath结构
     * 修复：避免路径重复问题
     */
    public String getCoverUrl() {
        // 优先使用直接设置的coverUrl字段
        if (this.coverUrl != null && !this.coverUrl.trim().isEmpty()) {
            return this.coverUrl;
        }
        
        // 其次使用设置的封面图片 - 如果是相对路径，拼接rpath
        if (this.coverImage != null && !this.coverImage.trim().isEmpty()) {
            String coverImg = this.coverImage.trim();
            
            // 检查并处理完整URL（包含域名的情况）
            if (coverImg.startsWith("http://") || coverImg.startsWith("https://")) {
                // 如果是旧的错误域名，提取路径部分
                if (coverImg.contains("your-domain.workers.dev")) {
                    // 提取 /files/images/... 部分
                    int filesIndex = coverImg.indexOf("/files/");
                    if (filesIndex != -1) {
                        return coverImg.substring(filesIndex + 1); // 返回 files/images/...
                    }
                }
                // 如果是正确的域名，也提取路径部分以保持一致性
                if (coverImg.contains("tycgimage1.org")) {
                    int filesIndex = coverImg.indexOf("/files/");
                    if (filesIndex != -1) {
                        return coverImg.substring(filesIndex + 1); // 返回 files/images/...
                    }
                }
                // 其他完整URL，尝试提取路径部分
                try {
                    java.net.URL url = new java.net.URL(coverImg);
                    String path = url.getPath();
                    if (path.startsWith("/")) {
                        return path.substring(1); // 去掉前导斜杠
                    }
                    return path;
                } catch (Exception e) {
                    // URL解析失败，继续使用原逻辑
                }
            }
            
            // 如果封面图片已经是绝对路径（以/开头），处理路径
            if (coverImg.startsWith("/")) {
                // 如果是 /profile/images/ 开头，去掉 /profile/ 前缀
                if (coverImg.startsWith("/profile/images/")) {
                    return coverImg.substring("/profile/".length()); // 返回 images/...
                }
                // 其他情况去掉前导斜杠
                return coverImg.substring(1);
            }
            // 如果有rpath且封面图片是相对路径，拼接rpath
            if (this.rpath != null) {
                return this.rpath + "/" + coverImg;
            }
            return coverImg;
        }
        
        // 否则尝试使用第一张缩略图（缩略图与视频文件在同一目录）
        if (this.thumbnails != null && !this.thumbnails.trim().isEmpty()) {
            try {
                // 解析JSON数组，获取第一个缩略图
                if (this.thumbnails.startsWith("[") && this.thumbnails.endsWith("]")) {
                    String content = this.thumbnails.substring(1, this.thumbnails.length() - 1);
                    String[] thumbs = content.split(",");
                    if (thumbs.length > 0) {
                        String firstThumb = thumbs[0].trim().replace("\"", "");
                        if (!firstThumb.isEmpty()) {
                            // 重要修复：检查firstThumb是否已经包含完整路径
                            if (firstThumb.startsWith("/")) {
                                return firstThumb.substring(1); // 去掉前导斜杠
                            }
                            
                            // 检查是否包含rpath信息，避免重复拼接
                            if (this.rpath != null && !firstThumb.startsWith(this.rpath)) {
                                // 只有当缩略图路径不包含rpath时，才拼接rpath
                                return this.rpath + "/" + firstThumb;
                            }
                            
                            // 如果缩略图已经包含rpath或者rpath为空，直接返回
                            return firstThumb;
                        }
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，忽略错误
            }
        }
        
        return null;
    }

    /**
     * 获取缩略图URL列表（资源路径）
     * 类似pornhub项目设计，缩略图与视频文件使用相同的rpath结构
     */
    public java.util.List<String> getThumbnailUrls() {
        java.util.List<String> urls = new java.util.ArrayList<>();
        
        if (this.thumbnails == null || this.thumbnails.trim().isEmpty()) {
            return urls;
        }
        
        try {
            // 简单解析JSON数组，缩略图文件与视频文件在同一个rpath目录下
            if (this.thumbnails.startsWith("[") && this.thumbnails.endsWith("]")) {
                String content = this.thumbnails.substring(1, this.thumbnails.length() - 1);
                String[] thumbs = content.split(",");
                for (String thumb : thumbs) {
                    String cleanThumb = thumb.trim().replace("\"", "");
                    if (!cleanThumb.isEmpty()) {
                        // 修复：避免路径重复问题
                        if (cleanThumb.startsWith("/")) {
                            // 去掉前导斜杠，保持与R2存储格式一致
                            urls.add(cleanThumb.substring(1));
                        } else if (this.rpath != null && !cleanThumb.startsWith(this.rpath)) {
                            // 只有当缩略图路径不包含rpath时，才拼接rpath
                            urls.add(this.rpath + "/" + cleanThumb);
                        } else {
                            // 如果缩略图已经包含rpath或者rpath为空，直接使用
                            urls.add(cleanThumb);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // JSON解析失败，返回空列表
        }
        
        return urls;
    }

    /**
     * 设置缩略图URL列表（用于JSON反序列化）
     * 注意：这个setter主要用于缓存反序列化，实际数据基于thumbnails字段计算
     */
    public void setThumbnailUrls(java.util.List<String> thumbnailUrls) {
        // 可以选择不实现具体逻辑，因为这是计算属性
        // 或者可以反向更新thumbnails字段
    }

    /**
     * 检查是否有有效的视频文件信息
     */
    public boolean hasValidVideoFile() {
        return this.rpath != null && this.path != null && this.suffix != null;
    }

    /**
     * 检查是否有封面图片
     */
    public boolean hasCoverImage() {
        return (this.coverImage != null && !this.coverImage.trim().isEmpty()) ||
               (this.thumbnails != null && !this.thumbnails.trim().isEmpty());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("subtitle", getSubtitle())
            .append("description", getDescription())
            .append("videoContent", getVideoContent())
            .append("coverImage", getCoverImage())
            .append("duration", getDuration())
            .append("fileSize", getFileSize())
            .append("author", getAuthor())
            .append("categoryId", getCategoryId())
            .append("viewCount", getViewCount())
            .append("commentCount", getCommentCount())
            .append("likeCount", getLikeCount())
            .append("shareCount", getShareCount())
            .append("playCount", getPlayCount())
            .append("sortOrder", getSortOrder())
            .append("isRecommended", getIsRecommended())
            .append("isHot", getIsHot())
            .append("status", getStatus())
            .append("publishedAt", getPublishedAt())
            .append("lastEditedAt", getLastEditedAt())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 