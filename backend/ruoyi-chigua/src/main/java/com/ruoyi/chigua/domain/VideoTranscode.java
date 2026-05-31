package com.ruoyi.chigua.domain;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 视频记录对象 video_transcodes
 * （已调整为与pronhub videos表结构一致）
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
public class VideoTranscode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long id;

    /** 上传用户ID */
    @Excel(name = "上传用户ID")
    private String uid;

    /** 视频类型 */
    @Excel(name = "视频类型", readConverterExp = "short=短视频,long=长视频")
    private String videoType;

    /** 视频标题 */
    @Excel(name = "视频标题")
    private String title;

    /** 视频描述 */
    @Excel(name = "视频描述")
    private String description;

    /** 单次观看价格 */
    @Excel(name = "单次观看价格")
    private BigDecimal price;

    /** 金币价格 */
    @Excel(name = "金币价格")
    private Integer coinPrice;

    /** 积分价格 */
    @Excel(name = "积分价格")
    private Integer pointsPrice;

    /** 优惠价格 */
    @Excel(name = "优惠价格")
    private BigDecimal discountPrice;

    /** 优惠金币价格 */
    @Excel(name = "优惠金币价格")
    private Integer discountCoinPrice;

    /** 优惠开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "优惠开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date discountStartAt;

    /** 优惠结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "优惠结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date discountEndAt;

    /** 创作者分成比例(0-1) */
    @Excel(name = "创作者分成比例")
    private BigDecimal creatorRevenueRate;

    /** 所需VIP等级(0=无要求) */
    @Excel(name = "所需VIP等级")
    private Integer requiredVipLevel;

    /** 是否VIP专享 */
    @Excel(name = "是否VIP专享", readConverterExp = "0=否,1=是")
    private Integer isVipOnly;

    /** 是否高级会员专享 */
    @Excel(name = "是否高级会员专享", readConverterExp = "0=否,1=是")
    private Integer isPremiumOnly;

    /** 转码任务ID */
    @Excel(name = "转码任务ID")
    private String transcodeId;

    /** 文件MD5值 */
    @Excel(name = "文件MD5值")
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

    /** 缩略图列表 */
    private String thumbnails;

    /** 封面图片URL */
    @Excel(name = "封面图片URL")
    private String coverImage;

    /** 内容类型 */
    @Excel(name = "内容类型", readConverterExp = "free=免费,ppv=按次付费,subscription=订阅,vip=VIP,premium=高级会员,coin=金币,points=积分")
    private String contentType;

    /** 是否精选 */
    @Excel(name = "是否精选", readConverterExp = "0=否,1=是")
    private Integer isFeatured;

    /** 是否编辑推荐 */
    @Excel(name = "是否编辑推荐", readConverterExp = "0=否,1=是")
    private Integer isEditorChoice;

    /** 是否热门 */
    @Excel(name = "是否热门", readConverterExp = "0=否,1=是")
    private Integer isHot;

    /** 是否新品 */
    @Excel(name = "是否新品", readConverterExp = "0=否,1=是")
    private Integer isNew;

    /** 是否独家 */
    @Excel(name = "是否独家", readConverterExp = "0=否,1=是")
    private Integer isExclusive;

    /** 是否热门 */
    @Excel(name = "是否热门", readConverterExp = "0=否,1=是")
    private Integer isTrending;

    /** 是否私密 */
    @Excel(name = "是否私密", readConverterExp = "0=否,1=是")
    private Integer isPrivate;

    /** 是否成人内容 */
    @Excel(name = "是否成人内容", readConverterExp = "0=否,1=是")
    private Integer isAdult;

    /** 是否已使用（关联到videos表） */
    @Excel(name = "是否已使用", readConverterExp = "0=否,1=是")
    private Integer isUsed;

    /** 分辨率 */
    @Excel(name = "分辨率")
    private String resolution;

    /** 域名 */
    @Excel(name = "域名")
    private String domain;

    /** 图片域名 */
    @Excel(name = "图片域名")
    private String picdomain;

    /** MP4域名 */
    @Excel(name = "MP4域名")
    private String mp4domain;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String path;

    /** 文件后缀 */
    @Excel(name = "文件后缀")
    private String suffix;

    /** 码率 */
    @Excel(name = "码率")
    private Integer bitrate;

    /** 文件大小 */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** 视频时长（秒） */
    @Excel(name = "视频时长")
    private Integer duration;

    /** 帧率 */
    @Excel(name = "帧率")
    private BigDecimal fps;

    /** 视频宽度 */
    @Excel(name = "视频宽度")
    private Integer width;

    /** 视频高度 */
    @Excel(name = "视频高度")
    private Integer height;

    /** 播放次数 */
    @Excel(name = "播放次数")
    private Long viewsCount;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Long likesCount;

    /** 踩数 */
    @Excel(name = "踩数")
    private Long dislikeCount;

    /** 收藏数 */
    @Excel(name = "收藏数")
    private Long favoriteCount;

    /** 分享数 */
    @Excel(name = "分享数")
    private Long shareCount;

    /** 下载次数 */
    @Excel(name = "下载次数")
    private Long downloadCount;

    /** 购买次数 */
    @Excel(name = "购买次数")
    private Long purchaseCount;

    /** 总收益 */
    @Excel(name = "总收益")
    private BigDecimal totalRevenue;

    /** 每日观看限制(0=无限制) */
    @Excel(name = "每日观看限制")
    private Integer dailyViewLimit;

    /** 评论数 */
    @Excel(name = "评论数")
    private Long commentsCount;

    /** 免费预览时长(秒) */
    @Excel(name = "免费预览时长")
    private Integer freePreviewDuration;

    /** 是否允许预览 */
    @Excel(name = "是否允许预览", readConverterExp = "0=否,1=是")
    private Integer allowPreview;

    /** 年龄分级(0-21) */
    @Excel(name = "年龄分级")
    private Integer ageRating;

    /** 视频质量 */
    @Excel(name = "视频质量", readConverterExp = "sd=标清,hd=高清,fhd=全高清,4k=超高清,8k=8K")
    private String quality;

    /** 允许评论 */
    @Excel(name = "允许评论", readConverterExp = "0=否,1=是")
    private Integer allowComments;

    /** 允许下载 */
    @Excel(name = "允许下载", readConverterExp = "0=否,1=是")
    private Integer allowDownloads;

    /** 允许观看的国家 */
    private String allowedCountries;

    /** 禁止观看的国家 */
    private String blockedCountries;

    /** 视频语言 */
    @Excel(name = "视频语言")
    private String language;

    /** 字幕信息 */
    private String subtitles;

    /** 转码开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "转码开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date transcodeBegin;

    /** 转码结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "转码结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date transcodeEnd;

    /** 转码结果 */
    @Excel(name = "转码结果", readConverterExp = "ok=成功,failed=失败,pending=处理中")
    private String transcodeResult;

    /** 输出格式信息 */
    private String outputFormats;

    /** 种子哈希值 */
    @Excel(name = "种子哈希值")
    private String infoHash;

    /** SEO友好的URL */
    @Excel(name = "SEO友好的URL")
    private String slug;

    /** SEO标题 */
    @Excel(name = "SEO标题")
    private String metaTitle;

    /** SEO描述 */
    @Excel(name = "SEO描述")
    private String metaDescription;

    /** SEO关键词 */
    private String metaKeywords;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishedAt;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "processing=处理中,completed=已完成,failed=失败,published=已发布,draft=草稿,deleted=已删除")
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 删除时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "删除时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date deletedAt;

    // Getter and Setter methods
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setCoinPrice(Integer coinPrice) {
        this.coinPrice = coinPrice;
    }

    public Integer getCoinPrice() {
        return coinPrice;
    }

    public void setPointsPrice(Integer pointsPrice) {
        this.pointsPrice = pointsPrice;
    }

    public Integer getPointsPrice() {
        return pointsPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountCoinPrice(Integer discountCoinPrice) {
        this.discountCoinPrice = discountCoinPrice;
    }

    public Integer getDiscountCoinPrice() {
        return discountCoinPrice;
    }

    public void setDiscountStartAt(Date discountStartAt) {
        this.discountStartAt = discountStartAt;
    }

    public Date getDiscountStartAt() {
        return discountStartAt;
    }

    public void setDiscountEndAt(Date discountEndAt) {
        this.discountEndAt = discountEndAt;
    }

    public Date getDiscountEndAt() {
        return discountEndAt;
    }

    public void setCreatorRevenueRate(BigDecimal creatorRevenueRate) {
        this.creatorRevenueRate = creatorRevenueRate;
    }

    public BigDecimal getCreatorRevenueRate() {
        return creatorRevenueRate;
    }

    public void setRequiredVipLevel(Integer requiredVipLevel) {
        this.requiredVipLevel = requiredVipLevel;
    }

    public Integer getRequiredVipLevel() {
        return requiredVipLevel;
    }

    public void setIsVipOnly(Integer isVipOnly) {
        this.isVipOnly = isVipOnly;
    }

    public Integer getIsVipOnly() {
        return isVipOnly;
    }

    public void setIsPremiumOnly(Integer isPremiumOnly) {
        this.isPremiumOnly = isPremiumOnly;
    }

    public Integer getIsPremiumOnly() {
        return isPremiumOnly;
    }

    public void setTranscodeId(String transcodeId) {
        this.transcodeId = transcodeId;
    }

    public String getTranscodeId() {
        return transcodeId;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }

    public void setShareid(String shareid) {
        this.shareid = shareid;
    }

    public String getShareid() {
        return shareid;
    }

    public void setOrgfile(String orgfile) {
        this.orgfile = orgfile;
    }

    public String getOrgfile() {
        return orgfile;
    }

    public void setRpath(String rpath) {
        this.rpath = rpath;
    }

    public String getRpath() {
        return rpath;
    }

    public void setThumbnails(String thumbnails) {
        this.thumbnails = thumbnails;
    }

    public String getThumbnails() {
        return thumbnails;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setIsFeatured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getIsFeatured() {
        return isFeatured;
    }

    public void setIsEditorChoice(Integer isEditorChoice) {
        this.isEditorChoice = isEditorChoice;
    }

    public Integer getIsEditorChoice() {
        return isEditorChoice;
    }

    public void setIsHot(Integer isHot) {
        this.isHot = isHot;
    }

    public Integer getIsHot() {
        return isHot;
    }

    public void setIsNew(Integer isNew) {
        this.isNew = isNew;
    }

    public Integer getIsNew() {
        return isNew;
    }

    public void setIsExclusive(Integer isExclusive) {
        this.isExclusive = isExclusive;
    }

    public Integer getIsExclusive() {
        return isExclusive;
    }

    public void setIsTrending(Integer isTrending) {
        this.isTrending = isTrending;
    }

    public Integer getIsTrending() {
        return isTrending;
    }

    public void setIsPrivate(Integer isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Integer getIsPrivate() {
        return isPrivate;
    }

    public void setIsAdult(Integer isAdult) {
        this.isAdult = isAdult;
    }

    public Integer getIsAdult() {
        return isAdult;
    }

    public void setIsUsed(Integer isUsed) {
        this.isUsed = isUsed;
    }

    public Integer getIsUsed() {
        return isUsed;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getResolution() {
        return resolution;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setPicdomain(String picdomain) {
        this.picdomain = picdomain;
    }

    public String getPicdomain() {
        return picdomain;
    }

    public void setMp4domain(String mp4domain) {
        this.mp4domain = mp4domain;
    }

    public String getMp4domain() {
        return mp4domain;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setFps(BigDecimal fps) {
        this.fps = fps;
    }

    public BigDecimal getFps() {
        return fps;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getWidth() {
        return width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getHeight() {
        return height;
    }

    public void setViewsCount(Long viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Long getViewsCount() {
        return viewsCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getLikesCount() {
        return likesCount;
    }

    public void setDislikeCount(Long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public Long getDislikeCount() {
        return dislikeCount;
    }

    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Long getFavoriteCount() {
        return favoriteCount;
    }

    public void setShareCount(Long shareCount) {
        this.shareCount = shareCount;
    }

    public Long getShareCount() {
        return shareCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setPurchaseCount(Long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public Long getPurchaseCount() {
        return purchaseCount;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setDailyViewLimit(Integer dailyViewLimit) {
        this.dailyViewLimit = dailyViewLimit;
    }

    public Integer getDailyViewLimit() {
        return dailyViewLimit;
    }

    public void setCommentsCount(Long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Long getCommentsCount() {
        return commentsCount;
    }

    public void setFreePreviewDuration(Integer freePreviewDuration) {
        this.freePreviewDuration = freePreviewDuration;
    }

    public Integer getFreePreviewDuration() {
        return freePreviewDuration;
    }

    public void setAllowPreview(Integer allowPreview) {
        this.allowPreview = allowPreview;
    }

    public Integer getAllowPreview() {
        return allowPreview;
    }

    public void setAgeRating(Integer ageRating) {
        this.ageRating = ageRating;
    }

    public Integer getAgeRating() {
        return ageRating;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getQuality() {
        return quality;
    }

    public void setAllowComments(Integer allowComments) {
        this.allowComments = allowComments;
    }

    public Integer getAllowComments() {
        return allowComments;
    }

    public void setAllowDownloads(Integer allowDownloads) {
        this.allowDownloads = allowDownloads;
    }

    public Integer getAllowDownloads() {
        return allowDownloads;
    }

    public void setAllowedCountries(String allowedCountries) {
        this.allowedCountries = allowedCountries;
    }

    public String getAllowedCountries() {
        return allowedCountries;
    }

    public void setBlockedCountries(String blockedCountries) {
        this.blockedCountries = blockedCountries;
    }

    public String getBlockedCountries() {
        return blockedCountries;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public void setTranscodeBegin(Date transcodeBegin) {
        this.transcodeBegin = transcodeBegin;
    }

    public Date getTranscodeBegin() {
        return transcodeBegin;
    }

    public void setTranscodeEnd(Date transcodeEnd) {
        this.transcodeEnd = transcodeEnd;
    }

    public Date getTranscodeEnd() {
        return transcodeEnd;
    }

    public void setTranscodeResult(String transcodeResult) {
        this.transcodeResult = transcodeResult;
    }

    public String getTranscodeResult() {
        return transcodeResult;
    }

    public void setOutputFormats(String outputFormats) {
        this.outputFormats = outputFormats;
    }

    public String getOutputFormats() {
        return outputFormats;
    }

    public void setInfoHash(String infoHash) {
        this.infoHash = infoHash;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaKeywords(String metaKeywords) {
        this.metaKeywords = metaKeywords;
    }

    public String getMetaKeywords() {
        return metaKeywords;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("uid", getUid())
            .append("videoType", getVideoType())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("price", getPrice())
            .append("coinPrice", getCoinPrice())
            .append("pointsPrice", getPointsPrice())
            .append("discountPrice", getDiscountPrice())
            .append("discountCoinPrice", getDiscountCoinPrice())
            .append("discountStartAt", getDiscountStartAt())
            .append("discountEndAt", getDiscountEndAt())
            .append("creatorRevenueRate", getCreatorRevenueRate())
            .append("requiredVipLevel", getRequiredVipLevel())
            .append("isVipOnly", getIsVipOnly())
            .append("isPremiumOnly", getIsPremiumOnly())
            .append("transcodeId", getTranscodeId())
            .append("md5", getMd5())
            .append("shareid", getShareid())
            .append("orgfile", getOrgfile())
            .append("rpath", getRpath())
            .append("thumbnails", getThumbnails())
            .append("coverImage", getCoverImage())
            .append("contentType", getContentType())
            .append("isFeatured", getIsFeatured())
            .append("isEditorChoice", getIsEditorChoice())
            .append("isHot", getIsHot())
            .append("isNew", getIsNew())
            .append("isExclusive", getIsExclusive())
            .append("isTrending", getIsTrending())
            .append("isPrivate", getIsPrivate())
            .append("isAdult", getIsAdult())
            .append("isUsed", getIsUsed())
            .append("resolution", getResolution())
            .append("domain", getDomain())
            .append("picdomain", getPicdomain())
            .append("mp4domain", getMp4domain())
            .append("path", getPath())
            .append("suffix", getSuffix())
            .append("bitrate", getBitrate())
            .append("fileSize", getFileSize())
            .append("duration", getDuration())
            .append("fps", getFps())
            .append("width", getWidth())
            .append("height", getHeight())
            .append("viewsCount", getViewsCount())
            .append("likesCount", getLikesCount())
            .append("dislikeCount", getDislikeCount())
            .append("favoriteCount", getFavoriteCount())
            .append("shareCount", getShareCount())
            .append("downloadCount", getDownloadCount())
            .append("purchaseCount", getPurchaseCount())
            .append("totalRevenue", getTotalRevenue())
            .append("dailyViewLimit", getDailyViewLimit())
            .append("commentsCount", getCommentsCount())
            .append("freePreviewDuration", getFreePreviewDuration())
            .append("allowPreview", getAllowPreview())
            .append("ageRating", getAgeRating())
            .append("quality", getQuality())
            .append("allowComments", getAllowComments())
            .append("allowDownloads", getAllowDownloads())
            .append("allowedCountries", getAllowedCountries())
            .append("blockedCountries", getBlockedCountries())
            .append("language", getLanguage())
            .append("subtitles", getSubtitles())
            .append("transcodeBegin", getTranscodeBegin())
            .append("transcodeEnd", getTranscodeEnd())
            .append("transcodeResult", getTranscodeResult())
            .append("outputFormats", getOutputFormats())
            .append("infoHash", getInfoHash())
            .append("slug", getSlug())
            .append("metaTitle", getMetaTitle())
            .append("metaDescription", getMetaDescription())
            .append("metaKeywords", getMetaKeywords())
            .append("publishedAt", getPublishedAt())
            .append("status", getStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .append("deletedAt", getDeletedAt())
            .toString();
    }

    /**
     * URL访问器方法 - 与pronhub Video模型保持一致
     * 以下方法用于构建资源URL路径（不包含域名）
     */

    /**
     * 获取视频播放URL（资源路径，不包含域名）
     * 类似pornhub项目的设计，只返回路径部分
     */
    public String getVideoUrl() {
        // 🔧 修复：智能处理路径拼接
        if (this.rpath == null) {
            return null;
        }
        
        // 如果rpath已经包含了完整的视频文件路径，直接返回
        if (this.rpath.contains(".mp4") || this.rpath.contains(".avi") || this.rpath.contains(".webm")) {
            return this.rpath;
        }
        
        // 🔧 关键修复：处理包含转码子目录的情况
        String basePath = this.rpath;
        
        // 移除常见的转码子目录，回到原始文件目录
        basePath = basePath.replaceFirst("/(\\d+kb?|\\d+p?)/hls$", "");
        basePath = basePath.replaceFirst("/hls$", "");
        basePath = basePath.replaceFirst("/(low|medium|high|sd|hd|fhd|4k)/hls$", "");
        
        // 尝试拼接path和suffix
        if (this.path != null && this.suffix != null) {
            return basePath + "/" + this.path + "." + this.suffix;
        }
        
        // 如果没有path和suffix，尝试使用orgfile
        if (this.orgfile != null) {
            return basePath + "/" + this.orgfile;
        }
        
        // 最后返回处理过的基础路径
        return basePath;
    }

    /**
     * 获取M3U8播放列表URL（资源路径，不包含域名）
     * 🎯 适配ppvod新数据结构：优先使用path字段，备用rpath拼接
     */
    public String getM3u8Url() {
        // 🎯 新逻辑：优先使用path字段（ppvod直接保存完整路径）
        if (this.path != null && !this.path.trim().isEmpty()) {
            String pathValue = this.path.trim();
            // 如果path已经包含完整的m3u8路径，直接返回
            if (pathValue.endsWith(".m3u8")) {
                return pathValue;
            }
        }
        
        // 🔧 备用逻辑：使用rpath拼接（兼容旧数据）
        if (this.rpath == null) {
            return null;
        }
        
        // 如果rpath已经包含了完整的m3u8路径，直接返回
        if (this.rpath.endsWith(".m3u8")) {
            return this.rpath;
        } else {
            // 处理包含转码子目录的情况
            String basePath = this.rpath;
            
            // 移除常见的转码子目录
            basePath = basePath.replaceFirst("/(\\d+kb?|\\d+p?)/hls$", "");
            basePath = basePath.replaceFirst("/hls$", "");
            basePath = basePath.replaceFirst("/(low|medium|high|sd|hd|fhd|4k)/hls$", "");
            
            return basePath + "/index.m3u8";
        }
    }

    /**
     * 获取封面图片URL（资源路径）
     * 🎯 适配ppvod新数据结构：cover_image字段包含完整路径
     * 优先使用coverImage，否则使用thumbnails数组的第一个
     */
    public String getCoverUrl() {
        // 🎯 新逻辑：优先使用cover_image字段（ppvod直接保存完整路径）
        if (this.coverImage != null && !this.coverImage.trim().isEmpty()) {
            String coverImg = this.coverImage.trim();
            
            // ppvod保存的格式：videos/{video_id}/thumbnail.jpg
            // 直接返回，无需额外处理
            return coverImg;
        }
        
        // 🔧 备用逻辑：使用thumbnails JSON数组（兼容旧数据）
        if (this.thumbnails != null && !this.thumbnails.trim().isEmpty()) {
            try {
                // 解析JSON数组，获取第一个缩略图
                if (this.thumbnails.startsWith("[") && this.thumbnails.endsWith("]")) {
                    String content = this.thumbnails.substring(1, this.thumbnails.length() - 1);
                    String[] thumbs = content.split(",");
                    if (thumbs.length > 0) {
                        String firstThumb = thumbs[0].trim().replace("\"", "");
                        if (!firstThumb.isEmpty()) {
                            // 检查是否已经是完整路径
                            if (firstThumb.startsWith("/")) {
                                return firstThumb.substring(1); // 去掉前导斜杠
                            }
                            
                            // 如果是相对路径且有rpath，进行拼接
                            if (this.rpath != null && !firstThumb.startsWith(this.rpath)) {
                                return this.rpath + "/" + firstThumb;
                            }
                            
                            return firstThumb;
                        }
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，忽略错误
            }
        }
        
        // 🎯 最后备用：如果没有封面图，尝试从rpath自动拼接
        if (this.rpath != null && !this.rpath.trim().isEmpty()) {
            return this.rpath + "/thumbnail.jpg";
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

    /**
     * 获取优先播放URL（M3U8优先，其次MP4）
     * 类似pornhub项目的逻辑
     */
    public String getPreferredPlayUrl() {
        String m3u8Url = getM3u8Url();
        String videoUrl = getVideoUrl();
        return (m3u8Url != null && !m3u8Url.trim().isEmpty()) ? m3u8Url : videoUrl;
    }
} 