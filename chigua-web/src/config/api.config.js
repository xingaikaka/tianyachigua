/**
 * API 配置管理
 */
import { API_CONFIG } from './env';

// API 端点配置
export const API_ENDPOINTS = {
  // 用户相关
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    register: '/auth/register',
    profile: '/auth/profile'
  },
  
  // 视频相关
  videos: {
    list: '/api/videos',
    detail: (id) => `/api/videos/${id}`,
    search: '/api/videos/search',
    categories: '/api/videos/categories',
    tags: '/api/videos/tags'
  },
  
  // 合集相关
  collections: {
    list: '/api/collections',
    detail: (id) => `/api/collections/${id}`,
    videos: (id) => `/api/collections/${id}/videos`
  },
  
  // 评论相关
  comments: {
    list: (videoId) => `/api/videos/${videoId}/comments`,
    add: (videoId) => `/api/videos/${videoId}/comments`,
    like: (commentId) => `/api/comments/${commentId}/like`
  },
  
  // 系统配置
  config: {
    site: '/api/config/site',
    ads: '/api/config/ads',
    announcements: '/api/config/announcements'
  }
};

// API 基础配置
export const getApiConfig = () => ({
  baseURL: API_CONFIG.baseUrl,
  timeout: API_CONFIG.timeout,
  headers: {
    'Content-Type': 'application/json',
  }
});

// 构建完整API URL
export const buildApiUrl = (endpoint) => {
  return `${API_CONFIG.baseUrl}${endpoint}`;
};

export default {
  endpoints: API_ENDPOINTS,
  config: getApiConfig,
  buildUrl: buildApiUrl
};
