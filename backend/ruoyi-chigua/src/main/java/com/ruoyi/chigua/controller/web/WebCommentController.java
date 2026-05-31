package com.ruoyi.chigua.controller.web;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.domain.Comment;
import com.ruoyi.chigua.service.ICommentService;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web端评论Controller
 * 
 * @author chigua
 * @date 2025-01-22
 */
@RestController
@RequestMapping("/web/api/comment")
public class WebCommentController extends BaseController
{
    private static final Logger logger = LoggerFactory.getLogger(WebCommentController.class);

    @Autowired
    private ICommentService commentService;

    /**
     * 根据视频ID查询已通过的评论列表
     */
    @RateLimiter(time = 60, count = 50, limitType = LimitType.IP)
    @GetMapping("/video/{videoId}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCommentsByVideoId(@PathVariable("videoId") Long videoId)
    {
        logger.info("🔍 查询视频评论: videoId={}", videoId);
        
        if (videoId == null || videoId <= 0) {
            return AjaxResult.error("视频ID无效");
        }
        
        List<Comment> comments = commentService.selectApprovedCommentsByVideoId(videoId);
        
        // 构建评论树形结构
        List<Comment> topLevelComments = buildCommentTree(comments);
        
        logger.info("✅ 查询视频评论完成: videoId={}, 评论数量={}", videoId, comments.size());
        return AjaxResult.success(topLevelComments);
    }

    /**
     * 根据评论类型查询已通过的评论列表
     */
    @RateLimiter(time = 60, count = 50, limitType = LimitType.IP)
    @GetMapping("/type/{commentType}")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult getCommentsByType(@PathVariable("commentType") String commentType)
    {
        logger.info("🔍 查询类型评论: commentType={}", commentType);
        
        if (StringUtils.isEmpty(commentType)) {
            return AjaxResult.error("评论类型无效");
        }
        
        Comment queryComment = new Comment();
        queryComment.setCommentType(commentType);
        queryComment.setStatus(1); // 只查询已通过的评论
        
        List<Comment> comments = commentService.selectCommentList(queryComment);
        
        // 构建评论树形结构
        List<Comment> topLevelComments = buildCommentTree(comments);
        
        logger.info("✅ 查询类型评论完成: commentType={}, 评论数量={}", commentType, comments.size());
        return AjaxResult.success(topLevelComments);
    }

    /**
     * 提交评论
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/submit")
    @EncryptResponse // 启用响应数据加密
    public AjaxResult submitComment(@RequestBody Comment comment, HttpServletRequest request)
    {
        logger.info("📝 提交评论: videoId={}, username={}, content={}", 
            comment.getVideoId(), comment.getUsername(), comment.getContent());
        
        // 基础验证
        // 设置默认评论类型
        if (StringUtils.isEmpty(comment.getCommentType())) {
            comment.setCommentType("video");
        }
        
        // 根据评论类型验证
        if ("video".equals(comment.getCommentType())) {
            // 视频评论需要视频ID
            if (comment.getVideoId() == null || comment.getVideoId() <= 0) {
                return AjaxResult.error("视频ID无效");
            }
        } else if ("submission".equals(comment.getCommentType())) {
            // 投稿评论不需要视频ID，设置为null
            comment.setVideoId(null);
        }
        
        if (StringUtils.isEmpty(comment.getUsername())) {
            return AjaxResult.error("用户名不能为空");
        }
        
        if (StringUtils.isEmpty(comment.getContent())) {
            return AjaxResult.error("评论内容不能为空");
        }
        
        // 内容长度验证
        if (comment.getUsername().length() > 50) {
            return AjaxResult.error("用户名不能超过50个字符");
        }
        
        if (comment.getContent().length() > 1000) {
            return AjaxResult.error("评论内容不能超过1000个字符");
        }
        
        // 邮箱格式验证（如果提供了邮箱）
        if (StringUtils.isNotEmpty(comment.getEmail()) && 
            !isValidEmail(comment.getEmail())) {
            return AjaxResult.error("邮箱格式不正确");
        }
        
        // 设置IP地址和用户代理
        String ipAddress = IpUtils.getIpAddr(request);
        comment.setIpAddress(ipAddress);
        comment.setUserAgent(request.getHeader("User-Agent"));
        
        // IP防刷检查：5分钟内最多10次评论
        if (commentService.checkIpCommentLimit(ipAddress, 5)) {
            logger.warn("🚫 IP评论限制: ip={}, 5分钟内评论次数过多", ipAddress);
            return AjaxResult.error("评论过于频繁，请稍后再试");
        }
        
        // 初始化默认值
        comment.setStatus(0); // 待审核
        comment.setReplyCount(0);
        comment.setLikeCount(0);
        comment.setIsSticky(0);
        
        try {
            int result = commentService.insertComment(comment);
            if (result > 0) {
                logger.info("✅ 评论提交成功: commentId={}, ip={}, username={}", 
                    comment.getId(), ipAddress, comment.getUsername());
                return AjaxResult.success("评论提交成功，正在审核中");
            } else {
                logger.error("❌ 评论提交失败: videoId={}, ip={}", comment.getVideoId(), ipAddress);
                return AjaxResult.error("评论提交失败");
            }
        } catch (Exception e) {
            logger.error("❌ 评论提交异常: videoId={}, ip={}, error={}", 
                comment.getVideoId(), ipAddress, e.getMessage());
            return AjaxResult.error("系统错误，请稍后重试");
        }
    }

    /**
     * 构建评论树形结构
     * 
     * @param comments 所有评论列表
     * @return 顶级评论列表（包含子回复）
     */
    private List<Comment> buildCommentTree(List<Comment> comments) 
    {
        if (comments == null || comments.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 创建评论映射
        java.util.Map<Long, Comment> commentMap = new java.util.HashMap<>();
        java.util.List<Comment> topLevelComments = new java.util.ArrayList<>();

        // 先将所有评论放入映射中，并初始化replies列表
        for (Comment comment : comments) {
            comment.setReplies(new java.util.ArrayList<>());
            commentMap.put(comment.getId(), comment);
        }

        // 构建树形结构
        for (Comment comment : comments) {
            if (comment.getParentId() == null) {
                // 顶级评论
                topLevelComments.add(comment);
            } else {
                // 回复评论，添加到父评论的replies中
                Comment parentComment = commentMap.get(comment.getParentId());
                if (parentComment != null) {
                    parentComment.getReplies().add(comment);
                }
            }
        }

        return topLevelComments;
    }

    /**
     * 验证邮箱格式
     * 
     * @param email 邮箱地址
     * @return 是否有效
     */
    private boolean isValidEmail(String email) 
    {
        if (StringUtils.isEmpty(email)) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
} 