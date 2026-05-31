package com.ruoyi.chigua.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 视频地址对象 video_urls
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class VideoUrl extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 视频地址ID */
    private Long id;

    /** 视频ID */
    @Excel(name = "视频ID")
    private Long videoId;

    /** 视频标题 */
    @Excel(name = "视频标题")
    private String title;

    /** 视频描述 */
    @Excel(name = "视频描述")
    private String description;

    /** 视频播放地址URL */
    @Excel(name = "播放地址")
    private String videoUrl;

    /** 集数编号 */
    @Excel(name = "集数编号")
    private Integer episodeNumber;

    /** 视频清晰度 */
    @Excel(name = "清晰度")
    private String quality;

    /** 视频格式 */
    @Excel(name = "视频格式")
    private String format;

    /** 视频时长（秒） */
    @Excel(name = "视频时长")
    private Integer duration;

    /** 文件大小（字节） */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** 视频码率（kbps） */
    @Excel(name = "视频码率")
    private Integer bitrate;

    /** 分辨率 */
    @Excel(name = "分辨率")
    private String resolution;

    /** 是否为主要播放地址（1是 0否） */
    @Excel(name = "是否主要地址", readConverterExp = "1=是,0=否")
    private Integer isPrimary;

    /** 播放次数统计 */
    @Excel(name = "播放次数")
    private Integer playCount;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 状态（1可用 0不可用 -1已删除） */
    @Excel(name = "状态", readConverterExp = "1=可用,0=不可用,-1=已删除")
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 关联的视频标题（用于显示） */
    private String videoTitle;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setVideoId(Long videoId) 
    {
        this.videoId = videoId;
    }

    @NotNull(message = "视频ID不能为空")
    public Long getVideoId() 
    {
        return videoId;
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

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setVideoUrl(String videoUrl) 
    {
        this.videoUrl = videoUrl;
    }

    @NotBlank(message = "播放地址不能为空")
    @Size(min = 0, max = 1000, message = "播放地址长度不能超过1000个字符")
    public String getVideoUrl() 
    {
        return videoUrl;
    }

    public void setEpisodeNumber(Integer episodeNumber) 
    {
        this.episodeNumber = episodeNumber;
    }

    public Integer getEpisodeNumber() 
    {
        return episodeNumber;
    }

    public void setQuality(String quality) 
    {
        this.quality = quality;
    }

    @Size(min = 0, max = 20, message = "清晰度长度不能超过20个字符")
    public String getQuality() 
    {
        return quality;
    }

    public void setFormat(String format) 
    {
        this.format = format;
    }

    @Size(min = 0, max = 20, message = "视频格式长度不能超过20个字符")
    public String getFormat() 
    {
        return format;
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

    public void setBitrate(Integer bitrate) 
    {
        this.bitrate = bitrate;
    }

    public Integer getBitrate() 
    {
        return bitrate;
    }

    public void setResolution(String resolution) 
    {
        this.resolution = resolution;
    }

    @Size(min = 0, max = 20, message = "分辨率长度不能超过20个字符")
    public String getResolution() 
    {
        return resolution;
    }

    public void setIsPrimary(Integer isPrimary) 
    {
        this.isPrimary = isPrimary;
    }

    public Integer getIsPrimary() 
    {
        return isPrimary;
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

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
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

    public String getVideoTitle() 
    {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) 
    {
        this.videoTitle = videoTitle;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("videoId", getVideoId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("videoUrl", getVideoUrl())
            .append("episodeNumber", getEpisodeNumber())
            .append("quality", getQuality())
            .append("format", getFormat())
            .append("duration", getDuration())
            .append("fileSize", getFileSize())
            .append("bitrate", getBitrate())
            .append("resolution", getResolution())
            .append("isPrimary", getIsPrimary())
            .append("playCount", getPlayCount())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 