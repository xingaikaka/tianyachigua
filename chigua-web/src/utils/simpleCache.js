/**
 * 简单的前端缓存工具
 * 基于Java后端的签名有效期设计
 */
class SimpleCache {
  constructor() {
    this.cache = new Map();
    this.maxSize = 100; // 限制缓存条目数
    
    // 定期清理过期缓存
    setInterval(() => {
      this.cleanup();
    }, 5 * 60 * 1000); // 每5分钟清理一次
  }
  
  /**
   * 设置缓存
   * @param {string} key 缓存键
   * @param {any} data 缓存数据
   * @param {number} ttl 存活时间（毫秒）
   * @param {boolean} persistent 是否持久化到localStorage
   */
  set(key, data, ttl = 15 * 60 * 1000, persistent = false) {
    // 简单的LRU：如果超过限制，删除最老的
    if (this.cache.size >= this.maxSize) {
      const firstKey = this.cache.keys().next().value;
      this.cache.delete(firstKey);
    }
    
    const cacheItem = {
      data,
      expires: Date.now() + ttl,
      persistent
    };
    
    this.cache.set(key, cacheItem);
    
    // 持久化到localStorage
    if (persistent) {
      try {
        localStorage.setItem(`cache_${key}`, JSON.stringify(cacheItem));
      } catch (error) {
      }
    }
  }
  
  /**
   * 获取缓存
   * @param {string} key 缓存键
   * @returns {any} 缓存数据，过期或不存在返回null
   */
  get(key) {
    // 先从内存缓存获取
    let item = this.cache.get(key);
    
    // 内存中没有，尝试从localStorage获取
    if (!item) {
      try {
        const stored = localStorage.getItem(`cache_${key}`);
        if (stored) {
          item = JSON.parse(stored);
          // 恢复到内存缓存
          this.cache.set(key, item);
        }
      } catch (error) {
      }
    }
    
    if (!item) return null;
    
    // 检查是否过期
    if (Date.now() > item.expires) {
      this.delete(key);
      return null;
    }
    
    return item.data;
  }
  
  /**
   * 删除缓存
   * @param {string} key 缓存键
   */
  delete(key) {
    this.cache.delete(key);
    try {
      localStorage.removeItem(`cache_${key}`);
    } catch (error) {
    }
  }
  
  /**
   * 清理所有缓存
   */
  clear() {
    this.cache.clear();
    // 清理localStorage中的缓存
    try {
      Object.keys(localStorage).forEach(key => {
        if (key.startsWith('cache_')) {
          localStorage.removeItem(key);
        }
      });
    } catch (error) {
    }
  }
  
  /**
   * 按模式清理缓存
   * @param {string} pattern 匹配模式
   */
  clearByPattern(pattern) {
    const keysToDelete = [];
    
    // 清理内存缓存
    for (const key of this.cache.keys()) {
      if (key.includes(pattern)) {
        keysToDelete.push(key);
      }
    }
    
    keysToDelete.forEach(key => {
      this.delete(key);
    });
    
  }
  
  /**
   * 清理过期项
   */
  cleanup() {
    const now = Date.now();
    const expiredKeys = [];
    
    // 清理内存缓存中的过期项
    for (const [key, item] of this.cache.entries()) {
      if (now > item.expires) {
        expiredKeys.push(key);
      }
    }
    
    expiredKeys.forEach(key => {
      this.delete(key);
    });
    
    if (expiredKeys.length > 0) {
    }
  }
  
  /**
   * 获取缓存统计信息
   */
  getStats() {
    const now = Date.now();
    let expired = 0;
    let active = 0;
    
    for (const item of this.cache.values()) {
      if (now > item.expires) {
        expired++;
      } else {
        active++;
      }
    }
    
    return {
      total: this.cache.size,
      active,
      expired,
      maxSize: this.maxSize
    };
  }
}

// 创建全局缓存实例
export const simpleCache = new SimpleCache();
export default simpleCache;