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
 * 视频图片对象 video_images
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class VideoImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图片ID */
    private Long id;

    /** 视频ID */
    @Excel(name = "视频ID")
    private Long videoId;

    /** 图片标题 */
    @Excel(name = "图片标题")
    private String title;

    /** 图片描述 */
    @Excel(name = "图片描述")
    private String description;

    /** 图片URL地址 */
    @Excel(name = "图片URL")
    private String imageUrl;

    /** 图片文件大小（字节） */
    @Excel(name = "文件大小")
    private Integer fileSize;

    /** 图片宽度（像素） */
    @Excel(name = "图片宽度")
    private Integer width;

    /** 图片高度（像素） */
    @Excel(name = "图片高度")
    private Integer height;

    /** 图片替代文本 */
    @Excel(name = "替代文本")
    private String altText;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 是否为主图（1是 0否） */
    @Excel(name = "是否主图", readConverterExp = "1=是,0=否")
    private Integer isPrimary;

    /** 状态（1启用 0禁用） */
    @Excel(name = "状态", readConverterExp = "1=启用,0=禁用")
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

    @Size(min = 0, max = 200, message = "图片标题长度不能超过200个字符")
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

    public void setImageUrl(String imageUrl) 
    {
        this.imageUrl = imageUrl;
    }

    @NotBlank(message = "图片URL不能为空")
    @Size(min = 0, max = 500, message = "图片URL长度不能超过500个字符")
    public String getImageUrl() 
    {
        return imageUrl;
    }

    public void setFileSize(Integer fileSize) 
    {
        this.fileSize = fileSize;
    }

    public Integer getFileSize() 
    {
        return fileSize;
    }

    public void setWidth(Integer width) 
    {
        this.width = width;
    }

    public Integer getWidth() 
    {
        return width;
    }

    public void setHeight(Integer height) 
    {
        this.height = height;
    }

    public Integer getHeight() 
    {
        return height;
    }

    public void setAltText(String altText) 
    {
        this.altText = altText;
    }

    @Size(min = 0, max = 200, message = "替代文本长度不能超过200个字符")
    public String getAltText() 
    {
        return altText;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setIsPrimary(Integer isPrimary) 
    {
        this.isPrimary = isPrimary;
    }

    public Integer getIsPrimary() 
    {
        return isPrimary;
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
            .append("imageUrl", getImageUrl())
            .append("fileSize", getFileSize())
            .append("width", getWidth())
            .append("height", getHeight())
            .append("altText", getAltText())
            .append("sortOrder", getSortOrder())
            .append("isPrimary", getIsPrimary())
            .append("status", getStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 