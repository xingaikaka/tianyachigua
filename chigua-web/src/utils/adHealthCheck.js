/**
 * 广告健康检查工具
 * 用于检测广告图片的可用性
 */

class AdHealthChecker {
  constructor() {
    this.failedAds = new Set();
    this.checkCache = new Map();
    this.CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
  }

  /**
   * 检查广告图片是否可用
   * @param {string} imageUrl - 图片URL
   * @returns {Promise<boolean>} - 是否可用
   */
  async checkAdImage(imageUrl) {
    if (!imageUrl) return false;

    // 检查缓存
    const cached = this.checkCache.get(imageUrl);
    if (cached && Date.now() - cached.timestamp < this.CACHE_DURATION) {
      return cached.isValid;
    }

    try {
      const isValid = await this.loadImage(imageUrl);
      
      // 缓存结果
      this.checkCache.set(imageUrl, {
        isValid,
        timestamp: Date.now()
      });

      if (!isValid) {
        this.failedAds.add(imageUrl);
      }

      return isValid;
    } catch (error) {
      this.failedAds.add(imageUrl);
      return false;
    }
  }

  /**
   * 加载图片进行测试
   * @param {string} url - 图片URL
   * @returns {Promise<boolean>}
   */
  loadImage(url) {
    return new Promise((resolve) => {
      const img = new Image();
      const timeout = setTimeout(() => {
        resolve(false);
      }, 5000); // 5秒超时

      img.onload = () => {
        clearTimeout(timeout);
        resolve(true);
      };

      img.onerror = () => {
        clearTimeout(timeout);
        resolve(false);
      };

      img.src = url;
    });
  }

  /**
   * 获取失败的广告列表
   * @returns {Array<string>}
   */
  getFailedAds() {
    return Array.from(this.failedAds);
  }

  /**
   * 清理失败记录
   */
  clearFailedAds() {
    this.failedAds.clear();
    this.checkCache.clear();
  }

  /**
   * 批量检查广告列表
   * @param {Array} ads - 广告列表
   * @returns {Promise<Array>} - 过滤后的可用广告
   */
  async filterValidAds(ads) {
    if (!Array.isArray(ads)) return [];

    const validAds = [];
    
    for (const ad of ads) {
      if (ad.imageUrl) {
        const isValid = await this.checkAdImage(ad.imageUrl);
        if (isValid) {
          validAds.push(ad);
        }
      }
    }

    return validAds;
  }
}

// 创建全局实例
const adHealthChecker = new AdHealthChecker();

export default adHealthChecker;
