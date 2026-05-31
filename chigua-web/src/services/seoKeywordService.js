import { request } from './api';

/**
 * SEO关键词服务
 * 独立模块，不影响现有功能
 */
const seoKeywordService = {
  /**
   * 根据ID获取关键词详情和相关视频
   */
  getKeywordDetail: (id, pageNum = 1, pageSize = 20) => {
    return request(`/web/api/seo-keywords/${id}`, {
      method: 'GET',
      params: { pageNum, pageSize }
    });
  },

  /**
   * 根据关键词内容获取详情和相关视频
   */
  getByKeyword: (keyword, pageNum = 1, pageSize = 20) => {
    return request('/web/api/seo-keywords/by-keyword', {
      method: 'GET',
      params: { keyword, pageNum, pageSize }
    });
  },

  /**
   * 获取热门关键词
   */
  getHotKeywords: (limit = 50) => {
    return request('/web/api/seo-keywords/hot', {
      method: 'GET',
      params: { limit }
    });
  },

  /**
   * 获取随机关键词
   */
  getRandomKeywords: (limit = 30) => {
    return request('/web/api/seo-keywords/random', {
      method: 'GET',
      params: { limit }
    });
  },

  /**
   * 记录搜索统计
   */
  recordSearch: (id) => {
    return request(`/web/api/seo-keywords/${id}/search`, {
      method: 'POST'
    }).catch(() => {
      // 静默失败，不影响用户体验
    });
  },

  /**
   * 记录点击统计
   */
  recordClick: (id) => {
    return request(`/web/api/seo-keywords/${id}/click`, {
      method: 'POST'
    }).catch(() => {
      // 静默失败
    });
  },
};

export default seoKeywordService;

