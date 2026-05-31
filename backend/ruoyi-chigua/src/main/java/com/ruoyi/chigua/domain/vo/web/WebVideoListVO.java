package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Web视频列表VO对象（优化版，仅包含列表页必需字段）
 * 
 * @author chigua
 * @date 2024-12-20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebVideoListVO
{
    /** 视频ID */
    private Long id;

    /** 视频标题 */
    private String title;

    /** 视频封面图片URL */
    private String coverImageUrl;

    /** 作者 */
    private String author;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date publishedAt;

    /** 分类名称 */
    private String categoryName;

    /** 所有分类列表 */
    private List<WebCategoryInfo> categories;

    /** 是否热门（1 是，0 否） */
    private Integer isHot;

    public WebVideoListVO() {}

    public WebVideoListVO(Long id, String title, String coverImageUrl, String author, Date publishedAt, String categoryName) {
        this.id = id;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.author = author;
        this.publishedAt = publishedAt;
        this.categoryName = categoryName;
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
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

    public Integer getIsHot() {
        return isHot;
    }

    public void setIsHot(Integer isHot) {
        this.isHot = isHot;
    }

    @Override
    public String toString() {
        return "WebVideoListVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", coverImageUrl='" + coverImageUrl + '\'' +
                ", author='" + author + '\'' +
                ", publishedAt=" + publishedAt +
                ", categoryName='" + categoryName + '\'' +
                ", categories=" + categories +
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
}
