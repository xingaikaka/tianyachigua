package com.ruoyi.chigua.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.ibatis.type.Alias;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 合集对象 collections
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Alias("ChiguaCollection")
public class Collection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 合集ID */
    private Long id;

    /** 合集标题 */
    @Excel(name = "合集标题")
    private String title;

    /** 合集描述 */
    @Excel(name = "合集描述")
    private String description;

    /** 合集封面图片URL */
    @Excel(name = "封面图片")
    private String coverImage;

    /** 合集作者 */
    @Excel(name = "合集作者")
    private String author;

    /** 所属分类ID */
    @Excel(name = "所属分类ID")
    private String categoryId;

    /** 观看次数 */
    @Excel(name = "观看次数")
    private Integer viewCount;

    /** 包含视频数量 */
    @Excel(name = "视频数量")
    private Integer videoCount;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

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

    /** 关联的分类列表（用于显示） */
    private List<Category> categories;

    /** 关联的视频列表（用于显示） */
    private List<Video> videos;

    /** 分类ID数组（用于前端多选） */
    private List<Long> categoryIds;

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

    @NotBlank(message = "合集标题不能为空")
    @Size(min = 0, max = 200, message = "合集标题长度不能超过200个字符")
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

    public void setCoverImage(String coverImage) 
    {
        this.coverImage = coverImage;
    }

    @Size(min = 0, max = 500, message = "封面图片URL长度不能超过500个字符")
    public String getCoverImage() 
    {
        return coverImage;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    @Size(min = 0, max = 100, message = "作者长度不能超过100个字符")
    public String getAuthor() 
    {
        return author;
    }

    public void setCategoryId(String categoryId) 
    {
        this.categoryId = categoryId;
    }

    @Size(min = 0, max = 50, message = "分类ID长度不能超过50个字符")
    public String getCategoryId() 
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

    public void setVideoCount(Integer videoCount) 
    {
        this.videoCount = videoCount;
    }

    public Integer getVideoCount() 
    {
        return videoCount;
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

    public List<Category> getCategories() 
    {
        return categories;
    }

    public void setCategories(List<Category> categories) 
    {
        this.categories = categories;
    }

    public List<Video> getVideos() 
    {
        return videos;
    }

    public void setVideos(List<Video> videos) 
    {
        this.videos = videos;
    }

    public List<Long> getCategoryIds() 
    {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) 
    {
        this.categoryIds = categoryIds;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("coverImage", getCoverImage())
            .append("author", getAuthor())
            .append("categoryId", getCategoryId())
            .append("viewCount", getViewCount())
            .append("videoCount", getVideoCount())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 