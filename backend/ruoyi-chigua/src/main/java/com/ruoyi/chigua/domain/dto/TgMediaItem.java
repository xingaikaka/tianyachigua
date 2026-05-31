package com.ruoyi.chigua.domain.dto;

import java.math.BigDecimal;

/**
 * 入库请求中单条媒体信息
 */
public class TgMediaItem
{
    /** 媒体类型：photo / video */
    private String mediaType;

    /** 服务器本地文件路径 */
    private String localPath;

    /** 前端可访问 URL */
    private String localUrl;

    /** 宽度（像素） */
    private Integer width;

    /** 高度（像素） */
    private Integer height;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型，如 image/jpeg、video/mp4 */
    private String mimeType;

    /** 视频时长（秒，仅 video 类型填写） */
    private BigDecimal duration;

    /** 是否支持流媒体播放：1=是 0=否 */
    private Integer supportsStreaming;

    /** 封面图 URL（视频用） */
    private String thumbUrl;

    /** 封面图宽度 */
    private Integer thumbWidth;

    /** 封面图高度 */
    private Integer thumbHeight;

    /** 视频首帧图片 URL */
    private String firstFrameUrl;

    /** 帖子内显示顺序（从 0 开始） */
    private Integer sortOrder;

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

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
}
