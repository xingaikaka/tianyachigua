import { request } from './api';

/**
 * 视频统计服务
 * 用于调用后台视频统计相关API
 */
const videoStatsService = {
  /**
   * 获取视频统计数据
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 统计数据响应
   */
  async getVideoStats(videoId) {
    try {
      const response = await request(`/chigua/stats/video/${videoId}`);
      
      if (response.code === 200) {
        return response;
      } else {
        return response;
      }
    } catch (error) {
      // 返回默认数据，确保前端不会因为统计API失败而崩溃
      return {
        code: 500,
        data: {
          videoId: videoId,
          viewCount: 0,
          playCount: 0,
          likeCount: 0,
          commentCount: 0,
          shareCount: 0
        },
        msg: '统计数据获取失败'
      };
    }
  },

  /**
   * 增加浏览量（页面进入时调用）
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async incrementViewCount(videoId) {
    try {
      const ids = getIdentity();
      const response = await request(`/chigua/stats/view/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ anonymousId: ids.anonymousId })
      });
      
      return response;
    } catch (error) {
      return { code: 500, msg: '浏览量统计失败' };
    }
  },

  /**
   * 增加播放量（视频开始播放时调用）
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async incrementPlayCount(videoId) {
    try {
      const ids = getIdentity();
      const response = await request(`/chigua/stats/play/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ anonymousId: ids.anonymousId })
      });
      
      return response;
    } catch (error) {
      return { code: 500, msg: '播放量统计失败' };
    }
  },

  /**
   * 增加点赞量
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async incrementLikeCount(videoId) {
    try {
      const response = await request(`/chigua/stats/like/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      // 静默处理响应
      
      return response;
    } catch (error) {
      return { code: 500, msg: '点赞失败' };
    }
  },

  /**
   * 记录分类点击
   * @param {number|string} categoryId - 分类ID
   */
  async trackCategoryClick(categoryId) {
    try {
      const ids = getIdentity();
      await request(`/web/analytics/event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'category_click', categoryId, ...ids })
      });
    } catch (_) {}
  },

  /**
   * 记录搜索提交
   * @param {string} keyword - 搜索关键词
   */
  async trackSearchSubmit(keyword) {
    try {
      const ids = getIdentity();
      await request(`/web/analytics/event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'search_submit', keyword, ...ids })
      });
    } catch (_) {}
  },

  /**
   * 记录搜索零结果（搜索成功但返回 0 条内容时调用）
   * @param {string} keyword - 搜索关键词
   */
  async trackSearchNoResult(keyword) {
    try {
      const ids = getIdentity();
      await request(`/web/analytics/event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'search_no_result', keyword, ...ids })
      });
    } catch (_) {}
  },

  /**
   * 记录页面浏览
   * @param {string} path - 页面路径
   */
  async trackPageView(path) {
    try {
      const ids = getIdentity();
      await request(`/web/analytics/event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: 'page_view', path, ...ids })
      });
    } catch (_) {}
  },

  /**
   * 通用埋点：上报任意类型事件到 /web/analytics/event
   * @param {string} type      事件类型
   * @param {Object} payload   其它字段，会与 ids/path 一起合并发送
   */
  async trackEvent(type, payload = {}) {
    try {
      const ids = getIdentity();
      await request(`/web/analytics/event`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type, ...ids, ...payload })
      });
    } catch (_) {}
  },

  // ===== Redgifs（独立 ID 体系，与主站 videos 分开统计） =====

  /** 进入 redgifs 视频页（浏览） */
  trackRedgifsView(videoId, extra) {
    return this.trackEvent('redgifs_view', { targetId: String(videoId), ...(extra || {}) });
  },

  /** redgifs 视频开始播放 */
  trackRedgifsPlay(videoId, extra) {
    return this.trackEvent('redgifs_play', { targetId: String(videoId), ...(extra || {}) });
  },

  /** 分享 redgifs 视频 */
  trackRedgifsShare(videoId, extra) {
    return this.trackEvent('redgifs_share', { targetId: String(videoId), ...(extra || {}) });
  },

  /** 进入 redgifs 用户详情页 */
  trackRedgifsUserView(userId, extra) {
    return this.trackEvent('redgifs_user_view', { targetId: String(userId), ...(extra || {}) });
  },

  // ===== Telegram =====

  /** 进入 Telegram 帖子详情页 */
  trackTgPostView(postId, extra) {
    return this.trackEvent('tg_post_view', { targetId: String(postId), ...(extra || {}) });
  },

  /** Telegram 帖子内某媒体被打开/全屏播放（可选） */
  trackTgMediaPlay(postId, mediaId, extra) {
    return this.trackEvent('tg_media_play', { targetId: String(postId), mediaId: String(mediaId), ...(extra || {}) });
  },

  // ===== 其它通用行为 =====

  /** 提交评论（视频评论 / 投稿评论） */
  trackCommentSubmit(targetId, extra) {
    return this.trackEvent('comment_submit', {
      targetId: targetId != null ? String(targetId) : '',
      ...(extra || {})
    });
  },

  /** 广告点击（包括 popup / banner / logo / textLink 等所有位置） */
  trackAdClick(adId, extra) {
    return this.trackEvent('ad_click', {
      targetId: String(adId),
      ...(extra || {})
    });
  },

  /** 合集卡片点击 → 进入合集详情 */
  trackCollectionView(collectionId, extra) {
    return this.trackEvent('collection_view', {
      targetId: String(collectionId),
      ...(extra || {})
    });
  },

  /** 标签点击（热门 tag / 标签页 tag） */
  trackTagClick(tagId, extra) {
    return this.trackEvent('tag_click', {
      targetId: String(tagId),
      ...(extra || {})
    });
  },

  /** 用户卡片点击（Redgifs 用户组列表点开某用户） */
  trackUserClick(username, extra) {
    if (!username) return;
    return this.trackEvent('redgifs_user_view', {
      targetId: String(username),
      ...(extra || {})
    });
  },

  /**
   * 减少点赞量（取消点赞）
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async decrementLikeCount(videoId) {
    try {
      const response = await request(`/chigua/stats/unlike/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      return response;
    } catch (error) {
      return { code: 500, msg: '取消点赞失败' };
    }
  },

  /**
   * 增加分享次数
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async incrementShareCount(videoId) {
    try {
      const response = await request(`/chigua/stats/share/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      return response;
    } catch (error) {
      return { code: 500, msg: '分享统计失败' };
    }
  },

  /**
   * 同步评论数量（用于后台同步评论数）
   * @param {number|string} videoId - 视频ID
   * @returns {Promise<Object>} 操作结果
   */
  async syncCommentCount(videoId) {
    try {
      const response = await request(`/chigua/stats/sync-comment/${videoId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      return response;
    } catch (error) {
      return { code: 500, msg: '评论数量同步失败' };
    }
  }
};

function getIdentity() {
  try {
    let anonymousId = localStorage.getItem('anonymous_id');
    if (!anonymousId) {
      anonymousId = `${Date.now().toString(36)}_${Math.random().toString(36).slice(2,10)}`;
      localStorage.setItem('anonymous_id', anonymousId);
    }
    let sessionId = sessionStorage.getItem('session_id');
    if (!sessionId) {
      sessionId = `${Date.now().toString(36)}_${Math.random().toString(36).slice(2,10)}`;
      sessionStorage.setItem('session_id', sessionId);
    }
    return { anonymousId, sessionId };
  } catch (_) {
    return {};
  }
}

export default videoStatsService;