package com.ruoyi.chigua.service.impl;

import java.util.List;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.mapper.CommentMapper;
import com.ruoyi.chigua.domain.Comment;
import com.ruoyi.chigua.service.ICommentService;
import com.ruoyi.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import com.ruoyi.chigua.service.CacheRefreshService;

/**
 * 评论Service业务层处理
 * 
 * @author chigua
 * @date 2025-01-22
 */
@Service
public class CommentServiceImpl implements ICommentService 
{
    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 查询评论
     * 
     * @param id 评论主键
     * @return 评论
     */
    @Override
    public Comment selectCommentById(Long id)
    {
        return commentMapper.selectCommentById(id);
    }

    /**
     * 查询评论列表
     * 
     * @param comment 评论
     * @return 评论
     */
    @Override
    public List<Comment> selectCommentList(Comment comment)
    {
        return commentMapper.selectCommentList(comment);
    }

    /**
     * 查询待审核评论列表
     * 
     * @param comment 评论
     * @return 评论
     */
    @Override
    public List<Comment> selectPendingCommentList(Comment comment)
    {
        return commentMapper.selectPendingCommentList(comment);
    }

    /**
     * 新增评论
     * 
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int insertComment(Comment comment)
    {
        logger.info("🆕 新增评论: videoId={}, username={}, content={}", 
            comment.getVideoId(), comment.getUsername(), comment.getContent());
            
        comment.setCreateTime(DateUtils.getNowDate());
        comment.setStatus(0); // 默认待审核状态
        
        int result = commentMapper.insertComment(comment);
        
        // 如果是回复，更新父评论的回复数量
        if (comment.getParentId() != null) {
            commentMapper.updateReplyCount(comment.getParentId(), 1);
            logger.info("📈 更新父评论回复数量: parentId={}", comment.getParentId());
        }
        
        // 刷新评论缓存
        cacheRefreshService.refreshCommentCache();
        
        logger.info("✅ 新增评论成功: commentId={}", comment.getId());
        return result;
    }

    /**
     * 修改评论
     * 
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int updateComment(Comment comment)
    {
        comment.setUpdateTime(DateUtils.getNowDate());
        return commentMapper.updateComment(comment);
    }

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentByIds(Long[] ids)
    {
        logger.info("🗑️ 批量删除评论: ids={}", (Object) ids);
        
        // 删除前需要更新相关的回复数量
        for (Long id : ids) {
            Comment comment = commentMapper.selectCommentById(id);
            if (comment != null && comment.getParentId() != null) {
                commentMapper.updateReplyCount(comment.getParentId(), -1);
            }
        }
        
        int result = commentMapper.deleteCommentByIds(ids);
        logger.info("✅ 批量删除评论完成: 删除数量={}", result);
        return result;
    }

    /**
     * 删除评论信息
     * 
     * @param id 评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentById(Long id)
    {
        logger.info("🗑️ 删除评论: id={}", id);
        
        Comment comment = commentMapper.selectCommentById(id);
        if (comment != null && comment.getParentId() != null) {
            commentMapper.updateReplyCount(comment.getParentId(), -1);
        }
        
        int result = commentMapper.deleteCommentById(id);
        logger.info("✅ 删除评论完成: id={}", id);
        return result;
    }

    /**
     * 审核评论
     * 
     * @param id 评论ID
     * @param status 审核状态
     * @param auditUserId 审核人ID
     * @param auditRemark 审核备注
     * @return 结果
     */
    @Override
    public int auditComment(Long id, Integer status, Long auditUserId, String auditRemark)
    {
        logger.info("⚖️ 审核评论: id={}, status={}, auditUserId={}", id, status, auditUserId);
        
        Comment comment = new Comment();
        comment.setId(id);
        comment.setStatus(status);
        comment.setAuditUserId(auditUserId);
        comment.setAuditTime(new Date());
        comment.setAuditRemark(auditRemark);
        
        int result = commentMapper.updateComment(comment);
        
        // 审核状态变更需要刷新缓存
        if (result > 0) {
            cacheRefreshService.refreshCommentCache();
        }
        
        String statusText = status == 1 ? "通过" : "拒绝";
        logger.info("✅ 评论审核完成: id={}, 审核结果={}", id, statusText);
        
        return result;
    }

    /**
     * 批量审核评论
     * 
     * @param ids 评论ID数组
     * @param status 审核状态
     * @param auditUserId 审核人ID
     * @param auditRemark 审核备注
     * @return 结果
     */
    @Override
    public int batchAuditComments(Long[] ids, Integer status, Long auditUserId, String auditRemark)
    {
        logger.info("⚖️ 批量审核评论: ids={}, status={}, auditUserId={}", 
            (Object) ids, status, auditUserId);
        
        int result = commentMapper.batchAuditComments(ids, status, auditUserId, auditRemark);
        
        String statusText = status == 1 ? "通过" : "拒绝";
        logger.info("✅ 批量审核评论完成: 处理数量={}, 审核结果={}", result, statusText);
        
        return result;
    }

    /**
     * 获取评论统计信息
     * 
     * @param comment 查询条件
     * @return 统计信息
     */
        @Override
    public Comment getCommentStatistics(Comment comment)
    {
        Comment statistics = commentMapper.selectCommentStatistics(comment);
        logger.info("📊 获取评论统计: 总数={}, 待审核={}, 已通过={}, 已拒绝={}, 查询条件videoId={}",
            statistics.getId(), statistics.getVideoId(),
            statistics.getParentId(), statistics.getReplyCount(),
            comment != null ? comment.getVideoId() : "全部");
        return statistics;
    }

    @Override
    public boolean checkIpCommentLimit(String ipAddress, int minutes)
    {
        if (com.ruoyi.common.utils.StringUtils.isEmpty(ipAddress)) {
            return false;
        }
        
        int count = commentMapper.selectCommentCountByIpAndTime(ipAddress, minutes);
        logger.info("🔒 IP限制检查: ip={}, {}分钟内评论数={}, 限制={}", ipAddress, minutes, count, count >= 10);
        
        // 设置限制：同一IP在指定时间内最多评论10次
        return count >= 10;
    }

    /**
     * 置顶/取消置顶评论
     * 
     * @param id 评论ID
     * @param isSticky 是否置顶
     * @return 结果
     */
    @Override
    public int updateCommentSticky(Long id, Integer isSticky)
    {
        logger.info("📌 {}置顶评论: id={}", isSticky == 1 ? "设置" : "取消", id);
        
        Comment comment = new Comment();
        comment.setId(id);
        comment.setIsSticky(isSticky);
        
        int result = commentMapper.updateComment(comment);
        logger.info("✅ 置顶操作完成: id={}, isSticky={}", id, isSticky);
        
        return result;
    }

    /**
     * 根据视频ID查询已通过的评论列表（前端展示用）- 带缓存
     * 
     * @param videoId 视频ID
     * @return 评论集合
     */
    @Override
    @Cacheable(value = "videoComments", key = "'comments_' + #videoId", unless = "#result == null")
    public List<Comment> selectApprovedCommentsByVideoId(Long videoId)
    {
        logger.info("🔍 查询视频已通过评论: videoId={}", videoId);
        List<Comment> comments = commentMapper.selectCommentsByVideoId(videoId, 1);
        logger.info("✅ 查询完成: videoId={}, 评论数量={}", videoId, comments.size());
        return comments;
    }
} 