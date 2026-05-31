/**
 * 边缘缓存配置管理
 * 基于 pornhub-r2-worker 的缓存设计理念
 */

export interface CacheConfig {
  // 是否启用边缘缓存
  enabled: boolean;
  
  // 默认缓存时间（秒）
  defaultTTL: number;
  
  // 最大缓存文件大小（字节）
  maxCacheSize: number;
  
  // 可缓存的文件类型
  cacheableTypes: string[];
  
  // 排除缓存的路径模式
  excludePatterns: string[];
  
  // 动态 TTL 策略（根据文件类型）
  ttlStrategies: Record<string, number>;
}

// 默认配置
export const DEFAULT_CACHE_CONFIG: CacheConfig = {
  enabled: true,
  defaultTTL: 3600,  // 1小时
  maxCacheSize: 10 * 1024 * 1024,  // 10MB
  cacheableTypes: [
    // 图片
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/svg+xml',
    'image/bmp',
    // 视频
    'video/mp4',
    'video/webm',
    'video/mp2t',  // .ts 视频分片
    // HLS 流媒体
    'application/x-mpegURL',  // .m3u8
    'application/vnd.apple.mpegurl',
    // 音频
    'audio/mpeg',
    'audio/mp3',
    'audio/wav',
    'audio/ogg'
  ],
  excludePatterns: [
    'temp/*',
    'draft/*',
    'preview/*'
  ],
  ttlStrategies: {
    // 图片 - 1小时55分钟（签名2小时，留5分钟安全边界）
    'image/': 6900,
    // 视频 - 1小时（签名3600小时，缓存时间可以更长）
    'video/mp4': 3600,
    'video/webm': 3600,
    // HLS 播放列表 - 5小时55分钟（签名6小时，留5分钟安全边界）
    'application/x-mpegURL': 21300,
    'application/vnd.apple.mpegurl': 21300,
    // HLS 分片 - 1天（TS文件通常不需要签名，可以长期缓存）
    'video/mp2t': 86400,
    // 音频 - 1小时
    'audio/': 3600
  }
};

/**
 * 从环境变量构建缓存配置
 */
export function buildCacheConfig(env: any): CacheConfig {
  const enabled = env.ENABLE_EDGE_CACHE === 'true';
  const defaultTTL = parseInt(env.CACHE_TTL || '3600', 10);
  const maxCacheSize = parseInt(env.MAX_CACHE_SIZE || '10485760', 10);
  
  // 从环境变量解析可缓存类型（可选）
  const cacheableTypes = env.CACHEABLE_TYPES
    ? env.CACHEABLE_TYPES.split(',').map((t: string) => t.trim())
    : DEFAULT_CACHE_CONFIG.cacheableTypes;
  
  // 从环境变量解析排除模式（可选）
  const excludePatterns = env.CACHE_EXCLUDE_PATTERNS
    ? env.CACHE_EXCLUDE_PATTERNS.split(',').map((p: string) => p.trim())
    : DEFAULT_CACHE_CONFIG.excludePatterns;

  return {
    enabled,
    defaultTTL,
    maxCacheSize,
    cacheableTypes,
    excludePatterns,
    ttlStrategies: DEFAULT_CACHE_CONFIG.ttlStrategies
  };
}

