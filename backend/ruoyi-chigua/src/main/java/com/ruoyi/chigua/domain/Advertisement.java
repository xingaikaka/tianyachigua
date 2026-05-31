package com.ruoyi.chigua.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 广告对象 advertisements
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class Advertisement extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 广告ID */
    private Long id;

    /** 广告标题 */
    @Excel(name = "广告标题")
    private String title;

    /** 广告描述 */
    @Excel(name = "广告描述")
    private String description;

    /** 广告类型 */
    @Excel(name = "广告类型", readConverterExp = "1=横幅广告,2=logo广告,3=文字链接广告,4=弹窗广告,5=短视频广告,6=分页模式广告")
    private String adType;

    /** 应用类型 */
    @Excel(name = "应用类型", readConverterExp = "1=热门应用,2=最新上架,3=必备精品")
    private String appType;

    /** 广告位置 */
    @Excel(name = "广告位置", readConverterExp = "1=列表顶部横幅,2=列表底部横幅,3=详情顶部横幅,4=详情底部横幅")
    private String position;

    /** 所属分类ID */
    @Excel(name = "所属分类ID")
    private String categoryId;

    /** 广告图片URL */
    @Excel(name = "广告图片URL")
    private String imageUrl;

    /** 图标名称 */
    @Excel(name = "图标名称")
    private String iconName;

    /** 点击跳转链接 */
    @Excel(name = "点击跳转链接")
    private String linkUrl;

    /** 链接文字 */
    @Excel(name = "链接文字")
    private String linkText;

    /** 点击次数统计 */
    @Excel(name = "点击次数统计")
    private Integer clickCount;

    /** 展示次数统计 */
    @Excel(name = "展示次数统计")
    private Integer impressionCount;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 投放开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "投放开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 投放结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "投放结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 状态 */
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

    /** 是否全站显示 */
    @Excel(name = "是否全站显示", readConverterExp = "1=全站显示,0=按分类显示")
    private Integer isGlobal;

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

    @NotBlank(message = "广告标题不能为空")
    @Size(min = 0, max = 200, message = "广告标题长度不能超过200个字符")
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

    public void setAdType(String adType) 
    {
        this.adType = adType;
    }

    @NotBlank(message = "广告类型不能为空")
    public String getAdType() 
    {
        return adType;
    }

    public void setAppType(String appType) 
    {
        this.appType = appType;
    }

    public String getAppType() 
    {
        return appType;
    }

    public void setPosition(String position) 
    {
        this.position = position;
    }

    public String getPosition() 
    {
        return position;
    }

    public void setCategoryId(String categoryId) 
    {
        this.categoryId = categoryId;
    }

    public String getCategoryId() 
    {
        return categoryId;
    }

    public void setImageUrl(String imageUrl) 
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() 
    {
        return imageUrl;
    }

    public void setIconName(String iconName) 
    {
        this.iconName = iconName;
    }

    public String getIconName() 
    {
        return iconName;
    }

    public void setLinkUrl(String linkUrl) 
    {
        this.linkUrl = linkUrl;
    }

    public String getLinkUrl() 
    {
        return linkUrl;
    }

    public void setLinkText(String linkText) 
    {
        this.linkText = linkText;
    }

    public String getLinkText() 
    {
        return linkText;
    }

    public void setClickCount(Integer clickCount) 
    {
        this.clickCount = clickCount;
    }

    public Integer getClickCount() 
    {
        return clickCount;
    }

    public void setImpressionCount(Integer impressionCount) 
    {
        this.impressionCount = impressionCount;
    }

    public Integer getImpressionCount() 
    {
        return impressionCount;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setStartDate(Date startDate) 
    {
        this.startDate = startDate;
    }

    public Date getStartDate() 
    {
        return startDate;
    }

    public void setEndDate(Date endDate) 
    {
        this.endDate = endDate;
    }

    public Date getEndDate() 
    {
        return endDate;
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

    public void setIsGlobal(Integer isGlobal) 
    {
        this.isGlobal = isGlobal;
    }

    public Integer getIsGlobal() 
    {
        return isGlobal;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("adType", getAdType())
            .append("appType", getAppType())
            .append("position", getPosition())
            .append("categoryId", getCategoryId())
            .append("imageUrl", getImageUrl())
            .append("iconName", getIconName())
            .append("linkUrl", getLinkUrl())
            .append("linkText", getLinkText())
            .append("clickCount", getClickCount())
            .append("impressionCount", getImpressionCount())
            .append("sortOrder", getSortOrder())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("status", getStatus())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .append("isGlobal", getIsGlobal())
            .toString();
    }
} 