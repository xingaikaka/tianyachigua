import { VIDEO_URL_CACHE_MAX_AGE_MS } from '../constants/cacheConfig';

/**
 * 全局状态预加载器
 * 在页面切换前预先准备好状态，避免闪烁
 * 缓存时间需与视频 URL 签名有效期对齐，避免使用过期签名
 */
class GlobalStatePreloader {
  constructor() {
    this.preloadedStates = new Map();
    this.isPreloading = new Map();
    this.maxPreloadAge = VIDEO_URL_CACHE_MAX_AGE_MS; // 50分钟，与后端签名有效期对齐
  }

  /**
   * 预加载页面状态
   */
  async preloadPageState(pageKey, stateLoader) {
    if (this.isPreloading.get(pageKey)) {
      return this.preloadedStates.get(pageKey);
    }

    if (this.preloadedStates.has(pageKey)) {
      const cached = this.preloadedStates.get(pageKey);
      if (Date.now() - cached.timestamp < this.maxPreloadAge) {
        return cached;
      }
    }

    this.isPreloading.set(pageKey, true);

    try {

      const state = await stateLoader();
      
      const preloadedState = {
        ...state,
        timestamp: Date.now(),
        pageKey
      };

      this.preloadedStates.set(pageKey, preloadedState);

      
      return preloadedState;
    } catch (error) {

      return null;
    } finally {
      this.isPreloading.set(pageKey, false);
    }
  }

  /**
   * 获取预加载的状态
   */
  getPreloadedState(pageKey) {
    const cached = this.preloadedStates.get(pageKey);
    if (cached && Date.now() - cached.timestamp < this.maxPreloadAge) {
      return cached;
    }
    return null;
  }

  /**
   * 设置预加载状态
   */
  setPreloadedState(pageKey, state) {
    const preloadedState = {
      ...state,
      timestamp: Date.now(),
      pageKey
    };
    this.preloadedStates.set(pageKey, preloadedState);
  }

  /**
   * 清除预加载状态
   */
  clearPreloadedState(pageKey) {
    this.preloadedStates.delete(pageKey);
    this.isPreloading.delete(pageKey);
  }

  /**
   * 清除所有预加载状态
   */
  clearAll() {
    this.preloadedStates.clear();
    this.isPreloading.clear();
  }

  /**
   * 清理过期状态
   */
  cleanup() {
    const now = Date.now();
    const keysToDelete = [];

    for (const [key, state] of this.preloadedStates) {
      if (now - state.timestamp > this.maxPreloadAge) {
        keysToDelete.push(key);
      }
    }

    keysToDelete.forEach(key => {
      this.preloadedStates.delete(key);
      this.isPreloading.delete(key);
    });

    return keysToDelete.length;
  }

  /**
   * 获取统计信息
   */
  getStats() {
    return {
      preloadedCount: this.preloadedStates.size,
      preloadingCount: Array.from(this.isPreloading.values()).filter(Boolean).length,
      keys: Array.from(this.preloadedStates.keys())
    };
  }
}

// 全局实例
const globalStatePreloader = new GlobalStatePreloader();

// 定期清理过期状态
setInterval(() => {
  globalStatePreloader.cleanup();
}, 2 * 60 * 1000); // 每2分钟清理一次

// 开发环境下暴露到window
if (process.env.NODE_ENV === 'development') {
  window.GlobalStatePreloader = globalStatePreloader;
}

export default globalStatePreloader;
