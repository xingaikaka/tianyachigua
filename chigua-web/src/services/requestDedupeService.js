/**
 * 全局请求去重服务
 * 解决React StrictMode和组件重复渲染导致的并发重复请求问题
 * 只合并"同一时刻"正在进行中的相同请求，不做业务层缓存（业务缓存由后端 Redis 负责）
 */
class RequestDedupeService {
  constructor() {
    this.activeRequests = new Map(); // 正在进行的请求（仅用于并发去重）
  }

  /**
   * 生成请求的唯一键
   */
  generateRequestKey(url, options = {}) {
    const method = options.method || 'GET';
    const params = options.params || {};
    const body = options.body || '';

    const sortedParams = Object.keys(params)
      .sort()
      .map(key => `${key}=${params[key]}`)
      .join('&');

    return `${method}:${url}:${sortedParams}:${JSON.stringify(body)}`;
  }

  /**
   * 去重的请求方法
   * 若已有相同请求正在进行中，则共享该请求的 Promise，不重复发起网络请求
   */
  async dedupeRequest(url, options = {}, requestFn) {
    const requestKey = this.generateRequestKey(url, options);

    // 若已有相同请求正在进行中，直接复用
    if (this.activeRequests.has(requestKey)) {
      return await this.activeRequests.get(requestKey);
    }

    // 发起新请求
    const requestPromise = requestFn();
    this.activeRequests.set(requestKey, requestPromise);

    try {
      return await requestPromise;
    } finally {
      // 请求完成后立即清理，下次同样请求会重新发起
      this.activeRequests.delete(requestKey);
    }
  }

  /**
   * 清理所有进行中的请求记录
   */
  clearAll() {
    this.activeRequests.clear();
  }
}

// 创建全局单例
const requestDedupeService = new RequestDedupeService();

export default requestDedupeService;
