/**
 * 全局统计跟踪器 - 防止React严格模式下的重复统计
 * 这个模块确保即使在React严格模式的双重渲染情况下，每个视频的统计也只会触发一次
 */

class StatsTracker {
  constructor() {
    // 存储已统计的视频ID和统计类型
    this.trackedStats = new Map();
    // 正在进行的请求，防止并发
    this.pendingRequests = new Set();
  }

  /**
   * 生成统计键
   * @param {string|number} videoId - 视频ID
   * @param {string} type - 统计类型 (view, play, share)
   * @returns {string} 统计键
   */
  getStatsKey(videoId, type) {
    return `${videoId}_${type}`;
  }

  /**
   * 检查是否已经统计过
   * @param {string|number} videoId - 视频ID
   * @param {string} type - 统计类型
   * @returns {boolean} 是否已统计
   */
  isTracked(videoId, type) {
    const key = this.getStatsKey(videoId, type);
    return this.trackedStats.has(key);
  }

  /**
   * 检查是否正在请求中
   * @param {string|number} videoId - 视频ID
   * @param {string} type - 统计类型
   * @returns {boolean} 是否正在请求中
   */
  isPending(videoId, type) {
    const key = this.getStatsKey(videoId, type);
    return this.pendingRequests.has(key);
  }

  /**
   * 标记开始统计
   * @param {string|number} videoId - 视频ID
   * @param {string} type - 统计类型
   * @returns {boolean} 是否可以继续统计（false表示已被其他请求占用）
   */
  startTracking(videoId, type) {
    const key = this.getStatsKey(videoId, type);
    
        // 如果已经统计过或正在统计，返回false
    if (this.trackedStats.has(key) || this.pendingRequests.has(key)) {
      return false;
    }

    // 标记为正在统计
    this.pendingRequests.add(key);
    return true;
  }

  /**
   * 标记统计完成
   * @param {string|number} videoId - 视频ID
   * @param {string} type - 统计类型
   * @param {boolean} success - 是否成功
   */
  finishTracking(videoId, type, success = true) {
    const key = this.getStatsKey(videoId, type);
    
    // 移除正在进行的标记
    this.pendingRequests.delete(key);
    
    if (success) {
      // 标记为已完成
      this.trackedStats.set(key, {
        timestamp: Date.now(),
        videoId,
        type
      });
    } else {
    }
  }

  /**
   * 重置指定视频的所有统计状态（用于页面切换）
   * @param {string|number} videoId - 视频ID
   */
  resetVideo(videoId) {
    const keysToDelete = [];
    
    // 找到所有相关的键
    for (const key of this.trackedStats.keys()) {
      if (key.startsWith(`${videoId}_`)) {
        keysToDelete.push(key);
      }
    }
    
    // 删除统计记录
    keysToDelete.forEach(key => {
      this.trackedStats.delete(key);
      this.pendingRequests.delete(key); // 也清理pending状态
    });
  }

  /**
   * 清理所有统计记录（用于应用重新启动）
   */
  clearAll() {
    this.trackedStats.clear();
    this.pendingRequests.clear();
  }

  /**
   * 获取调试信息
   */
  getDebugInfo() {
    return {
      tracked: Array.from(this.trackedStats.keys()),
      pending: Array.from(this.pendingRequests),
      count: this.trackedStats.size
    };
  }
}

// 创建全局单例
const statsTracker = new StatsTracker();

export default statsTracker;