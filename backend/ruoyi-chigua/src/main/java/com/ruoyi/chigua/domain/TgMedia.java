package com.ruoyi.chigua.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 媒体文件对象 tg_media（每张图片/视频一条）
 */
public class TgMedia extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "关联帖子ID")
    private Long postId;

    @Excel(name = "媒体类型", readConverterExp = "photo=图片,video=视频")
    private String mediaType;

    @Excel(name = "本地路径")
    private String localPath;

    @Excel(name = "访问URL")
    private String localUrl;

    @Excel(name = "宽度")
    private Integer width;

    @Excel(name = "高度")
    private Integer height;

    @Excel(name = "文件大小(字节)")
    private Long fileSize;

    @Excel(name = "MIME类型")
    private String mimeType;

    @Excel(name = "视频时长(秒)")
    private BigDecimal duration;

    @Excel(name = "支持流播放", readConverterExp = "1=是,0=否")
    private Integer supportsStreaming;

    @Excel(name = "封面图URL")
    private String thumbUrl;

    private Integer thumbWidth;
    private Integer thumbHeight;

    @Excel(name = "视频首帧图片URL")
    private String firstFrameUrl;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "是否推荐", readConverterExp = "1=是,0=否")
    private Integer isRecommend;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

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

    public Integer getIsRecommend() { return isRecommend; }
    public void setIsRecommend(Integer isRecommend) { this.isRecommend = isRecommend; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
