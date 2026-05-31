package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Web分类VO对象
 * 
 * @author chigua
 * @date 2024-12-20
 */
public class WebCategoryVO
{
    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String categoryName;

    /** 分类图标 */
    private String icon;

    /** 分类描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 视频数量 */
    private Integer videoCount;

    /** 是否推荐（1推荐 0否） */
    private Integer isRecommended;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public Integer getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Integer isRecommended) {
        this.isRecommended = isRecommended;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "WebCategoryVO{" +
                "id=" + id +
                ", categoryName='" + categoryName + '\'' +
                ", icon='" + icon + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", videoCount=" + videoCount +
                ", isRecommended=" + isRecommended +
                ", createTime=" + createTime +
                '}';
    }
} 