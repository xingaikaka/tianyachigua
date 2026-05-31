package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Web广告展示VO
 * 
 * @author ruoyi
 * @date 2025-01-21
 */
public class WebAdvertisementVO
{
    /** 广告ID */
    private Long id;

    /** 广告标题 */
    private String title;

    /** 广告描述 */
    private String description;

    /** 广告图片URL */
    private String imageUrl;

    /** 图标名称 */
    private String iconName;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序权重 */
    private Integer sortOrder;

    /** 点击次数统计 */
    private Integer clickCount;

    /** 展示次数统计 */
    private Integer impressionCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Integer getImpressionCount() {
        return impressionCount;
    }

    public void setImpressionCount(Integer impressionCount) {
        this.impressionCount = impressionCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "WebAdvertisementVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", linkUrl='" + linkUrl + '\'' +
                ", sortOrder=" + sortOrder +
                ", clickCount=" + clickCount +
                ", impressionCount=" + impressionCount +
                ", createTime=" + createTime +
                '}';
    }
} 