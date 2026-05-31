package com.ruoyi.chigua.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotNull;

/**
 * 视频合集关系对象 video_collections
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class VideoCollection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关系ID */
    private Long id;

    /** 合集ID */
    @Excel(name = "合集ID")
    private Long collectionId;

    /** 视频ID */
    @Excel(name = "视频ID")
    private Long videoId;

    /** 在合集中的排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 关联的合集信息（用于显示） */
    private Collection collection;

    /** 关联的视频信息（用于显示） */
    private Video video;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setCollectionId(Long collectionId) 
    {
        this.collectionId = collectionId;
    }

    @NotNull(message = "合集ID不能为空")
    public Long getCollectionId() 
    {
        return collectionId;
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

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }

    public Collection getCollection() 
    {
        return collection;
    }

    public void setCollection(Collection collection) 
    {
        this.collection = collection;
    }

    public Video getVideo() 
    {
        return video;
    }

    public void setVideo(Video video) 
    {
        this.video = video;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("collectionId", getCollectionId())
            .append("videoId", getVideoId())
            .append("sortOrder", getSortOrder())
            .append("createdAt", getCreatedAt())
            .toString();
    }
} 