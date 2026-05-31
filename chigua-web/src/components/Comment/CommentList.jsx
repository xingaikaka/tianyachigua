import React from 'react';
import commentService from '../../services/commentService';
import CommentInput from './CommentInput';

const CommentItem = ({ 
  comment, 
  onReply, 
  isReply = false,
  replyTo = null,
  inputPosition = 'top',
  movedInputRef = null,
  onSubmitComment = null,
  isSubmitting = false,
  onCancelReply = null,
  commentMap = null
}) => {
  const handleReplyClick = () => {
    onReply(comment);
  };

  return (
    <div className={`${isReply ? 'ml-8 pl-4' : ''}`} 
         style={{ 
           borderLeft: isReply ? '2px solid #666' : 'none',
           paddingBottom: '16px',
           marginBottom: '16px'
         }}>
      
      <div className="flex items-start space-x-3">
        {/* 头像 */}
        <div 
          className="w-10 h-10 rounded-full flex items-center justify-center text-white font-medium text-sm flex-shrink-0"
          style={{ 
            background: 'linear-gradient(135deg, #e91e63, #9c27b0)',
            minWidth: '40px',
            minHeight: '40px'
          }}
        >
          {comment.username ? comment.username.charAt(0).toUpperCase() : '?'}
        </div>
        
        {/* 评论内容区域 */}
        <div className="flex-1 min-w-0">
          {/* 用户信息和回复按钮 */}
          <div className="flex items-start justify-between mb-1">
            <div style={{ flex: 1, textAlign: 'left' }}>
              <div style={{ color: '#1ABC9C', fontSize: '14px', fontWeight: '500', textAlign: 'left' }}>
                {comment.username || '匿名用户'}
              </div>
              <div style={{ color: '#888888', fontSize: '12px', marginTop: '2px', textAlign: 'left' }}>
                {commentService.formatTime(comment.createTime)}
              </div>
            </div>
            <button
              onClick={handleReplyClick}
              style={{ 
                color: '#1ABC9C', 
                fontSize: '14px',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: '0',
                flexShrink: 0
              }}
              className="hover:opacity-80"
            >
              回复
            </button>
          </div>

          {/* 评论内容 */}
          <div style={{ 
            color: '#ffffff', 
            fontSize: '14px', 
            lineHeight: '1.5',
            marginTop: '6px',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            textAlign: 'left'
          }}>
            {isReply && comment.parentId && commentMap && commentMap.get(comment.parentId) && (
              <span style={{ color: '#1ABC9C' }}>
                @{commentMap.get(comment.parentId).username || '用户'}{' '}
              </span>
            )}
            {comment.content || ''}
          </div>
        </div>
      </div>

    </div>
  );
};

const CommentList = ({ 
  comments, 
  onReply, 
  isLoading = false,
  replyTo = null,
  inputPosition = 'top',
  movedInputRef = null,
  onSubmitComment = null,
  isSubmitting = false,
  onCancelReply = null 
}) => {
  
  // 构建评论映射，用于查找父评论信息
  const buildCommentMap = (commentList) => {
    const map = new Map();
    
    const addToMap = (comments) => {
      comments.forEach(comment => {
        map.set(comment.id, comment);
        if (comment.replies && comment.replies.length > 0) {
          addToMap(comment.replies);
        }
      });
    };
    
    addToMap(commentList);
    return map;
  };
  
  // 扁平化评论结构：只保留一级和二级
  const flattenComments = (commentList) => {
    const flattened = [];
    
    commentList.forEach(topComment => {
      // 添加一级评论
      flattened.push({
        ...topComment,
        isTopLevel: true,
        flattenedReplies: []
      });
      
      // 收集所有回复（递归展开所有嵌套回复）
      const collectAllReplies = (replies) => {
        const allReplies = [];
        
        replies.forEach(reply => {
          allReplies.push(reply);
          if (reply.replies && reply.replies.length > 0) {
            allReplies.push(...collectAllReplies(reply.replies));
          }
        });
        
        return allReplies;
      };
      
      if (topComment.replies && topComment.replies.length > 0) {
        const allReplies = collectAllReplies(topComment.replies);
        flattened[flattened.length - 1].flattenedReplies = allReplies;
      }
    });
    
    return flattened;
  };
  
  const commentMap = buildCommentMap(comments);
  const flattenedComments = flattenComments(comments);
  if (isLoading) {
    return (
      <div className="space-y-4">
        {/* 加载骨架屏 */}
        {[1, 2, 3].map((index) => (
          <div key={index} className="flex items-start space-x-3 animate-pulse" style={{ paddingBottom: '16px' }}>
            <div 
              style={{ 
                width: '40px', 
                height: '40px', 
                backgroundColor: '#444', 
                borderRadius: '50%' 
              }}
            ></div>
            <div className="flex-1">
              <div style={{ height: '12px', backgroundColor: '#444', borderRadius: '4px', width: '80px', marginBottom: '6px' }}></div>
              <div style={{ height: '10px', backgroundColor: '#333', borderRadius: '4px', width: '60px', marginBottom: '8px' }}></div>
              <div style={{ height: '12px', backgroundColor: '#444', borderRadius: '4px', width: '100%', marginBottom: '4px' }}></div>
              <div style={{ height: '12px', backgroundColor: '#444', borderRadius: '4px', width: '75%' }}></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (!comments || comments.length === 0) {
    return (
      <div style={{ 
        textAlign: 'center', 
        padding: '40px 0',
        color: '#888888',
        fontSize: '14px'
      }}>
        暂无评论，发布评论抢首评~
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {flattenedComments.map((comment) => (
        <div key={comment.id}>
          {/* 一级评论 */}
          <CommentItem
            comment={comment}
            onReply={onReply}
            isReply={false}
            replyTo={replyTo}
            inputPosition={inputPosition}
            movedInputRef={movedInputRef}
            onSubmitComment={onSubmitComment}
            isSubmitting={isSubmitting}
            onCancelReply={onCancelReply}
            commentMap={commentMap}
          />
          
          {/* 移动的评论输入框 - 一级评论回复 */}
          {inputPosition === 'moved' && replyTo && replyTo.id === comment.id && (
            <div ref={movedInputRef} style={{ marginTop: '16px' }}>
              <CommentInput
                onSubmit={onSubmitComment}
                isLoading={isSubmitting}
                replyTo={replyTo}
                onCancelReply={onCancelReply}
                position="moved"
              />
            </div>
          )}
          
          {/* 所有回复（扁平化显示为二级） */}
          {comment.flattenedReplies && comment.flattenedReplies.length > 0 && (
            <div className="mt-4 space-y-4">
              {comment.flattenedReplies.map((reply) => (
                <div key={reply.id}>
                  <CommentItem
                    comment={reply}
                    onReply={onReply}
                    isReply={true}
                    replyTo={replyTo}
                    inputPosition={inputPosition}
                    movedInputRef={movedInputRef}
                    onSubmitComment={onSubmitComment}
                    isSubmitting={isSubmitting}
                    onCancelReply={onCancelReply}
                    commentMap={commentMap}
                  />
                  
                  {/* 移动的评论输入框 - 二级评论回复 */}
                  {inputPosition === 'moved' && replyTo && replyTo.id === reply.id && (
                    <div ref={movedInputRef} style={{ marginTop: '16px' }}>
                      <CommentInput
                        onSubmit={onSubmitComment}
                        isLoading={isSubmitting}
                        replyTo={replyTo}
                        onCancelReply={onCancelReply}
                        position="moved"
                      />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default CommentList; 