package com.ruoyi.chigua.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotNull;

/**
 * 广告统计对象 ad_statistics
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
public class AdStatistics extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 统计记录ID */
    private Long id;

    /** 广告ID */
    @Excel(name = "广告ID")
    private Long adId;

    /** 广告标题（用户标识） */
    @Excel(name = "广告标题")
    private String adTitle;

    /** 广告类型 */
    @Excel(name = "广告类型", readConverterExp = "1=横幅广告,2=logo广告,3=文字链接广告,4=弹窗广告")
    private String adType;

    /** 广告位置 */
    @Excel(name = "广告位置", readConverterExp = "1=列表顶部横幅,2=列表底部横幅,3=详情顶部横幅,4=详情底部横幅")
    private String position;

    /** 统计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "统计日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date statDate;

    /** 当日点击次数 */
    @Excel(name = "点击次数")
    private Integer clickCount;

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

    public void setAdId(Long adId) 
    {
        this.adId = adId;
    }

    @NotNull(message = "广告ID不能为空")
    public Long getAdId() 
    {
        return adId;
    }

    public void setAdTitle(String adTitle) 
    {
        this.adTitle = adTitle;
    }

    @NotNull(message = "广告标题不能为空")
    public String getAdTitle() 
    {
        return adTitle;
    }

    public void setAdType(String adType) 
    {
        this.adType = adType;
    }

    public String getAdType() 
    {
        return adType;
    }

    public void setPosition(String position) 
    {
        this.position = position;
    }

    public String getPosition() 
    {
        return position;
    }

    public void setStatDate(Date statDate) 
    {
        this.statDate = statDate;
    }

    @NotNull(message = "统计日期不能为空")
    public Date getStatDate() 
    {
        return statDate;
    }

    public void setClickCount(Integer clickCount) 
    {
        this.clickCount = clickCount;
    }

    public Integer getClickCount() 
    {
        return clickCount;
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
            .append("adId", getAdId())
            .append("adTitle", getAdTitle())
            .append("adType", getAdType())
            .append("position", getPosition())
            .append("statDate", getStatDate())
            .append("clickCount", getClickCount())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}
