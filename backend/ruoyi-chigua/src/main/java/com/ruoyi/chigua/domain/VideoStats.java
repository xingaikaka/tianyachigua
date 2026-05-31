package com.ruoyi.chigua.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 视频统计对象 video_statistics
 * 
 * @author ruoyi
 * @date 2024-01-15
 */
public class VideoStats extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 统计ID */
    private Long id;

    /** 视频ID */
    @Excel(name = "视频ID")
    private Long videoId;

    /** 浏览量（详情页访问） */
    @Excel(name = "浏览量")
    private Integer viewCount;

    /** 播放量（视频播放） */
    @Excel(name = "播放量")
    private Integer playCount;

    /** 点赞数量 */
    @Excel(name = "点赞数量")
    private Integer likeCount;

    /** 评论数量 */
    @Excel(name = "评论数量")
    private Integer commentCount;

    /** 分享次数 */
    @Excel(name = "分享次数")
    private Integer shareCount;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

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

    public Long getVideoId() 
    {
        return videoId;
    }

    public void setViewCount(Integer viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Integer getViewCount() 
    {
        return viewCount;
    }

    public void setPlayCount(Integer playCount) 
    {
        this.playCount = playCount;
    }

    public Integer getPlayCount() 
    {
        return playCount;
    }

    public void setLikeCount(Integer likeCount) 
    {
        this.likeCount = likeCount;
    }

    public Integer getLikeCount() 
    {
        return likeCount;
    }

    public void setCommentCount(Integer commentCount) 
    {
        this.commentCount = commentCount;
    }

    public Integer getCommentCount() 
    {
        return commentCount;
    }

    public void setShareCount(Integer shareCount) 
    {
        this.shareCount = shareCount;
    }

    public Integer getShareCount() 
    {
        return shareCount;
    }

    public void setUpdatedAt(Date updatedAt) 
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() 
    {
        return updatedAt;
    }

    public void setCreatedAt(Date createdAt) 
    {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() 
    {
        return createdAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("videoId", getVideoId())
            .append("viewCount", getViewCount())
            .append("playCount", getPlayCount())
            .append("likeCount", getLikeCount())
            .append("commentCount", getCommentCount())
            .append("shareCount", getShareCount())
            .append("updatedAt", getUpdatedAt())
            .append("createdAt", getCreatedAt())
            .toString();
    }
}