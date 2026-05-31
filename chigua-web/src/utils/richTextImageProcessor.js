/**
 * 富文本图片处理工具
 * 用于处理富文本中的加密图片，将其替换为Blob URL
 */

import blobImageDecryption from './blobImageDecryption';

class RichTextImageProcessor {
  constructor() {
    // 存储处理过的图片映射
    this.processedImages = new Map();
    // 存储待清理的Blob URL
    this.pendingCleanup = new Set();
  }

  /**
   * 处理富文本内容中的图片
   * @param {string} htmlContent - 富文本HTML内容
   * @returns {Promise<string>} 处理后的HTML内容
   */
  async processRichTextImages(htmlContent, options = {}) {
    if (!htmlContent) return htmlContent;

    // 创建临时DOM来解析HTML
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = htmlContent;

    // 查找所有图片元素
    const images = tempDiv.querySelectorAll('img');
    const imagePromises = [];

    for (const img of images) {
      const currentSrc = img.src;
      // 🚀 优先从data-secure-id获取原始URL，如果没有则使用当前src
      const secureId = img.getAttribute('data-secure-id');
      const originalSrc = secureId ? img.getAttribute('data-original-src') || currentSrc : currentSrc;
      // 记录原始地址，便于后续异步处理
      if (originalSrc && originalSrc !== '/loding.jpg') {
        img.setAttribute('data-original-src', originalSrc);
      }
      
      // 🚀 为所有图片先设置预加载占位图片（如果还没有设置的话）
      if (currentSrc !== '/loding.jpg') {
        img.src = '/loding.jpg';
        img.classList.add('image-loading');
      } else {
        // 如果已经是预加载图片，确保添加loading类
        img.classList.add('image-loading');
      }
      
      // 对于有secure-id的图片，我们需要在VideoDetail组件中处理
      // 这里只处理直接的图片URL
      if (!secureId && !options.placeholdersOnly) {
        // 检查是否需要解密
        if (this.needsDecryption(originalSrc)) {
          const promise = this.processImage(img, originalSrc, { priority: 'low' });
          imagePromises.push(promise);
        } else if (originalSrc && originalSrc !== '/loding.jpg') {
          // 🚀 普通图片也需要异步加载，避免阻塞渲染
          const promise = this.processNormalImage(img, originalSrc);
          imagePromises.push(promise);
        }
      }
    }

    // 等待所有图片处理完成（可选）。当 placeholdersOnly=true 时，不等待，立即返回。
    if (!options.placeholdersOnly) {
      await Promise.all(imagePromises);
    }

    return tempDiv.innerHTML;
  }

  /**
   * 处理单个图片元素
   * @param {HTMLImageElement} img - 图片元素
   * @param {string} originalSrc - 原始图片URL
   */
  async processImage(img, originalSrc, options = {}) {
    try {
      // 检查缓存
      if (this.processedImages.has(originalSrc)) {
        const cachedBlobUrl = this.processedImages.get(originalSrc);
        if (blobImageDecryption.isValidBlobUrl(cachedBlobUrl)) {
          img.src = cachedBlobUrl;
          return;
        } else {
          // 缓存失效，清理
          this.processedImages.delete(originalSrc);
        }
      }

      // 🚀 预加载占位图片已在processRichTextImages中设置，这里不需要重复设置

      // 解密图片并获取Blob URL
      const blobUrl = await blobImageDecryption.decryptImageToBlob(originalSrc, { priority: options.priority || 'normal' });
      
      // 更新图片src
      img.src = blobUrl;
      
      // 缓存结果
      this.processedImages.set(originalSrc, blobUrl);
      
      // 添加加载完成的样式类，移除加载状态
      img.classList.add('decrypted-image');
      img.classList.remove('image-loading');
      
      // 设置图片样式
      img.style.maxWidth = '100%';
      img.style.height = 'auto';
      img.style.display = 'block';
      img.style.margin = '8px auto';

    } catch (error) {

      
      // 处理失败时，添加错误样式
      img.classList.add('image-error');
      img.alt = '图片加载失败';
      
      // 可以设置一个错误占位图
      img.src = '/loding.jpg';
    }
  }

  /**
   * 处理普通图片（不需要解密）
   * @param {HTMLImageElement} img - 图片元素
   * @param {string} originalSrc - 原始图片URL
   */
  async processNormalImage(img, originalSrc) {
    try {
      
      // 创建新的图片对象来预加载
      const tempImg = new Image();
      
      // 等待图片加载完成
      await new Promise((resolve, reject) => {
        tempImg.onload = () => {
          resolve();
        };
        tempImg.onerror = () => {
          reject(new Error('图片加载失败'));
        };
        tempImg.src = originalSrc;
      });
      
      // 图片加载成功后，更新实际的img元素
      img.src = originalSrc;
      img.classList.add('image-loaded');
      img.classList.remove('image-loading');
      
      // 设置图片样式
      img.style.maxWidth = '100%';
      img.style.height = 'auto';
      img.style.display = 'block';
      img.style.margin = '8px auto';
      
    } catch (error) {
      
      // 处理失败时，保持占位图片
      img.classList.add('image-error');
      img.classList.remove('image-loading');
      img.alt = '图片加载失败';
    }
  }

  /**
   * 检查图片是否需要解密
   * @param {string} src - 图片URL
   * @returns {boolean} 是否需要解密
   */
  needsDecryption(src) {
    if (!src) return false;
    return src.includes('signature=') || src.includes('key=');
  }

  /**
   * 处理视频封面图片
   * @param {HTMLVideoElement} video - 视频元素
   * @param {string} posterUrl - 封面图片URL
   */
  async processVideoPoster(video, posterUrl, options = {}) {
    if (!video || !posterUrl) {
      return;
    }


    try {
      // 检查是否需要解密
      if (!this.needsDecryption(posterUrl)) {
        video.poster = posterUrl;
        return;
      }

      // 设置预加载占位封面
      video.poster = '/loding.jpg';
      video.classList.add('poster-loading');

      // 解密封面图片
      const blobUrl = await blobImageDecryption.decryptImageToBlob(posterUrl, { priority: options.priority || 'normal' });
      
      // 设置视频封面
      video.poster = blobUrl;
      
      // 存储清理函数
      video._posterCleanup = () => {
        blobImageDecryption.revokeBlobUrl(blobUrl);
      };

      // 添加加载完成的样式类
      video.classList.add('poster-loaded');
      video.classList.remove('poster-loading');

    } catch (error) {
      
      // 使用fallback封面
      video.poster = '/loding.jpg';
      video.classList.add('poster-error');
      video.classList.remove('poster-loading');
    }
  }

  /**
   * 清理指定内容的所有Blob URL
   * @param {string} htmlContent - HTML内容
   */
  cleanupContentImages(htmlContent) {
    if (!htmlContent) return;

    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = htmlContent;
    const images = tempDiv.querySelectorAll('img');

    for (const img of images) {
      const src = img.src;
      if (src && src.startsWith('blob:')) {
        blobImageDecryption.revokeBlobUrl(src);
        
        // 从缓存中查找并清理
        for (const [originalSrc, blobUrl] of this.processedImages.entries()) {
          if (blobUrl === src) {
            this.processedImages.delete(originalSrc);
            break;
          }
        }
      }
    }
  }

  /**
   * 清理所有处理过的图片
   */
  cleanupAllImages() {
    for (const blobUrl of this.processedImages.values()) {
      blobImageDecryption.revokeBlobUrl(blobUrl);
    }
    this.processedImages.clear();
    this.pendingCleanup.clear();
  }

  /**
   * 获取处理统计信息
   */
  getStats() {
    return {
      processedCount: this.processedImages.size,
      pendingCleanupCount: this.pendingCleanup.size
    };
  }
}

// 创建全局实例
const richTextImageProcessor = new RichTextImageProcessor();

// 页面卸载时清理所有图片
window.addEventListener('beforeunload', () => {
  richTextImageProcessor.cleanupAllImages();
});

export default richTextImageProcessor;
