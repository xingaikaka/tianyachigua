package com.ruoyi.chigua.domain.vo.web;

import java.math.BigDecimal;

/**
 * Telegram 媒体文件 VO（前端展示用）
 */
public class WebTgMediaVO
{
    private Long id;

    /** 媒体类型：photo | video */
    private String mediaType;

    /** 前端可访问的文件 URL */
    private String localUrl;

    /** 宽度（像素） */
    private Integer width;

    /** 高度（像素） */
    private Integer height;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String mimeType;

    /** 视频时长（秒，photo 为 null） */
    private BigDecimal duration;

    /** 是否支持流媒体播放 */
    private Integer supportsStreaming;

    /** 视频封面图 URL（photo 为 null） */
    private String thumbUrl;

    private Integer thumbWidth;
    private Integer thumbHeight;

    /** 视频首帧图片 URL */
    private String firstFrameUrl;

    /** 在帖子内的显示顺序 */
    private Integer sortOrder;

    /** 是否推荐：1=是 0=否 */
    private Integer isRecommend;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getLocalUrl() { return localUrl; }
    public void setLocalUrl(String localUrl) { this.localUrl = localUrl; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public BigDecimal getDuration() { return duration; }
    public void setDuration(BigDecimal duration) { this.duration = duration; }

    public Integer getSupportsStreaming() { return supportsStreaming; }
    public void setSupportsStreaming(Integer supportsStreaming) { this.supportsStreaming = supportsStreaming; }

    public String getThumbUrl() { return thumbUrl; }
    public void setThumbUrl(String thumbUrl) { this.thumbUrl = thumbUrl; }

    public Integer getThumbWidth() { return thumbWidth; }
    public void setThumbWidth(Integer thumbWidth) { this.thumbWidth = thumbWidth; }

    public Integer getThumbHeight() { return thumbHeight; }
    public void setThumbHeight(Integer thumbHeight) { this.thumbHeight = thumbHeight; }

    public String getFirstFrameUrl() { return firstFrameUrl; }
    public void setFirstFrameUrl(String firstFrameUrl) { this.firstFrameUrl = firstFrameUrl; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getIsRecommend() { return isRecommend; }
    public void setIsRecommend(Integer isRecommend) { this.isRecommend = isRecommend; }
}
