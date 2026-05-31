package com.ruoyi.chigua.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * RedGifs视频信息对象 redgifs_videos
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public class RedgifsVideo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** RedGifs视频ID（唯一标识） */
    @Excel(name = "RedGifs视频ID")
    @NotBlank(message = "RedGifs视频ID不能为空")
    @Size(min = 0, max = 100, message = "RedGifs视频ID长度不能超过100个字符")
    private String gifId;

    /** 所属用户ID（关联redgifs_users.id） */
    @Excel(name = "所属用户ID")
    private Integer userId;

    /** 视频标题（自动生成） */
    @Excel(name = "视频标题")
    @Size(min = 0, max = 300, message = "视频标题长度不能超过300个字符")
    private String title;

    /** 视频描述 */
    @Excel(name = "视频描述")
    private String description;

    /** 视频时长（秒） */
    @Excel(name = "视频时长")
    private BigDecimal duration;

    /** 视频宽度 */
    @Excel(name = "视频宽度")
    private Integer width;

    /** 视频高度 */
    @Excel(name = "视频高度")
    private Integer height;

    /** 分辨率（如：1920x1080） */
    @Excel(name = "分辨率")
    @Size(min = 0, max = 20, message = "分辨率长度不能超过20个字符")
    private String resolution;

    /** 是否有音频 */
    @Excel(name = "是否有音频", readConverterExp = "1=是,0=否")
    private Integer hasAudio;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Integer likes;

    /** 观看数 */
    @Excel(name = "观看数")
    private Integer views;

    /** 是否认证 */
    @Excel(name = "是否认证", readConverterExp = "1=是,0=否")
    private Integer verified;

    /** 标签（JSON数组） */
    @Excel(name = "标签")
    private String tags;

    /** 类型（JSON数组） */
    @Excel(name = "类型")
    private String sexuality;

    /** 分类（JSON数组） */
    @Excel(name = "分类")
    private String niches;

    /** 高清MP4 URL（R2相对路径） */
    @Excel(name = "高清MP4 URL")
    @Size(min = 0, max = 500, message = "高清MP4 URL长度不能超过500个字符")
    private String hdUrl;

    /** 标清MP4 URL（R2相对路径） */
    @Excel(name = "标清MP4 URL")
    @Size(min = 0, max = 500, message = "标清MP4 URL长度不能超过500个字符")
    private String sdUrl;

    /** 封面图URL（R2相对路径） */
    @Excel(name = "封面图URL")
    @Size(min = 0, max = 500, message = "封面图URL长度不能超过500个字符")
    private String posterUrl;

    /** 缩略图URL（R2相对路径） */
    @Excel(name = "缩略图URL")
    @Size(min = 0, max = 500, message = "缩略图URL长度不能超过500个字符")
    private String thumbnailUrl;

    /** RedGifs原始URL */
    @Excel(name = "RedGifs原始URL")
    @Size(min = 0, max = 500, message = "RedGifs原始URL长度不能超过500个字符")
    private String redgifsUrl;

    /** RedGifs创建时间（时间戳） */
    @Excel(name = "RedGifs创建时间")
    private Long createDate;

    /** 状态：1-已发布，0-草稿 */
    @Excel(name = "状态", readConverterExp = "1=已发布,0=草稿")
    private Integer status;

    /** 同步状态：0-未同步，1-同步中，2-已同步 */
    @Excel(name = "同步状态", readConverterExp = "0=未同步,1=同步中,2=已同步")
    private Integer syncStatus;

    /** 入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "入库时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setGifId(String gifId) 
    {
        this.gifId = gifId;
    }

    public String getGifId() 
    {
        return gifId;
    }

    public void setUserId(Integer userId) 
    {
        this.userId = userId;
    }

    public Integer getUserId() 
    {
        return userId;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

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

    public void setDuration(BigDecimal duration) 
    {
        this.duration = duration;
    }

    public BigDecimal getDuration() 
    {
        return duration;
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

    public void setResolution(String resolution) 
    {
        this.resolution = resolution;
    }

    public String getResolution() 
    {
        return resolution;
    }

    public void setHasAudio(Integer hasAudio) 
    {
        this.hasAudio = hasAudio;
    }

    public Integer getHasAudio() 
    {
        return hasAudio;
    }

    public void setLikes(Integer likes) 
    {
        this.likes = likes;
    }

    public Integer getLikes() 
    {
        return likes;
    }

    public void setViews(Integer views) 
    {
        this.views = views;
    }

    public Integer getViews() 
    {
        return views;
    }

    public void setVerified(Integer verified) 
    {
        this.verified = verified;
    }

    public Integer getVerified() 
    {
        return verified;
    }

    public void setTags(String tags) 
    {
        this.tags = tags;
    }

    public String getTags() 
    {
        return tags;
    }

    public void setSexuality(String sexuality) 
    {
        this.sexuality = sexuality;
    }

    public String getSexuality() 
    {
        return sexuality;
    }

    public void setNiches(String niches) 
    {
        this.niches = niches;
    }

    public String getNiches() 
    {
        return niches;
    }

    public void setHdUrl(String hdUrl) 
    {
        this.hdUrl = hdUrl;
    }

    public String getHdUrl() 
    {
        return hdUrl;
    }

    public void setSdUrl(String sdUrl) 
    {
        this.sdUrl = sdUrl;
    }

    public String getSdUrl() 
    {
        return sdUrl;
    }

    public void setPosterUrl(String posterUrl) 
    {
        this.posterUrl = posterUrl;
    }

    public String getPosterUrl() 
    {
        return posterUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) 
    {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnailUrl() 
    {
        return thumbnailUrl;
    }

    public void setRedgifsUrl(String redgifsUrl) 
    {
        this.redgifsUrl = redgifsUrl;
    }

    public String getRedgifsUrl() 
    {
        return redgifsUrl;
    }

    public void setCreateDate(Long createDate) 
    {
        this.createDate = createDate;
    }

    public Long getCreateDate() 
    {
        return createDate;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setSyncStatus(Integer syncStatus) 
    {
        this.syncStatus = syncStatus;
    }

    public Integer getSyncStatus() 
    {
        return syncStatus;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("gifId", getGifId())
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("duration", getDuration())
            .append("width", getWidth())
            .append("height", getHeight())
            .append("resolution", getResolution())
            .append("hasAudio", getHasAudio())
            .append("likes", getLikes())
            .append("views", getViews())
            .append("verified", getVerified())
            .append("tags", getTags())
            .append("sexuality", getSexuality())
            .append("niches", getNiches())
            .append("hdUrl", getHdUrl())
            .append("sdUrl", getSdUrl())
            .append("posterUrl", getPosterUrl())
            .append("thumbnailUrl", getThumbnailUrl())
            .append("redgifsUrl", getRedgifsUrl())
            .append("createDate", getCreateDate())
            .append("status", getStatus())
            .append("syncStatus", getSyncStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}
