import React, { useState, useEffect, useRef } from 'react';
import CommentInput from './CommentInput';
import CommentList from './CommentList';
import commentService from '../../services/commentService';
import apiCacheService from '../../services/apiCacheService';
import videoStatsService from '../../services/videoStatsService';

/**
 * @param {{ videoId?: any, commentType?: string }} props
 */
const CommentSection = ({ videoId = null, commentType = 'video' }) => {
  const [comments, setComments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [replyTo, setReplyTo] = useState(null);
  const [inputPosition, setInputPosition] = useState('top');
  
  // 用于存储移动的输入框元素
  const topInputRef = useRef(null);
  const movedInputRef = useRef(null);
  const replyTargetRef = useRef(null);

  // 本地仅自己可见的乐观评论列表（不持久化，刷新即消失）
  const [optimisticComments, setOptimisticComments] = useState([]);

  // 加载评论列表（使用缓存）
  const loadComments = async () => {
    try {
      setIsLoading(true);
      
      // 根据评论类型调用不同的API
      let response;
      if (commentType === 'submission') {
        // 投稿评论：获取投稿相关的评论
        response = await apiCacheService.getApiData(
          'COMMENTS_SUBMISSION',
          { type: 'submission' },
          () => commentService.getCommentsByType('submission'),
          { cacheDuration: 2 * 60 * 1000 } // 2分钟缓存
        );
      } else if (commentType === 'video' && videoId) {
        // 视频评论：根据videoId获取评论
        response = await apiCacheService.getApiData(
          'COMMENTS_VIDEO',
          { videoId },
          (params) => commentService.getCommentsByVideoId(params.videoId),
          { cacheDuration: 2 * 60 * 1000 } // 2分钟缓存
        );
      } else {
        // 如果没有明确的ID或类型，设置为空数组
        setComments([]);
        setIsLoading(false);
        return;
      }
      
      if (response.code === 200 && response.data) {
        // 后端已经构建好了评论树形结构，直接使用
        setComments(response.data);
      } else {
        
        setComments([]);
      }
    } catch (error) {
        
        setComments([]);
      } finally {
      setIsLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    loadComments();
    // 切换视频时清空本地乐观评论
    setOptimisticComments([]);
  }, [videoId, commentType]);

  // 提交评论
  const handleSubmitComment = async (commentData) => {
    try {
      setIsSubmitting(true);
      
      const submitData = {
        ...commentData,
        videoId: commentType === 'video' && videoId ? parseInt(videoId) : null,
        commentType: commentType,
        parentId: replyTo ? replyTo.id : null
      };

      // 先生成一个仅本地可见的乐观节点，立刻插入到UI
      const optimisticNode = {
        id: `optimistic_${Date.now()}`,
        content: commentData.content,
        username: commentData.username,
        email: commentData.email,
        createTime: new Date().toISOString(),
        parentId: replyTo ? replyTo.id : null,
        // 标记仅本地可见
        _optimistic: true,
        children: []
      };

      // 将乐观评论拼接到现有树形结构的尾部（只在前端合并，刷新即消失）
      setOptimisticComments((prev) => {
        // 只维护一个简单的线性列表，渲染时与真实评论一起拼接
        return [...prev, optimisticNode];
      });

      // 继续向后端提交
      const response = await commentService.submitComment(submitData);
      
      if (response.code === 200) {
        // 对于真实视频，清除缓存并重新加载评论列表
        if (commentType === 'video') {
          apiCacheService.clearCache('COMMENTS_VIDEO', { videoId });
        }
        try {
          videoStatsService.trackCommentSubmit(
            commentType === 'video' ? videoId : '',
            {
              commentType,
              parentId: replyTo ? replyTo.id : null,
              hasEmail: !!(commentData.email && commentData.email.trim())
            }
          );
        } catch (_) {}
        // 提交成功后并不清理本地乐观评论，让其自然随刷新消失
        // 同时避免立刻从后端刷新造成重复显示
      } else {
        throw new Error(response.msg || '评论提交失败');
      }

      // 提交成功后返回到初始状态
      setReplyTo(null);
      setInputPosition('top');
      replyTargetRef.current = null;
    } catch (error) {
      // 提交失败时，移除刚添加的乐观节点
      setOptimisticComments((prev) => prev.filter((c) => !String(c.id).startsWith('optimistic_')));
      // 重新抛出错误，确保错误信息能传递给 CommentInput 处理
      throw error;
    } finally {
      setIsSubmitting(false);
    }
  };

  // 处理回复
  const handleReply = (comment) => {
    setReplyTo(comment);
    setInputPosition('moved');
    
    // 保存回复目标的引用，用于后续定位
    replyTargetRef.current = comment;
    
    // 延迟一下让DOM更新，然后滚动到输入框位置
    setTimeout(() => {
      if (movedInputRef.current) {
        movedInputRef.current.scrollIntoView({
          block: 'center'
        });
      }
    }, 100);
  };

  // 取消回复
  const handleCancelReply = () => {
    setReplyTo(null);
    setInputPosition('top');
    replyTargetRef.current = null;
    
    // 滚动回到顶部输入框
    setTimeout(() => {
      if (topInputRef.current) {
        topInputRef.current.scrollIntoView({
          block: 'start'
        });
      }
    }, 100);
  };

  // 将真实评论与乐观评论合并用于渲染
  const getMergedComments = () => {
    if (!optimisticComments.length) return comments;
    // 简单策略：将所有乐观评论当作顶级评论追加在尾部；
    // 若是回复，则在对应父节点下方追加一个“平级显示”的子项（不嵌套超过二级，以免影响现有逻辑）。
    const merged = JSON.parse(JSON.stringify(comments || []));

    optimisticComments.forEach((node) => {
      if (!node.parentId) {
        // 顶级乐观评论直接追加
        merged.push({ ...node, replies: [] });
      } else {
        // 找到父节点，追加为其子回复
        const appendToParent = (list) => {
          for (let i = 0; i < list.length; i++) {
            const item = list[i];
            if (item.id === node.parentId) {
              const childArr = Array.isArray(item.replies) ? item.replies : [];
              childArr.push({ ...node });
              item.replies = childArr;
              return true;
            }
            if (item.replies && item.replies.length) {
              if (appendToParent(item.replies)) return true;
            }
          }
          return false;
        };
        if (!appendToParent(merged)) {
          // 如果未找到父节点，则降级为顶级显示
          merged.push({ ...node, replies: [] });
        }
      }
    });

    return merged;
  };

  // 总评论数
  const getTotalCommentCount = () => {
    const countReplies = (commentList) => {
      return commentList.reduce((total, comment) => {
        let count = 1; // 当前评论
        if (comment.replies && comment.replies.length > 0) {
          count += countReplies(comment.replies);
        }
        return total + count;
      }, 0);
    };
    
    return countReplies(getMergedComments());
  };

  return (
    <div style={{ backgroundColor: '#2a2a2a', color: '#ffffff', padding: '20px 0' }}>
      {/* 评论统计 */}
      {getTotalCommentCount() > 0 && (
        <div className="mb-4">
          <span style={{ color: '#1ABC9C', fontSize: '16px', fontWeight: 'normal' }}>
            已有 {getTotalCommentCount()} 条评论
          </span>
        </div>
      )}

      {/* 顶部评论输入框 */}
      {inputPosition === 'top' && (
        <div ref={topInputRef} className="mb-6">
          <CommentInput
            onSubmit={handleSubmitComment}
            isLoading={isSubmitting}
            position="top"
          />
        </div>
      )}

      {/* 评论列表 */}
      <div className="space-y-4">
        <CommentList
          comments={getMergedComments()}
          onReply={handleReply}
          isLoading={isLoading}
          // 传递移动输入框相关的props
          replyTo={replyTo}
          inputPosition={inputPosition}
          movedInputRef={movedInputRef}
          onSubmitComment={handleSubmitComment}
          isSubmitting={isSubmitting}
          onCancelReply={handleCancelReply}
        />
      </div>
    </div>
  );
};

export default CommentSection; 