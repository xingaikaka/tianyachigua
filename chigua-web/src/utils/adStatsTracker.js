/**
 * 简单的广告统计跟踪器：避免同一个广告在同一会话内重复上报曝光。
 */
class AdStatsTracker {
  constructor() {
    this.impressionSet = new Set();
  }

  /**
   * 尝试标记曝光，如果之前已经记录则返回 false。
   * @param {number|string} adId
   * @returns {boolean}
   */
  startImpression(adId) {
    if (!adId) return false;
    if (this.impressionSet.has(adId)) {
      return false;
    }
    this.impressionSet.add(adId);
    return true;
  }

  /**
   * 曝光上报失败时撤销标记，允许后续重试。
   * @param {number|string} adId
   */
  cancelImpression(adId) {
    if (!adId) return;
    this.impressionSet.delete(adId);
  }

  /**
   * 清空所有状态（调试用）。
   */
  reset() {
    this.impressionSet.clear();
  }
}

const adStatsTracker = new AdStatsTracker();

export default adStatsTracker;
