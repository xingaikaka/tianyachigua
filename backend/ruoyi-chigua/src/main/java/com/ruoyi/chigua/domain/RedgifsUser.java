package com.ruoyi.chigua.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * RedGifs用户信息对象 redgifs_users
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public class RedgifsUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户名（唯一标识） */
    @Excel(name = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 0, max = 100, message = "用户名长度不能超过100个字符")
    private String username;

    /** 显示名称 */
    @Excel(name = "显示名称")
    @Size(min = 0, max = 200, message = "显示名称长度不能超过200个字符")
    private String name;

    /** 用户描述 */
    @Excel(name = "用户描述")
    private String description;

    /** 头像URL（R2相对路径） */
    @Excel(name = "头像URL")
    @Size(min = 0, max = 500, message = "头像URL长度不能超过500个字符")
    private String profileImageUrl;

    /** 粉丝数 */
    @Excel(name = "粉丝数")
    private Integer followers;

    /** 关注数 */
    @Excel(name = "关注数")
    private Integer following;

    /** 总视频数 */
    @Excel(name = "总视频数")
    private Integer gifsCount;

    /** 已发布视频数 */
    @Excel(name = "已发布视频数")
    private Integer publishedGifsCount;

    /** 总观看次数 */
    @Excel(name = "总观看次数")
    private Long views;

    /** 是否认证 */
    @Excel(name = "是否认证", readConverterExp = "1=是,0=否")
    private Integer verified;

    /** RedGifs主页URL */
    @Excel(name = "RedGifs主页URL")
    @Size(min = 0, max = 500, message = "RedGifs主页URL长度不能超过500个字符")
    private String profileUrl;

    /** 账号创建时间（时间戳） */
    @Excel(name = "账号创建时间")
    private Long creationTime;

    /** 状态：1-启用，0-禁用 */
    @Excel(name = "状态", readConverterExp = "1=启用,0=禁用")
    private Integer status;

    /** 是否推荐：0-否，1-是 */
    @Excel(name = "是否推荐", readConverterExp = "1=是,0=否")
    private Integer recommended;

    /** 排序权重（推荐用户排序，数值越小越靠前） */
    @Excel(name = "排序权重")
    private Integer sortOrder;

    /** 同步状态：0-未同步，1-同步中，2-已同步 */
    @Excel(name = "同步状态", readConverterExp = "0=未同步,1=同步中,2=已同步")
    private Integer syncStatus;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后同步时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncAt;

    /** 入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "入库时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 列表排序列（请求参数：sortOrder、createdAt，白名单由 Mapper 校验） */
    private String sortColumn;

    /** 列表排序方向（请求参数：asc / desc） */
    private String sortDirection;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUsername(String username) 
    {
        this.username = username;
    }

    public String getUsername() 
    {
        return username;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

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

    public void setProfileImageUrl(String profileImageUrl) 
    {
        this.profileImageUrl = profileImageUrl;
    }

    public String getProfileImageUrl() 
    {
        return profileImageUrl;
    }

    public void setFollowers(Integer followers) 
    {
        this.followers = followers;
    }

    public Integer getFollowers() 
    {
        return followers;
    }

    public void setFollowing(Integer following) 
    {
        this.following = following;
    }

    public Integer getFollowing() 
    {
        return following;
    }

    public void setGifsCount(Integer gifsCount) 
    {
        this.gifsCount = gifsCount;
    }

    public Integer getGifsCount() 
    {
        return gifsCount;
    }

    public void setPublishedGifsCount(Integer publishedGifsCount) 
    {
        this.publishedGifsCount = publishedGifsCount;
    }

    public Integer getPublishedGifsCount() 
    {
        return publishedGifsCount;
    }

    public void setViews(Long views) 
    {
        this.views = views;
    }

    public Long getViews() 
    {
        return views;
    }

    public void setVerified(Integer verified) 
    {
        this.verified = verified;
    }

    public Integer getVerified() 
    {
        return verified;
    }

    public void setProfileUrl(String profileUrl) 
    {
        this.profileUrl = profileUrl;
    }

    public String getProfileUrl() 
    {
        return profileUrl;
    }

    public void setCreationTime(Long creationTime) 
    {
        this.creationTime = creationTime;
    }

    public Long getCreationTime() 
    {
        return creationTime;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setRecommended(Integer recommended)
    {
        this.recommended = recommended;
    }

    public Integer getRecommended()
    {
        return recommended;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSyncStatus(Integer syncStatus) 
    {
        this.syncStatus = syncStatus;
    }

    public Integer getSyncStatus() 
    {
        return syncStatus;
    }

    public void setLastSyncAt(Date lastSyncAt) 
    {
        this.lastSyncAt = lastSyncAt;
    }

    public Date getLastSyncAt() 
    {
        return lastSyncAt;
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

    public String getSortColumn()
    {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn)
    {
        this.sortColumn = sortColumn;
    }

    public String getSortDirection()
    {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection)
    {
        this.sortDirection = sortDirection;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("username", getUsername())
            .append("name", getName())
            .append("description", getDescription())
            .append("profileImageUrl", getProfileImageUrl())
            .append("followers", getFollowers())
            .append("following", getFollowing())
            .append("gifsCount", getGifsCount())
            .append("publishedGifsCount", getPublishedGifsCount())
            .append("views", getViews())
            .append("verified", getVerified())
            .append("profileUrl", getProfileUrl())
            .append("creationTime", getCreationTime())
            .append("status", getStatus())
            .append("recommended", getRecommended())
            .append("sortOrder", getSortOrder())
            .append("syncStatus", getSyncStatus())
            .append("lastSyncAt", getLastSyncAt())
            .append("createdAt", getCreatedAt())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}
