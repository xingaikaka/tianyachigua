package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.Comment;

/**
 * 评论Service接口
 * 
 * @author chigua
 * @date 2025-01-22
 */
public interface ICommentService 
{
    /**
     * 查询评论
     * 
     * @param id 评论主键
     * @return 评论
     */
    public Comment selectCommentById(Long id);

    /**
     * 查询评论列表
     * 
     * @param comment 评论
     * @return 评论集合
     */
    public List<Comment> selectCommentList(Comment comment);

    /**
     * 查询待审核评论列表
     * 
     * @param comment 评论
     * @return 评论集合
     */
    public List<Comment> selectPendingCommentList(Comment comment);

    /**
     * 新增评论
     * 
     * @param comment 评论
     * @return 结果
     */
    public int insertComment(Comment comment);

    /**
     * 修改评论
     * 
     * @param comment 评论
     * @return 结果
     */
    public int updateComment(Comment comment);

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的评论主键集合
     * @return 结果
     */
    public int deleteCommentByIds(Long[] ids);

    /**
     * 删除评论信息
     * 
     * @param id 评论主键
     * @return 结果
     */
    public int deleteCommentById(Long id);

    /**
     * 审核评论
     * 
     * @param id 评论ID
     * @param status 审核状态
     * @param auditUserId 审核人ID
     * @param auditRemark 审核备注
     * @return 结果
     */
    public int auditComment(Long id, Integer status, Long auditUserId, String auditRemark);

    /**
     * 批量审核评论
     * 
     * @param ids 评论ID数组
     * @param status 审核状态
     * @param auditUserId 审核人ID
     * @param auditRemark 审核备注
     * @return 结果
     */
    public int batchAuditComments(Long[] ids, Integer status, Long auditUserId, String auditRemark);

        /**
     * 获取评论统计信息
     *
     * @param comment 查询条件
     * @return 统计信息
     */
    public Comment getCommentStatistics(Comment comment);

    /**
     * 检查IP是否在短时间内发表过评论
     *
     * @param ipAddress IP地址
     * @param minutes 时间间隔（分钟）
     * @return 是否超出限制
     */
    public boolean checkIpCommentLimit(String ipAddress, int minutes);

    /**
     * 置顶/取消置顶评论
     * 
     * @param id 评论ID
     * @param isSticky 是否置顶
     * @return 结果
     */
    public int updateCommentSticky(Long id, Integer isSticky);

    /**
     * 根据视频ID查询已通过的评论列表（前端展示用）
     * 
     * @param videoId 视频ID
     * @return 评论集合
     */
    public List<Comment> selectApprovedCommentsByVideoId(Long videoId);
} 