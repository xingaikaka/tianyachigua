package com.ruoyi.chigua.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * SEO关键词对象 seo_keywords
 * 
 * @author ruoyi
 * @date 2026-01-14
 */
public class SeoKeyword extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 关键词ID */
    private Long id;

    /** 关键词内容 */
    private String keyword;

    /** SEO标题 */
    private String seoTitle;

    /** SEO描述 */
    private String seoDescription;

    /** 状态（1启用 0禁用） */
    private Integer status;

    /** SEO权重 */
    private BigDecimal priority;

    /** 搜索次数统计 */
    private Integer searchCount;

    /** 点击次数统计 */
    private Integer clickCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public void setPriority(BigDecimal priority) {
        this.priority = priority;
    }

    public BigDecimal getPriority() {
        return priority;
    }

    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("keyword", getKeyword())
            .append("seoTitle", getSeoTitle())
            .append("seoDescription", getSeoDescription())
            .append("status", getStatus())
            .append("priority", getPriority())
            .append("searchCount", getSearchCount())
            .append("clickCount", getClickCount())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}

