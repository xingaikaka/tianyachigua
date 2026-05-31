package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.Comment;
import org.apache.ibatis.annotations.Param;

/**
 * 评论Mapper接口
 * 
 * @author chigua
 * @date 2025-01-22
 */
public interface CommentMapper 
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
     * 删除评论
     * 
     * @param id 评论主键
     * @return 结果
     */
    public int deleteCommentById(Long id);

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCommentByIds(Long[] ids);

    /**
     * 批量审核评论
     * 
     * @param ids 评论ID数组
     * @param status 审核状态
     * @param auditUserId 审核人ID
     * @param auditRemark 审核备注
     * @return 结果
     */
    public int batchAuditComments(@Param("ids") Long[] ids, 
                                 @Param("status") Integer status, 
                                 @Param("auditUserId") Long auditUserId, 
                                 @Param("auditRemark") String auditRemark);

        /**
     * 查询评论统计信息
     *
     * @param comment 查询条件
     * @return 统计信息
     */
    public Comment selectCommentStatistics(Comment comment);

    /**
     * 查询指定IP在指定时间内的评论数量
     *
     * @param ipAddress IP地址
     * @param minutes 时间间隔（分钟）
     * @return 评论数量
     */
    public int selectCommentCountByIpAndTime(@Param("ipAddress") String ipAddress, @Param("minutes") int minutes);

    /**
     * 根据视频ID查询评论列表（前端展示用）
     * 
     * @param videoId 视频ID
     * @param status 状态（1-已通过）
     * @return 评论集合
     */
    public List<Comment> selectCommentsByVideoId(@Param("videoId") Long videoId, @Param("status") Integer status);

    /**
     * 更新回复数量
     * 
     * @param parentId 父评论ID
     * @param increment 增量（+1或-1）
     * @return 结果
     */
    public int updateReplyCount(@Param("parentId") Long parentId, @Param("increment") Integer increment);
} 