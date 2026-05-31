package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Web视频VO对象
 * 
 * @author chigua
 * @date 2024-12-20
 */
public class WebVideoVO
{
    /** 视频ID */
    private Long id;

    /** 视频标题 */
    private String title;

    /** 视频描述 */
    private String description;

    /** 视频富文本内容 */
    private String videoContent;

    /** 视频封面图片URL */
    private String coverImageUrl;

    /** 视频时长(秒) */
    private Integer duration;

    /** 观看次数 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 作者 */
    private String author;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 所有分类列表 */
    private List<WebCategoryInfo> categories;

    /** 所有标签列表 */
    private List<WebTagInfo> tags;

    /** 视频状态 */
    private Integer status;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date publishedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 上一篇视频信息 */
    private AdjacentVideoInfo previousVideo;

    /** 下一篇视频信息 */
    private AdjacentVideoInfo nextVideo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoContent() {
        return videoContent;
    }

    public void setVideoContent(String videoContent) {
        this.videoContent = videoContent;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<WebCategoryInfo> getCategories() {
        return categories;
    }

    public void setCategories(List<WebCategoryInfo> categories) {
        this.categories = categories;
    }

    public List<WebTagInfo> getTags() {
        return tags;
    }

    public void setTags(List<WebTagInfo> tags) {
        this.tags = tags;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public AdjacentVideoInfo getPreviousVideo() {
        return previousVideo;
    }

    public void setPreviousVideo(AdjacentVideoInfo previousVideo) {
        this.previousVideo = previousVideo;
    }

    public AdjacentVideoInfo getNextVideo() {
        return nextVideo;
    }

    public void setNextVideo(AdjacentVideoInfo nextVideo) {
        this.nextVideo = nextVideo;
    }

    @Override
    public String toString() {
        return "WebVideoVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", coverImageUrl='" + coverImageUrl + '\'' +
                ", duration=" + duration +
                ", viewCount=" + viewCount +
                ", likeCount=" + likeCount +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

    /**
     * 分类信息
     */
    public static class WebCategoryInfo {
        private Long id;
        private String name;

        public WebCategoryInfo() {}

        public WebCategoryInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * 标签信息
     */
    public static class WebTagInfo {
        private Long id;
        private String name;
        private String color;

        public WebTagInfo() {}

        public WebTagInfo(Long id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    /**
     * 相邻视频信息VO（用于上一篇、下一篇）
     */
    public static class AdjacentVideoInfo {
        /** 视频ID */
        private Long id;
        
        /** 视频标题 */
        private String title;
        
        public AdjacentVideoInfo() {}
        
        public AdjacentVideoInfo(Long id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }
    
    /**
     * 相邻视频数据VO
     */
    public static class AdjacentVideosVO {
        /** 上一篇视频 */
        private AdjacentVideoInfo previous;
        
        /** 下一篇视频 */
        private AdjacentVideoInfo next;
        
        public AdjacentVideosVO() {}
        
        public AdjacentVideosVO(AdjacentVideoInfo previous, AdjacentVideoInfo next) {
            this.previous = previous;
            this.next = next;
        }
        
        public AdjacentVideoInfo getPrevious() {
            return previous;
        }
        
        public void setPrevious(AdjacentVideoInfo previous) {
            this.previous = previous;
        }
        
        public AdjacentVideoInfo getNext() {
            return next;
        }
        
        public void setNext(AdjacentVideoInfo next) {
            this.next = next;
        }
    }
} 