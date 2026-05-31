/**
 * 通用API缓存服务
 * 解决React Strict Mode导致的重复请求问题
 */
class ApiCacheService {
  constructor() {
    this.cache = new Map();
    this.pendingRequests = new Map();
    this.DEFAULT_CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存（已优化）
  }

  /**
   * 生成缓存键
   */
  generateCacheKey(apiName, params = {}) {
    const paramString = Object.keys(params)
      .sort()
      .map(key => `${key}=${params[key]}`)
      .join('&');
    return `${apiName}${paramString ? `?${paramString}` : ''}`;
  }

  /**
   * 检查缓存是否有效
   */
  isCacheValid(cacheData, customDuration = null) {
    if (!cacheData) return false;
    const duration = customDuration || this.DEFAULT_CACHE_DURATION;
    return Date.now() - cacheData.timestamp < duration;
  }

  /**
   * 获取API数据（带缓存和去重）
   */
  async getApiData(apiName, params = {}, apiCall, options = {}) {
    const { 
      cacheDuration = this.DEFAULT_CACHE_DURATION,
      forceRefresh = false,
      // 新增：满足条件时跳过写缓存（例如空页）
      skipCacheIf = null,
      // 新增：当结果为空时使用更短TTL
      emptyCacheDuration = null
    } = options;
    
    const cacheKey = this.generateCacheKey(apiName, params);
    
    // 1. 检查是否强制刷新
    if (forceRefresh) {
      this.cache.delete(cacheKey);
    }
    
    // 2. 检查缓存
    const cached = this.cache.get(cacheKey);
    if (this.isCacheValid(cached, cacheDuration)) {
      return cached.data;
    }

    // 3. 检查是否有正在进行的请求
    if (this.pendingRequests.has(cacheKey)) {
      return await this.pendingRequests.get(cacheKey);
    }

    // 4. 发起新请求
    const requestPromise = this.makeRequest(apiName, params, apiCall);
    this.pendingRequests.set(cacheKey, requestPromise);

    try {
      const result = await requestPromise;

      // 可选：跳过缓存
      if (typeof skipCacheIf === 'function') {
        try {
          if (skipCacheIf(result)) {
            return result;
          }
        } catch (_) {}
      }

      // 为空结果时使用短TTL
      let ttl = cacheDuration;
      if (emptyCacheDuration !== null) {
        const rowsLen = ((result && result.rows && result.rows.length)
          || (result && result.data && result.data.rows && result.data.rows.length)
          || (result && result.data && result.data.list && result.data.list.length)
          || 0);
        if (rowsLen === 0) {
          ttl = emptyCacheDuration;
        }
      }
      
      // 5. 缓存结果
      this.cache.set(cacheKey, {
        data: result,
        timestamp: Date.now()
      });

      return result;
    } finally {
      // 6. 清理进行中的请求
      this.pendingRequests.delete(cacheKey);
    }
  }

  /**
   * 执行实际的API请求
   */
  async makeRequest(apiName, params, apiCall) {
    try {
      const result = await apiCall(params);
      return result;
    } catch (error) {
      throw error;
    }
  }

  /**
   * 清除缓存
   * @param {string|null} pattern - 缓存键模式或API名称
   * @param {object|null} params - 特定参数（当pattern为API名称时使用）
   */
  clearCache(pattern = null, params = null) {
    if (pattern && params) {
      // 清除特定API的缓存
      const cacheKey = this.generateCacheKey(pattern, params);
      this.cache.delete(cacheKey);
      this.pendingRequests.delete(cacheKey);
    } else if (pattern) {
      // 清除匹配模式的缓存
      const keysToDelete = [];
      const cacheKeys = Array.from(this.cache.keys());
      
      for (const key of cacheKeys) {
        if (key.includes(pattern) || key.startsWith(pattern)) {
          keysToDelete.push(key);
        }
      }
      
      keysToDelete.forEach(key => {
        this.cache.delete(key);
        this.pendingRequests.delete(key);
      });
      
    } else {
      // 清除所有缓存
      this.cache.clear();
      this.pendingRequests.clear();
    }
  }

  /**
   * 获取缓存状态（用于调试）
   */
  getCacheStatus() {
    const status = {
      cacheSize: this.cache.size,
      pendingRequests: this.pendingRequests.size,
      cacheKeys: Array.from(this.cache.keys()),
      pendingKeys: Array.from(this.pendingRequests.keys())
    };
    return status;
  }

  /**
   * 预设的API缓存配置
   */
  static API_CONFIGS = {
    VIDEO_DETAIL: { cacheDuration: 5 * 60 * 1000 }, // 5分钟
    COMMENTS: { cacheDuration: 2 * 60 * 1000 }, // 2分钟
    LOGO_ADS: { cacheDuration: 10 * 60 * 1000 }, // 10分钟
    ADVERTISEMENTS: { cacheDuration: 5 * 60 * 1000 } // 5分钟
  };
}

// 创建全局单例
const apiCacheService = new ApiCacheService();

export default apiCacheService;
