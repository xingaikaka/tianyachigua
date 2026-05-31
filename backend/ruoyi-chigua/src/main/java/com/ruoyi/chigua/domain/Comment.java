package com.ruoyi.chigua.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 评论对象 comments
 * 
 * @author chigua
 * @date 2025-01-22
 */
public class Comment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评论ID */
    private Long id;

    /** 视频ID */
    @Excel(name = "视频ID")
    private Long videoId;

    /** 评论类型（video-视频评论，submission-投稿评论） */
    @Excel(name = "评论类型", readConverterExp = "video=视频评论,submission=投稿评论")
    private String commentType;

    /** 父评论ID，NULL表示顶级评论 */
    @Excel(name = "父评论ID")
    private Long parentId;

    /** 用户名 */
    @Excel(name = "用户名")
    private String username;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 评论内容 */
    @Excel(name = "评论内容")
    private String content;

    /** IP地址 */
    @Excel(name = "IP地址")
    private String ipAddress;

    /** 用户代理 */
    @Excel(name = "用户代理")
    private String userAgent;

    /** 状态：0-待审核，1-已通过，2-已拒绝 */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=已通过,2=已拒绝")
    private Integer status;

    /** 回复数量 */
    @Excel(name = "回复数量")
    private Integer replyCount;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Integer likeCount;

    /** 是否置顶 */
    @Excel(name = "是否置顶", readConverterExp = "0=否,1=是")
    private Integer isSticky;

    /** 审核人ID */
    @Excel(name = "审核人ID")
    private Long auditUserId;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审核备注 */
    @Excel(name = "审核备注")
    private String auditRemark;

    /** 回复列表（用于树形结构，不存储到数据库） */
    private List<Comment> replies;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setVideoId(Long videoId) 
    {
        this.videoId = videoId;
    }

    public Long getVideoId() 
    {
        return videoId;
    }

    public void setCommentType(String commentType)
    {
        this.commentType = commentType;
    }

    public String getCommentType()
    {
        return commentType;
    }

    public void setParentId(Long parentId) 
    {
        this.parentId = parentId;
    }

    public Long getParentId() 
    {
        return parentId;
    }

    public void setUsername(String username) 
    {
        this.username = username;
    }

    public String getUsername() 
    {
        return username;
    }

    public void setEmail(String email) 
    {
        this.email = email;
    }

    public String getEmail() 
    {
        return email;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setIpAddress(String ipAddress) 
    {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() 
    {
        return ipAddress;
    }

    public void setUserAgent(String userAgent) 
    {
        this.userAgent = userAgent;
    }

    public String getUserAgent() 
    {
        return userAgent;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    public void setReplyCount(Integer replyCount) 
    {
        this.replyCount = replyCount;
    }

    public Integer getReplyCount() 
    {
        return replyCount;
    }

    public void setLikeCount(Integer likeCount) 
    {
        this.likeCount = likeCount;
    }

    public Integer getLikeCount() 
    {
        return likeCount;
    }

    public void setIsSticky(Integer isSticky) 
    {
        this.isSticky = isSticky;
    }

    public Integer getIsSticky() 
    {
        return isSticky;
    }

    public void setAuditUserId(Long auditUserId) 
    {
        this.auditUserId = auditUserId;
    }

    public Long getAuditUserId() 
    {
        return auditUserId;
    }

    public void setAuditTime(Date auditTime) 
    {
        this.auditTime = auditTime;
    }

    public Date getAuditTime() 
    {
        return auditTime;
    }

    public void setAuditRemark(String auditRemark) 
    {
        this.auditRemark = auditRemark;
    }

    public String getAuditRemark() 
    {
        return auditRemark;
    }

    public void setReplies(List<Comment> replies) 
    {
        this.replies = replies;
    }

    public List<Comment> getReplies() 
    {
        return replies;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", videoId=" + videoId +
                ", commentType='" + commentType + '\'' +
                ", parentId=" + parentId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", content='" + content + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", status=" + status +
                ", replyCount=" + replyCount +
                ", likeCount=" + likeCount +
                ", isSticky=" + isSticky +
                ", auditUserId=" + auditUserId +
                ", auditTime=" + auditTime +
                ", auditRemark='" + auditRemark + '\'' +
                '}';
    }
} 