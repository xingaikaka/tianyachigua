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
 * 页面配置对象 page_config
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
public class PageConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 配置键值(唯一标识) */
    @Excel(name = "配置键值")
    @NotBlank(message = "配置键值不能为空")
    @Size(min = 0, max = 100, message = "配置键值长度不能超过100个字符")
    private String configKey;

    /** 配置名称 */
    @Excel(name = "配置名称")
    @NotBlank(message = "配置名称不能为空")
    @Size(min = 0, max = 200, message = "配置名称长度不能超过200个字符")
    private String configName;

    /** 配置类型(rich_text/basic_config) */
    @Excel(name = "配置类型", readConverterExp = "rich_text=页面富文本,basic_config=基础配置")
    @NotBlank(message = "配置类型不能为空")
    private String configType;

    /** 富文本内容 */
    @Excel(name = "富文本内容")
    private String richContent;

    /** 基础文本内容 */
    @Excel(name = "基础文本内容")
    private String basicContent;

    /** 跳转地址（用于分类更多等场景） */
    @Excel(name = "跳转地址")
    @Size(min = 0, max = 500, message = "跳转地址长度不能超过500个字符")
    private String jumpUrl;

    /** 配置分类 */
    @Excel(name = "配置分类", readConverterExp = "page_content=页面内容,site_config=站点配置")
    @NotBlank(message = "配置分类不能为空")
    private String configCategory;

    /** 状态(0正常 1停用) */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 显示顺序 */
    @Excel(name = "显示顺序")
    private Integer sortOrder;

    public void setConfigId(Long configId) 
    {
        this.configId = configId;
    }

    public Long getConfigId() 
    {
        return configId;
    }

    public void setConfigKey(String configKey) 
    {
        this.configKey = configKey;
    }

    public String getConfigKey() 
    {
        return configKey;
    }

    public void setConfigName(String configName) 
    {
        this.configName = configName;
    }

    public String getConfigName() 
    {
        return configName;
    }

    public void setConfigType(String configType) 
    {
        this.configType = configType;
    }

    public String getConfigType() 
    {
        return configType;
    }

    public void setRichContent(String richContent) 
    {
        this.richContent = richContent;
    }

    public String getRichContent() 
    {
        return richContent;
    }

    public void setBasicContent(String basicContent) 
    {
        this.basicContent = basicContent;
    }

    public String getBasicContent() 
    {
        return basicContent;
    }

    public void setJumpUrl(String jumpUrl)
    {
        this.jumpUrl = jumpUrl;
    }

    public String getJumpUrl()
    {
        return jumpUrl;
    }

    public void setConfigCategory(String configCategory) 
    {
        this.configCategory = configCategory;
    }

    public String getConfigCategory() 
    {
        return configCategory;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setSortOrder(Integer sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder() 
    {
        return sortOrder;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configKey", getConfigKey())
            .append("configName", getConfigName())
            .append("configType", getConfigType())
            .append("richContent", getRichContent())
            .append("basicContent", getBasicContent())
            .append("jumpUrl", getJumpUrl())
            .append("configCategory", getConfigCategory())
            .append("status", getStatus())
            .append("sortOrder", getSortOrder())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}