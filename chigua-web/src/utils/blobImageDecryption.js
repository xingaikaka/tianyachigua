/**
 * Blob URL 图片解密工具
 * 基于现有的imageDecryption，但返回Blob URL而不是Canvas渲染
 */

import imageDecryption from './imageDecryption';

class BlobImageDecryption {
  constructor() {
    // 存储已创建的Blob URL，用于清理
    this.blobUrls = new Map();
    // 存储解密缓存，避免重复解密
    this.decryptionCache = new Map();
    // 请求队列，避免重复请求
    this.pendingRequests = new Map();
    // 优先级队列管理
    this.priorityQueue = {
      high: [],
      normal: [],
      low: []
    };
    // 并发控制
    this.maxConcurrentRequests = 3; // 基础并发
    this.highPriorityBoost = 2; // 允许高优先级任务超配并发，避免被前置图片阻塞
    this.activeRequests = 0;
  }

  /**
   * 解密图片并创建Blob URL
   * @param {string} src - 图片源URL
   * @param {Object} options - 选项
   * @returns {Promise<string>} Blob URL
   */
  async decryptImageToBlob(src, options = {}) {
    if (!src) {
      throw new Error('图片源URL不能为空');
    }

    // 检查缓存
    if (this.decryptionCache.has(src)) {
      const cached = this.decryptionCache.get(src);
      if (cached.blobUrl && this.isValidBlobUrl(cached.blobUrl)) {
        return cached.blobUrl;
      } else {
        // 缓存的Blob URL已失效，清理缓存
        this.decryptionCache.delete(src);
      }
    }

    // 检查是否有相同的请求正在进行
    if (this.pendingRequests.has(src)) {
      return await this.pendingRequests.get(src);
    }

    // 创建请求Promise并加入队列管理
    const requestPromise = this._queueDecryptionRequest(src, options);
    this.pendingRequests.set(src, requestPromise);

    try {
      const result = await requestPromise;
      return result;
    } catch (error) {
      // 确保错误时也清理请求队列
      this.pendingRequests.delete(src);
      throw error;
    } finally {
      // 清理请求队列
      this.pendingRequests.delete(src);
    }
  }

  /**
   * 队列化解密请求
   * @private
   */
  async _queueDecryptionRequest(src, options) {
    const priority = options.priority || 'normal';
    
    return new Promise((resolve, reject) => {
      const task = {
        src,
        options,
        resolve,
        reject,
        priority
      };

      // 高优先级任务可在一定范围内突破并发上限，确保视频封面/关键图优先
      const limit = (priority === 'high') 
        ? (this.maxConcurrentRequests + this.highPriorityBoost) 
        : this.maxConcurrentRequests;

      if (this.activeRequests < limit) {
        // 立即执行
        this._executeDecryption(task);
      } else {
        // 加入优先级队列
        this.priorityQueue[priority].push(task);
      }
    });
  }

  /**
   * 获取总队列长度
   * @private
   */
  _getTotalQueueLength() {
    return this.priorityQueue.high.length + 
           this.priorityQueue.normal.length + 
           this.priorityQueue.low.length;
  }

  /**
   * 执行解密任务
   * @private
   */
  async _executeDecryption(task) {
    const { src, options, resolve, reject } = task;
    
    this.activeRequests++;

    try {
      const result = await this._performDecryption(src, options);
      resolve(result);
    } catch (error) {
      reject(error);
    } finally {
      this.activeRequests--;
      
      // 处理队列中的下一个任务（按优先级）
      this._processNextTask();
    }
  }

  /**
   * 处理下一个任务（按优先级）
   * @private
   */
  _processNextTask() {
    let nextTask = null;
    
    // 按优先级顺序检查队列
    if (this.priorityQueue.high.length > 0) {
      nextTask = this.priorityQueue.high.shift();
    } else if (this.priorityQueue.normal.length > 0) {
      nextTask = this.priorityQueue.normal.shift();
    } else if (this.priorityQueue.low.length > 0) {
      nextTask = this.priorityQueue.low.shift();
    }

    if (nextTask) {
      this._executeDecryption(nextTask);
    }
  }

  /**
   * 执行实际的解密操作
   * @private
   */
  async _performDecryption(src, options) {
    try {
      // 如果不包含签名参数，可能不是加密图片，直接返回原URL
      if (!src.includes('signature=') && !src.includes('key=')) {
        return src;
      }

      // 检查是否支持图片解密
      if (!imageDecryption.isSupported()) {
        return src;
      }

      // 获取加密的图片数据
      const processedUrl = src.replace(/[&?]decrypt=true/g, '');
      const response = await fetch(processedUrl);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const contentType = response.headers.get('content-type') || '';
      const isEncrypted = response.headers.get('x-encrypted') === 'true';
      const originalContentType = response.headers.get('x-original-content-type') || 'image/jpeg';

      const imageBuffer = await response.arrayBuffer();
      let finalBuffer = imageBuffer;
      let finalContentType = contentType;

      // 如果是加密图片，进行前端解密
      const shouldDecrypt = isEncrypted || imageDecryption.isEncryptedImage(contentType);
      
      if (shouldDecrypt) {
        const filePath = imageDecryption.extractFilePathFromUrl(src);
        
        try {
          finalBuffer = await imageDecryption.decryptImage(
            imageBuffer, 
            filePath, 
            { encrypted: true }
          );
          finalContentType = originalContentType;
        } catch (decryptError) {
          // 解密失败时，检查是否允许降级
          const allowFallback = process.env.REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK === 'true';
          if (allowFallback) {
            finalBuffer = imageBuffer;
            finalContentType = contentType;
          } else {
            throw decryptError;
          }
        }
      }

      // 创建Blob对象
      const blob = new Blob([finalBuffer], { type: finalContentType });
      const blobUrl = URL.createObjectURL(blob);

      // 缓存结果
      const cacheEntry = {
        blobUrl,
        blob,
        createdAt: Date.now()
      };
      this.decryptionCache.set(src, cacheEntry);
      this.blobUrls.set(blobUrl, src);

      return blobUrl;

    } catch (error) {
      throw error;
    }
  }

  /**
   * 检查Blob URL是否仍然有效
   * @param {string} blobUrl - Blob URL
   * @returns {boolean} 是否有效
   */
  isValidBlobUrl(blobUrl) {
    if (!blobUrl || !blobUrl.startsWith('blob:')) {
      return false;
    }
    return this.blobUrls.has(blobUrl);
  }

  /**
   * 清理指定的Blob URL
   * @param {string} blobUrl - 要清理的Blob URL
   */
  revokeBlobUrl(blobUrl) {
    if (blobUrl && blobUrl.startsWith('blob:')) {
      URL.revokeObjectURL(blobUrl);
      
      // 从缓存中移除
      const originalSrc = this.blobUrls.get(blobUrl);
      if (originalSrc) {
        this.blobUrls.delete(blobUrl);
        this.decryptionCache.delete(originalSrc);
      }
    }
  }

  /**
   * 清理所有Blob URL
   */
  revokeAllBlobUrls() {
    for (const blobUrl of this.blobUrls.keys()) {
      URL.revokeObjectURL(blobUrl);
    }
    this.blobUrls.clear();
    this.decryptionCache.clear();
  }

  /**
   * 清理过期的缓存（超过5分钟的缓存）
   */
  cleanupExpiredCache() {
    const now = Date.now();
    const expireTime = 5 * 60 * 1000; // 5分钟

    for (const [src, cacheEntry] of this.decryptionCache.entries()) {
      if (now - cacheEntry.createdAt > expireTime) {
        this.revokeBlobUrl(cacheEntry.blobUrl);
      }
    }
  }

  /**
   * 重置解密系统（清理所有卡住的请求）
   */
  reset() {
    // 清理所有待处理的请求
    this.pendingRequests.clear();
    
    // 清理优先级队列
    this.priorityQueue.high = [];
    this.priorityQueue.normal = [];
    this.priorityQueue.low = [];
    
    // 重置活跃请求计数
    this.activeRequests = 0;
    
  }

  /**
   * 获取缓存统计信息
   */
  getCacheStats() {
    return {
      cacheSize: this.decryptionCache.size,
      blobUrlCount: this.blobUrls.size,
      memoryUsage: this.estimateMemoryUsage(),
      activeRequests: this.activeRequests,
      pendingRequests: this.pendingRequests.size,
      queueLength: this._getTotalQueueLength()
    };
  }

  /**
   * 估算内存使用量（字节）
   */
  estimateMemoryUsage() {
    let totalSize = 0;
    for (const cacheEntry of this.decryptionCache.values()) {
      if (cacheEntry.blob) {
        totalSize += cacheEntry.blob.size;
      }
    }
    return totalSize;
  }
}

// 创建全局实例
const blobImageDecryption = new BlobImageDecryption();

// 页面加载时重置状态，防止残留问题
if (typeof window !== 'undefined') {
  // 监听页面可见性变化，页面重新可见时重置
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
      // 页面重新可见时，清理可能的死锁状态
      if (blobImageDecryption.activeRequests > 0 && blobImageDecryption.pendingRequests.size === 0) {
        blobImageDecryption.reset();
      }
    }
  });
}

// 定期清理过期缓存
setInterval(() => {
  blobImageDecryption.cleanupExpiredCache();
}, 2 * 60 * 1000); // 每2分钟清理一次

// 页面卸载时清理所有Blob URL
window.addEventListener('beforeunload', () => {
  blobImageDecryption.revokeAllBlobUrls();
});

// 开发环境下暴露到全局，方便调试
if (process.env.NODE_ENV === 'development') {
  window.blobImageDecryption = blobImageDecryption;
}

export default blobImageDecryption;
