import { request } from './api';
import { simpleCache } from '../utils/simpleCache';

/**
 * 带缓存的API服务
 * 基于Java后端签名有效期的前端缓存实现
 */
class CachedApiService {
  
  /**
   * 缓存配置
   */
  static CACHE_CONFIG = {
    // 基于签名有效期的缓存（约50分钟，比Java后端1小时少10分钟安全边界）
    SIGNATURE_BASED_TTL: 50 * 60 * 1000,
    
    // 视频详情缓存
    // ⚠️ 不使用 localStorage 持久化：后端 Redis 缓存(webVideoDetail)已有 50min TTL
    // 两层叠加最坏 100min > VIDEO 签名 60min 有效期，会导致签名过期但缓存仍命中
    // 改为仅内存缓存，页面刷新时重新请求，避免叠加
    VIDEO_DETAIL: {
      ttl: 10 * 60 * 1000,  // 改为10分钟（后端Redis缓存50min，前端内存10min，总计最坏60min刚好等于签名有效期）
      persistent: false,
      keyPrefix: 'video_detail'
    },
    
    // 视频列表缓存
    VIDEO_LIST: {
      ttl: 5 * 60 * 1000,  // 5分钟，配合后端版本控制机制
      persistent: false,
      keyPrefix: 'video_list'
    },
    
    // 合集详情缓存
    COLLECTION_DETAIL: {
      ttl: 50 * 60 * 1000,
      persistent: true,
      keyPrefix: 'collection_detail'
    },
    
    // 合集列表缓存
    COLLECTION_LIST: {
      ttl: 30 * 60 * 1000,  // 30分钟，有主动刷新机制
      persistent: false,
      keyPrefix: 'collection_list'
    },
    
    // 分类缓存（长期）
    CATEGORY_LIST: {
      ttl: 60 * 60 * 1000, // 1小时
      persistent: true,
      keyPrefix: 'category_list'
    },
    
    // 搜索结果缓存
    SEARCH_RESULT: {
      ttl: 15 * 60 * 1000,  // 15分钟，搜索结果变化相对较慢
      persistent: false,
      keyPrefix: 'search_result'
    }
  };
  
  /**
   * 生成缓存Key
   * @param {string} prefix 前缀
   * @param {any} params 参数
   * @returns {string} 缓存Key
   */
  generateCacheKey(prefix, params) {
    if (typeof params === 'object' && params !== null) {
      // 对象参数标准化
      const normalizedParams = Object.keys(params)
        .filter(key => params[key] !== null && params[key] !== undefined && params[key] !== '')
        .sort()
        .map(key => `${key}=${params[key]}`)
        .join('&');
      
      return `${prefix}:${this.hashString(normalizedParams)}`;
    }
    
    return `${prefix}:${params}`;
  }
  
  /**
   * 简单字符串哈希
   * @param {string} str 字符串
   * @returns {string} 哈希值
   */
  hashString(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // 转换为32位整数
    }
    return Math.abs(hash).toString(36);
  }
  
  /**
   * 带缓存的API调用
   * @param {Function} apiFunction API函数
   * @param {any} params 参数
   * @param {Object} cacheConfig 缓存配置
   * @returns {Promise} API响应
   */
  async callWithCache(apiFunction, params, cacheConfig) {
    const cacheKey = this.generateCacheKey(cacheConfig.keyPrefix, params);
    
    // 尝试从缓存获取
    const cached = simpleCache.get(cacheKey);
    if (cached) {
      return cached;
    }
    
    try {
      // 调用API
      const response = await apiFunction(params);
      
      // 缓存成功响应
      if (response && response.code === 200) {
        simpleCache.set(cacheKey, response, cacheConfig.ttl, cacheConfig.persistent);
      }
      
      return response;
    } catch (error) {
      throw error;
    }
  }
  
  /**
   * 清理特定类型的缓存
   * @param {string} type 缓存类型
   */
  clearCacheByType(type) {
    const config = CachedApiService.CACHE_CONFIG[type];
    if (config) {
      simpleCache.clearByPattern(config.keyPrefix);
    }
  }
  
  /**
   * 视频详情（带缓存）
   */
  async getVideoDetail(videoId) {
    return this.callWithCache(
      (id) => request(`/web/api/category/video/detail/${id}`),
      videoId,
      CachedApiService.CACHE_CONFIG.VIDEO_DETAIL
    );
  }
  
  /**
   * 视频列表（带缓存）
   */
  async getVideoList(params = {}) {
    return this.callWithCache(
      (params) => {
        const { categoryId, pageNum = 1, pageSize = 20 } = params;
        const queryParams = new URLSearchParams({
          pageNum: pageNum.toString(),
          pageSize: pageSize.toString()
        });
        
        if (categoryId) {
          queryParams.append('categoryId', categoryId.toString());
        }
        
        return request(`/web/api/category/videos/optimized?${queryParams}`);
      },
      params,
      CachedApiService.CACHE_CONFIG.VIDEO_LIST
    );
  }
  
  /**
   * 合集详情（带缓存）
   */
  async getCollectionDetail(collectionId) {
    return this.callWithCache(
      (id) => request(`/web/api/collection/${id}`),
      collectionId,
      CachedApiService.CACHE_CONFIG.COLLECTION_DETAIL
    );
  }
  
  /**
   * 合集列表（带缓存）
   */
  async getCollectionList(params = {}) {
    return this.callWithCache(
      (params) => request('/web/api/collection/list', {
        method: 'GET',
        params: params
      }),
      params,
      CachedApiService.CACHE_CONFIG.COLLECTION_LIST
    );
  }
  
  /**
   * 分类列表（带缓存）
   */
  async getCategoryList() {
    return this.callWithCache(
      () => request('/web/api/category/list'),
      'all',
      CachedApiService.CACHE_CONFIG.CATEGORY_LIST
    );
  }
  
  /**
   * 搜索视频（带缓存）
   */
  async searchVideos(keyword, pageNum = 1, pageSize = 20) {
    const params = { keyword: keyword.trim(), pageNum, pageSize };
    
    // 空关键词不缓存
    if (!params.keyword) {
      return request('/web/api/search/videos', { params });
    }
    
    return this.callWithCache(
      (params) => request('/web/api/search/videos', { params }),
      params,
      CachedApiService.CACHE_CONFIG.SEARCH_RESULT
    );
  }
  
  /**
   * 手动刷新缓存
   * @param {string} type 缓存类型
   * @param {any} params 参数（可选）
   */
  async refreshCache(type, params = null) {
    const config = CachedApiService.CACHE_CONFIG[type];
    if (!config) {
      return;
    }
    
    if (params) {
      // 刷新特定参数的缓存
      const cacheKey = this.generateCacheKey(config.keyPrefix, params);
      simpleCache.delete(cacheKey);
    } else {
      // 清理整个类型的缓存
      this.clearCacheByType(type);
    }
  }
  
  /**
   * 清理所有缓存（用于调试或强制刷新）
   */
  clearAllCache() {
    simpleCache.clear();
  }
  
  /**
   * 获取缓存统计
   */
  getCacheStats() {
    const stats = simpleCache.getStats();
    const typeStats = {};
    
    // 统计各类型缓存数量
    Object.keys(CachedApiService.CACHE_CONFIG).forEach(type => {
      const config = CachedApiService.CACHE_CONFIG[type];
      let count = 0;
      
      for (const key of simpleCache.cache.keys()) {
        if (key.startsWith(config.keyPrefix)) {
          count++;
        }
      }
      
      typeStats[type] = count;
    });
    
    return {
      ...stats,
      byType: typeStats
    };
  }
}

// 创建全局实例
export const cachedApiService = new CachedApiService();

// 清理历史遗留的 video_detail localStorage 缓存（旧版本 persistent: true 写入的）
// 避免旧的包含过期签名URL的缓存被读取
if (typeof window !== 'undefined') {
  try {
    Object.keys(localStorage).forEach(key => {
      if (key.startsWith('cache_video_detail')) {
        localStorage.removeItem(key);
      }
    });
  } catch (_) {}
}

// 🔧 开发调试：将缓存服务暴露到全局，方便调试
if (typeof window !== 'undefined') {
  window.cachedApiService = cachedApiService;
  window.clearAllCache = () => cachedApiService.clearAllCache();
}

export default cachedApiService;