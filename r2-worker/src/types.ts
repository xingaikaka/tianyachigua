// ===== 文件服务相关类型 =====

// 文件上传请求
export interface FileUploadRequest {
  file: File;
  type: 'image' | 'video' | 'document' | 'audio';
  customKey?: string;
  metadata?: Record<string, string>;
}

// 文件上传响应
export interface FileUploadResponse {
  success: boolean;
  filePath: string;
  fileName: string;
  size: number;
  contentType: string;
  previewUrl?: string;
  etag?: string;
}

// 文件访问请求
export interface FileAccessRequest {
  filePath: string;
  signature: string;
  expires: number;
  type?: string;
}

// ===== URL生成服务相关类型 =====

// 转码记录信息（仅用于URL生成，不做CRUD）
export interface TranscodeInfo {
  id: string;
  transcode_id: string;
  domain: string;
  picdomain?: string;
  mp4domain?: string;
  path: string;
  orgfile: string;
  suffix?: string;
  cover_image?: string;
  thumbnails?: any;
  resolution?: string;
  duration?: number;
}

// 视频URL信息
export interface VideoUrls {
  videoUrl?: string;         // MP4直接播放URL
  m3u8Url?: string;          // HLS播放URL
  coverUrl?: string;         // 封面图片URL
  preferredPlayUrl?: string; // 推荐的播放URL
}

// URL生成请求
export interface UrlGenerateRequest {
  transcodes: TranscodeInfo[];
}

// 批量URL响应
export interface BatchUrlsResponse {
  urls: Record<string, VideoUrls>; // key为transcodeId
}

// ===== 富文本相关类型 =====

// 富文本资源引用
export interface ResourceReference {
  type: 'image' | 'video';
  resourceKey: string;
  transcodeId?: string;
}

// 富文本上传响应
export interface RichTextUploadResponse {
  success: boolean;
  resourceKey: string;
  url: string;
  type: string;
}

// ===== 通用API响应类型 =====

// API响应格式
export interface ApiResponse<T = any> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: string;
}

// 错误响应
export interface ErrorResponse {
  success: false;
  error: string;
  message: string;
  code?: string;
}

// ===== 配置和环境类型 =====

// Worker环境变量
export interface WorkerEnv {
  CHIGUA_MEDIA: R2Bucket;
  ENVIRONMENT: string;
  JAVA_API_BASE?: string;
  CORS_ORIGINS?: string;
  // 内容安全配置
  CONTENT_SECURITY_ENABLED?: string;
  SIGNATURE_SECRET?: string;
  ALLOWED_DOMAINS?: string;
  ALLOWED_ORIGINS?: string;
  MAX_FILE_SIZE?: string;
  ALLOWED_FILE_TYPES?: string;
  ACCESS_TIME_WINDOW?: string;
  // 图片加密配置
  IMAGE_ENCRYPTION_KEY?: string;
  // 边缘缓存配置
  ENABLE_EDGE_CACHE?: string;
  CACHE_TTL?: string;
  MAX_CACHE_SIZE?: string;
  CACHE_EXCLUDE_PATTERNS?: string;
  CACHEABLE_TYPES?: string;
}

// 文件类型配置
export interface FileTypeConfig {
  allowedExtensions: string[];
  maxSize: number; // bytes
  mimeTypes: string[];
}

// 签名配置
export interface SignatureConfig {
  algorithm: string;
  expirationTime: number; // seconds
  secretKey: string;
}

// ===== 缓存相关类型 =====

// 缓存统计信息
export interface CacheStats {
  hits: number;
  misses: number;
  hitRate: number;
  savedR2Reads: number;
  estimatedCostSavings: string;
}

// 缓存元数据
export interface CacheMetadata {
  cacheKey: string;
  cacheStatus: 'HIT' | 'MISS';
  cachedAt?: string;
  ttl?: number;
}

// 缓存响应
export interface CachedResponse {
  success: boolean;
  cached: boolean;
  cacheStatus: 'HIT' | 'MISS';
  response: Response;
}

// ===== 导入R2类型 =====
import { R2Bucket } from '@cloudflare/workers-types'; 