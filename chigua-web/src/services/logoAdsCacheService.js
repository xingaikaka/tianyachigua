/**
 * Logo广告缓存服务
 * 解决React Strict Mode导致的重复请求问题
 */
class LogoAdsCacheService {
  constructor() {
    this.cache = new Map();
    this.pendingRequests = new Map();
    this.CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
  }

  /**
   * 获取缓存键
   */
  getCacheKey(appType) {
    return `logo_ads_${appType}`;
  }

  /**
   * 检查缓存是否有效
   */
  isCacheValid(cacheData) {
    if (!cacheData) return false;
    return Date.now() - cacheData.timestamp < this.CACHE_DURATION;
  }

  /**
   * 获取Logo广告（带缓存和去重）
   */
  async getLogoAds(appType, apiCall) {
    const cacheKey = this.getCacheKey(appType);
    
    // 1. 检查缓存
    const cached = this.cache.get(cacheKey);
    if (this.isCacheValid(cached)) {
      return cached.data;
    }

    // 2. 检查是否有正在进行的请求
    if (this.pendingRequests.has(cacheKey)) {
      return await this.pendingRequests.get(cacheKey);
    }

    // 3. 发起新请求
    const requestPromise = this.makeRequest(appType, apiCall);
    this.pendingRequests.set(cacheKey, requestPromise);

    try {
      const result = await requestPromise;
      
      // 4. 缓存结果
      this.cache.set(cacheKey, {
        data: result,
        timestamp: Date.now()
      });

      return result;
    } finally {
      // 5. 清理进行中的请求
      this.pendingRequests.delete(cacheKey);
    }
  }

  /**
   * 执行实际的API请求
   */
  async makeRequest(appType, apiCall) {
    try {
      const response = await apiCall(appType);
      return response;
    } catch (error) {
      throw error;
    }
  }

  /**
   * 清除缓存
   */
  clearCache(appType = null) {
    if (appType) {
      const cacheKey = this.getCacheKey(appType);
      this.cache.delete(cacheKey);
      this.pendingRequests.delete(cacheKey);
    } else {
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
}

// 创建全局单例
const logoAdsCacheService = new LogoAdsCacheService();

export default logoAdsCacheService;
