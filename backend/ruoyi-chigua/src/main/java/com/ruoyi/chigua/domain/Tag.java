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
 * 标签对象 tags
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class Tag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    @Excel(name = "标签名称")
    private String name;

    /** 标签颜色 */
    @Excel(name = "标签颜色")
    private String color;

    /** 使用次数统计 */
    @Excel(name = "使用次数")
    private Integer usageCount;

    /** 状态（1启用 0禁用） */
    @Excel(name = "状态", readConverterExp = "1=启用,0=禁用")
    private Integer status;

    /** 是否热点标签（1是 0否） */
    @Excel(name = "是否热点标签", readConverterExp = "1=是,0=否")
    private Integer isHot;

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

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 0, max = 512, message = "标签名称长度不能超过512个字符")
    public String getName() 
    {
        return name;
    }

    public void setColor(String color) 
    {
        this.color = color;
    }

    @Size(min = 0, max = 7, message = "标签颜色长度不能超过7个字符")
    public String getColor() 
    {
        return color;
    }

    public void setUsageCount(Integer usageCount) 
    {
        this.usageCount = usageCount;
    }

    public Integer getUsageCount() 
    {
        return usageCount;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setIsHot(Integer isHot) 
    {
        this.isHot = isHot;
    }

    public Integer getIsHot() 
    {
        return isHot;
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
            .append("color", getColor())
            .append("usageCount", getUsageCount())
            .append("status", getStatus())
            .append("isHot", getIsHot())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
} 