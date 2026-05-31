/**
 * LRU 图片 Blob URL 缓存
 *
 * - 最多保留 MAX_SIZE 条，超限时驱逐最久未访问的条目
 * - get() 命中时将条目移到末尾（保持 LRU 顺序）
 * - 注意：此缓存不负责 revoke Blob URL，生命周期由 blobImageDecryption 统一管理
 *   若在此处 revoke，会导致 blobImageDecryption 内部仍认为 URL 有效，
 *   切回旧分类时返回已失效的 URL 造成图片加载失败。
 *
 * Key:   原始图片 URL
 * Value: 已解密的 blob: URL
 */

const MAX_SIZE = 150;

// Map 的迭代顺序 = 插入顺序；最旧的在最前面
const cache = new Map();

function evictOldest() {
  const firstKey = cache.keys().next().value;
  if (firstKey === undefined) return;
  // 只从 LRU 中移除映射关系，不 revoke Blob URL
  // Blob URL 生命周期由 blobImageDecryption 的 cleanupExpiredCache 统一管理
  cache.delete(firstKey);
}

export function get(url) {
  if (!cache.has(url)) return undefined;
  // 访问时移到末尾，维持 LRU 顺序
  const val = cache.get(url);
  cache.delete(url);
  cache.set(url, val);
  return val;
}

export function set(url, blobUrl) {
  if (!url || !blobUrl) return;
  if (cache.has(url)) {
    // 已存在：先删再追加到末尾（更新 LRU 顺序）
    cache.delete(url);
  } else if (cache.size >= MAX_SIZE) {
    evictOldest();
  }
  cache.set(url, blobUrl);
}

export function has(url) {
  return cache.has(url);
}

export function del(url) {
  if (!url) return;
  // 只移除映射关系，不 revoke Blob URL（由 blobImageDecryption 统一管理）
  cache.delete(url);
}

export function clear() {
  // 只清空映射关系，不 revoke Blob URL（由 blobImageDecryption 统一管理）
  cache.clear();
}

export default { get, set, has, del, clear };
