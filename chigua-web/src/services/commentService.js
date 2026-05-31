import { request } from './api';

class CommentService {
  /**
   * 根据视频ID获取评论列表
   */
  async getCommentsByVideoId(videoId) {
    try {
      const response = await request(`/web/api/comment/video/${videoId}`, {
        method: 'GET'
      });
      return response; // 直接返回响应，因为api.js已经解析了JSON
    } catch (error) {
      
      throw error;
    }
  }

  /**
   * 根据评论类型获取评论列表
   */
  async getCommentsByType(commentType) {
    try {
      const response = await request(`/web/api/comment/type/${commentType}`, {
        method: 'GET'
      });
      return response; // 直接返回响应，因为api.js已经解析了JSON
    } catch (error) {
      
      throw error;
    }
  }

  /**
   * 提交评论
   */
  async submitComment(commentData) {
    try {
      const response = await request('/web/api/comment/submit', {
        method: 'POST',
        body: JSON.stringify(commentData),
        headers: {
          'Content-Type': 'application/json'
        }
      });
      return response; // 直接返回响应，因为api.js已经解析了JSON
    } catch (error) {
      
      throw error;
    }
  }

  /**
   * 构建评论树形结构
   */
  buildCommentTree(comments) {
    if (!Array.isArray(comments)) return [];
    
    const commentMap = new Map();
    const topLevelComments = [];

    // 创建评论映射
    comments.forEach(comment => {
      comment.replies = [];
      commentMap.set(comment.id, comment);
    });

    // 构建树形结构
    comments.forEach(comment => {
      if (comment.parentId && commentMap.has(comment.parentId)) {
        commentMap.get(comment.parentId).replies.push(comment);
      } else {
        topLevelComments.push(comment);
      }
    });

    return topLevelComments;
  }

  /**
   * 格式化时间显示
   */
  formatTime(dateString) {
    if (!dateString) return '';
    
    const now = new Date();
    const commentTime = new Date(dateString);
    const diffMs = now.getTime() - commentTime.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSeconds < 60) {
      return '刚刚';
    } else if (diffMinutes < 60) {
      return `${diffMinutes}分钟前`;
    } else if (diffHours < 24) {
      return `${diffHours}小时前`;
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else {
      return commentTime.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric'
      });
    }
  }

  /**
   * 验证评论内容
   */
  validateComment(username, content, email = '') {
    const errors = [];

    if (!username || username.trim().length === 0) {
      errors.push('用户名不能为空');
    } else if (username.length > 50) {
      errors.push('用户名不能超过50个字符');
    }

    if (!content || content.trim().length === 0) {
      errors.push('评论内容不能为空');
    } else if (content.length > 1000) {
      errors.push('评论内容不能超过1000个字符');
    }

    if (email && email.trim().length > 0) {
      const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
      if (!emailRegex.test(email)) {
        errors.push('邮箱格式不正确');
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  /**
   * 本地存储用户信息
   */
  saveUserInfo(username, email = '') {
    const userInfo = { username, email };
    localStorage.setItem('commentUserInfo', JSON.stringify(userInfo));
  }

  /**
   * 获取本地存储的用户信息
   */
  getUserInfo() {
    try {
      const userInfo = localStorage.getItem('commentUserInfo');
      return userInfo ? JSON.parse(userInfo) : { username: '', email: '' };
    } catch (error) {
      
      return { username: '', email: '' };
    }
  }
}

const commentService = new CommentService();
export default commentService; 