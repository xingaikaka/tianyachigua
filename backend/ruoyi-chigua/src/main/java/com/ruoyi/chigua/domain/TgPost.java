package com.ruoyi.chigua.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 帖子对象 tg_posts（一个媒体组 = 一条帖子）
 */
public class TgPost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "关联分类ID")
    private Integer categoryId;

    @Excel(name = "同步源原始ID")
    private String sourceId;

    @Excel(name = "帖子正文")
    private String caption;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发帖时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date postDate;

    @Excel(name = "媒体总数")
    private Integer mediaCount;

    @Excel(name = "图片数量")
    private Integer photoCount;

    @Excel(name = "视频数量")
    private Integer videoCount;

    @Excel(name = "浏览次数")
    private Integer views;

    @Excel(name = "排序权重")
    private Integer sortOrder;

    @Excel(name = "状态", readConverterExp = "1=显示,0=隐藏")
    private Integer status;

    @Excel(name = "是否置顶", readConverterExp = "1=是,0=否")
    private Integer isTop;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 关联的媒体列表（查询时填充） */
    private List<TgMedia> mediaList;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Date getPostDate() { return postDate; }
    public void setPostDate(Date postDate) { this.postDate = postDate; }

    public Integer getMediaCount() { return mediaCount; }
    public void setMediaCount(Integer mediaCount) { this.mediaCount = mediaCount; }

    public Integer getPhotoCount() { return photoCount; }
    public void setPhotoCount(Integer photoCount) { this.photoCount = photoCount; }

    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getIsTop() { return isTop; }
    public void setIsTop(Integer isTop) { this.isTop = isTop; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public List<TgMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<TgMedia> mediaList) { this.mediaList = mediaList; }
}
