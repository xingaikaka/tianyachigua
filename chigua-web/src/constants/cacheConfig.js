/**
 * 缓存配置常量
 * 与后端视频URL签名有效期对齐，避免使用过期签名导致播放失败
 *
 * 后端签名有效期（ChiguaUrlService）：
 * - M3U8 流媒体：6 小时
 * - 图片：2 小时
 * - 视频(VIDEO)/默认：1 小时（生产环境）
 *
 * 前端缓存需短于最短签名有效期，留出安全边界
 */
export const VIDEO_URL_CACHE_MAX_AGE_MS = 50 * 60 * 1000; // 50 分钟，比 1 小时少 10 分钟

/**
 * 检查包含视频 URL 的缓存是否已过期
 * @param {object} cached - 缓存对象，需包含 timestamp 字段
 * @returns {boolean} true 表示已过期或无效，应丢弃
 */
export function isVideoUrlCacheExpired(cached) {
  if (!cached || typeof cached.timestamp !== 'number') return true;
  return Date.now() - cached.timestamp > VIDEO_URL_CACHE_MAX_AGE_MS;
}
