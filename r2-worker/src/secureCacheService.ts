/**
 * 安全边缘缓存服务
 * 
 * 核心原则：先验证，后缓存
 * 
 * 流程：
 * 1. 请求必须先通过签名验证
 * 2. 验证通过后才查询缓存
 * 3. 缓存键使用文件路径（不包含签名参数）
 * 4. 支持动态 TTL 策略
 */

import { CacheConfig } from './cacheConfig';

export interface CacheStats {
  hits: number;
  misses: number;
  hitRate: number;
  savedR2Reads: number;
  estimatedCostSavings: string;
}

export interface CacheMetadata {
  cacheKey: string;
  cacheStatus: 'HIT' | 'MISS';
  cachedAt?: string;
  ttl?: number;
}

export class SecureCacheService {
  private config: CacheConfig;
  private stats: CacheStats = {
    hits: 0,
    misses: 0,
    hitRate: 0,
    savedR2Reads: 0,
    estimatedCostSavings: '$0.00'
  };

  constructor(config: CacheConfig) {
    this.config = config;
    console.log(`🔥 缓存服务初始化: ${config.enabled ? '已启用' : '已禁用'}`);
  }

  /**
   * 检查文件是否可缓存
   */
  isCacheable(key: string, contentType: string, size?: number): boolean {
    // 1. 检查是否启用缓存
    if (!this.config.enabled) {
      return false;
    }

    // 2. 检查文件大小限制
    if (size && size > this.config.maxCacheSize) {
      return false;
    }

    // 3. 检查排除模式
    for (const pattern of this.config.excludePatterns) {
      if (this.matchPattern(key, pattern)) {
        return false;
      }
    }

    // 4. 检查文件类型
    // 🔧 根据扩展名判断可缓存文件（因为R2可能存储为application/octet-stream）
    const cacheableExtensions = [
      // HLS 流媒体
      '.ts', '.m3u8', '.key',
      // 图片
      '.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp',
      // 视频
      '.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv',
      // 音频
      '.mp3', '.wav', '.ogg', '.aac', '.flac', '.m4a'
    ];
    
    const hasCommonExtension = cacheableExtensions.some(ext => key.toLowerCase().endsWith(ext));
    if (hasCommonExtension) {
      return true;
    }
    
    // 基于Content-Type判断
    const isTypeAllowed = this.config.cacheableTypes.some(type =>
      contentType.toLowerCase().includes(type.toLowerCase())
    );

    if (!isTypeAllowed) {
      return false;
    }

    return true;
  }

  /**
   * 构建缓存键
   * 
   * 注意：不包含签名参数（signature, expires, key）
   * 因为这些参数只用于验证，不影响文件内容
   */
  buildCacheKey(key: string, queryParams?: Record<string, string>): string {
    // 🔧 修复：必须使用http/https协议，Cloudflare Workers Cache API要求
    // 版本号：v2修复decrypt=true图片显示问题
    let cacheKey = `https://cache.internal/v2/${key}`;

    // 只包含影响内容的参数（不包含签名相关参数）
    if (queryParams) {
      const relevantParams: string[] = [];
      
      // 这些参数会影响返回的内容，需要加入缓存键
      const contentParams = ['decrypt', 'segments', 'preview', 'quality', 'format'];

      for (const param of contentParams) {
        if (queryParams[param]) {
          relevantParams.push(`${param}=${queryParams[param]}`);
        }
      }

      if (relevantParams.length > 0) {
        cacheKey += `?${relevantParams.sort().join('&')}`;  // 排序保证一致性
      }
    }

    return cacheKey;
  }

  /**
   * 从边缘缓存获取文件
   * 
   * 前提条件：请求已通过签名验证
   * 
   * @param key 文件路径
   * @param queryParams 查询参数
   * @returns 缓存的响应或 null
   */
  async get(key: string, queryParams?: Record<string, string>): Promise<Response | null> {
    if (!this.config.enabled) {
      return null;
    }

    try {
      const cacheKey = this.buildCacheKey(key, queryParams);
      const cache = caches.default;
      
      // 🔧 修复：Cache API需要Request对象，不是字符串
      const cacheRequest = new Request(cacheKey, {
        method: 'GET'
      });
      
      const cachedResponse = await cache.match(cacheRequest);

      if (cachedResponse) {
        // 缓存命中
        this.stats.hits++;
        this.stats.savedR2Reads++;
        this.updateStats();

        // 克隆响应并添加缓存状态头
        const headers = new Headers(cachedResponse.headers);
        headers.set('X-Cache-Status', 'HIT');
        headers.set('X-Cache-Key', cacheKey);

        return new Response(cachedResponse.body, {
          status: cachedResponse.status,
          statusText: cachedResponse.statusText,
          headers
        });
      }

      // 缓存未命中
      this.stats.misses++;
      this.updateStats();

      return null;
    } catch (error) {
      console.error('Cache read error:', error);
      return null;
    }
  }

  /**
   * 将文件存入边缘缓存
   * 
   * 异步执行，不阻塞响应返回
   * 
   * @param key 文件路径
   * @param response 响应对象
   * @param contentType 内容类型
   * @param size 文件大小（可选）
   * @param queryParams 查询参数
   * @param ctx ExecutionContext（用于 waitUntil）
   */
  async put(
    key: string,
    response: Response,
    contentType: string,
    size?: number,
    queryParams?: Record<string, string>,
    ctx?: ExecutionContext
  ): Promise<void> {
    // 检查是否可缓存
    if (!this.isCacheable(key, contentType, size)) {
      return;
    }

    try {
      const cacheKey = this.buildCacheKey(key, queryParams);
      
      // 克隆响应用于缓存
      const cacheResponse = response.clone();

      // 添加缓存元数据头
      const headers = new Headers(cacheResponse.headers);
      headers.set('X-Cache-Status', 'MISS');
      headers.set('X-Cache-Key', cacheKey);
      headers.set('X-Cached-At', new Date().toISOString());
      
      // 🚀 优先使用响应中已有的Cache-Control头，如果没有则根据文件类型设置
      if (!headers.has('Cache-Control')) {
        const ttl = this.getTTL(contentType, key);
        headers.set('Cache-Control', `public, max-age=${ttl}`);
      }

      const responseToCache = new Response(cacheResponse.body, {
        status: cacheResponse.status,
        statusText: cacheResponse.statusText,
        headers
      });

      // 同步执行缓存写入
      try {
        const cache = caches.default;
        const cacheRequest = new Request(cacheKey, { method: 'GET' });
        await cache.put(cacheRequest, responseToCache);
      } catch (error) {
        console.error('Cache write error:', error);
      }
    } catch (error) {
      console.error('❗ 缓存准备错误:', error);
    }
  }

  /**
   * 清除指定文件的缓存
   * 
   * 用于文件删除或更新时
   * 
   * @param key 文件路径
   */
  async invalidate(key: string): Promise<void> {
    if (!this.config.enabled) {
      return;
    }

    try {
      const cache = caches.default;

      // 🔧 修复：Cache API需要Request对象
      // 删除主缓存键
      const mainKey = this.buildCacheKey(key);
      const mainRequest = new Request(mainKey, { method: 'GET' });
      const deleted1 = await cache.delete(mainRequest);

      // 删除可能的变体（带查询参数的版本）
      const variants = [
        this.buildCacheKey(key, { decrypt: 'true' }),
        this.buildCacheKey(key, { preview: 'true' }),
        this.buildCacheKey(key, { quality: 'low' }),
        this.buildCacheKey(key, { quality: 'high' })
      ];

      let deletedCount = deleted1 ? 1 : 0;
      for (const variant of variants) {
        const variantRequest = new Request(variant, { method: 'GET' });
        const deleted = await cache.delete(variantRequest);
        if (deleted) deletedCount++;
      }

      console.log(`🗑️ 缓存已失效: ${key} (清除了 ${deletedCount} 个缓存项)`);
    } catch (error) {
      console.error('❗ 缓存失效错误:', error);
    }
  }

  /**
   * 批量清除缓存
   * 
   * @param keys 文件路径数组
   */
  async invalidateMultiple(keys: string[]): Promise<void> {
    for (const key of keys) {
      await this.invalidate(key);
    }
  }

  /**
   * 获取 TTL（根据文件类型和路径动态计算）
   */
  private getTTL(contentType: string, filePath?: string): number {
    // 优先根据文件路径（扩展名）判断（支持长视频播放）
    if (filePath) {
      const lowerPath = filePath.toLowerCase();
      if (lowerPath.endsWith('.webp') || lowerPath.endsWith('.jpg') || lowerPath.endsWith('.jpeg') || 
          lowerPath.endsWith('.png') || lowerPath.endsWith('.gif') || lowerPath.endsWith('.svg')) {
        return 6900; // 图片：1小时55分钟（签名2小时，留5分钟安全边界）
      } else if (lowerPath.endsWith('.m3u8')) {
        return 21300; // HLS播放列表：5小时55分钟（签名6小时，留5分钟安全边界）
      } else if (lowerPath.endsWith('.ts')) {
        return 86400; // HLS分片：1天（TS文件通常不需要签名，可以长期缓存）
      } else if (lowerPath.endsWith('.mp4') || lowerPath.endsWith('.webm') || lowerPath.endsWith('.avi')) {
        return 3600; // 视频：1小时（签名3600小时，缓存时间可以更长）
      }
    }

    // 根据Content-Type判断
    const lowerType = contentType.toLowerCase();

    // 遍历 TTL 策略，找到匹配的
    for (const [pattern, ttl] of Object.entries(this.config.ttlStrategies)) {
      if (lowerType.includes(pattern.toLowerCase())) {
        return ttl;
      }
    }

    // 默认 TTL
    return this.config.defaultTTL;
  }

  /**
   * 简单的通配符模式匹配
   * 
   * 支持 * 通配符
   * 例如: "temp/*" 匹配 "temp/abc.jpg"
   */
  private matchPattern(key: string, pattern: string): boolean {
    // 将通配符模式转换为正则表达式
    const regexPattern = '^' + pattern.replace(/\*/g, '.*') + '$';
    const regex = new RegExp(regexPattern);
    return regex.test(key);
  }

  /**
   * 更新统计信息
   */
  private updateStats(): void {
    const total = this.stats.hits + this.stats.misses;
    this.stats.hitRate = total > 0 ? (this.stats.hits / total) * 100 : 0;

    // R2 Class A 操作费用：$0.00036/次
    // 每次缓存命中节省一次 R2 读取
    const savedCost = this.stats.savedR2Reads * 0.00036;
    this.stats.estimatedCostSavings = `$${savedCost.toFixed(2)}`;
  }

  /**
   * 获取缓存统计信息
   */
  getStats(): CacheStats {
    return { ...this.stats };
  }

  /**
   * 重置统计信息
   */
  resetStats(): void {
    this.stats = {
      hits: 0,
      misses: 0,
      hitRate: 0,
      savedR2Reads: 0,
      estimatedCostSavings: '$0.00'
    };
    console.log('📊 缓存统计已重置');
  }
}

