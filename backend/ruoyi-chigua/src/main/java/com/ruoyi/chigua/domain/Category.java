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
 * 分类对象 categories
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class Category extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String name;

    /** 分类描述 */
    @Excel(name = "分类描述")
    private String description;

    /** 排序权重 */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 状态（1启用 0禁用） */
    @Excel(name = "状态", readConverterExp = "1=启用,0=禁用")
    private Integer status;

    /** 广告显示模式（1集中显示 2交替显示 3不显示广告） */
    @Excel(name = "广告显示模式", readConverterExp = "1=集中显示,2=交替显示,3=不显示广告")
    private Integer adDisplayMode;

    /** 广告显示间隔 */
    @Excel(name = "广告显示间隔")
    private Integer adInterval;

    /** 是否视频合集（1是 0否） */
    @Excel(name = "是否视频合集", readConverterExp = "1=是,0=否")
    private Integer isCollection;

    /** 是否推荐（1推荐 0否） */
    @Excel(name = "是否推荐", readConverterExp = "1=推荐,0=否")
    private Integer isRecommended;

    /** 是否短视频（1是 0否） */
    @Excel(name = "是否短视频", readConverterExp = "1=是,0=否")
    private Integer isShort;

    /** 是否分页模式（1是 0否） */
    @Excel(name = "是否分页模式", readConverterExp = "1=是,0=否")
    private Integer isPagination;

    /** 是否用户组（1是 0否） */
    @Excel(name = "是否用户组", readConverterExp = "1=是,0=否")
    private Integer isUserGroup;

    /** 是否Telegram模式（1是 0否） */
    @Excel(name = "是否Telegram模式", readConverterExp = "1=是,0=否")
    private Integer isTelegram;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
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

    public void setName(String name) 
    {
        this.name = name;
    }

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 0, max = 100, message = "分类名称长度不能超过100个字符")
    public String getName() 
    {
        return name;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setAdDisplayMode(Integer adDisplayMode) 
    {
        this.adDisplayMode = adDisplayMode;
    }

    public Integer getAdDisplayMode() 
    {
        return adDisplayMode;
    }

    public void setAdInterval(Integer adInterval) 
    {
        this.adInterval = adInterval;
    }

    public Integer getAdInterval() 
    {
        return adInterval;
    }

    public void setIsCollection(Integer isCollection) 
    {
        this.isCollection = isCollection;
    }

    public Integer getIsCollection() 
    {
        return isCollection;
    }

    public void setIsRecommended(Integer isRecommended)
    {
        this.isRecommended = isRecommended;
    }

    public Integer getIsRecommended()
    {
        return isRecommended;
    }

    public void setIsShort(Integer isShort)
    {
        this.isShort = isShort;
    }

    public Integer getIsShort()
    {
        return isShort;
    }

    public void setIsPagination(Integer isPagination)
    {
        this.isPagination = isPagination;
    }

    public Integer getIsPagination()
    {
        return isPagination;
    }

    public void setIsUserGroup(Integer isUserGroup)
    {
        this.isUserGroup = isUserGroup;
    }

    public Integer getIsUserGroup()
    {
        return isUserGroup;
    }

    public void setIsTelegram(Integer isTelegram)
    {
        this.isTelegram = isTelegram;
    }

    public Integer getIsTelegram()
    {
        return isTelegram;
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
            .append("name", getName())
            .append("description", getDescription())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("adDisplayMode", getAdDisplayMode())
            .append("adInterval", getAdInterval())
            .append("isCollection", getIsCollection())
            .append("isRecommended", getIsRecommended())
            .append("isShort", getIsShort())
            .append("isPagination", getIsPagination())
            .append("isUserGroup", getIsUserGroup())
            .append("isTelegram", getIsTelegram())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 