/**
 * HLS.js 公共工具
 *
 * 核心问题：m3u8 托管在 R2 CDN，文件中的 key URI 是相对路径（/open/key-s/xxx）。
 * hls.js 会将该路径解析到 m3u8 所在的 R2 域名，而非 Java 后端 API 域名。
 * xhrSetup 拦截所有 key 请求，将其重定向到正确的后端地址。
 *
 * iOS Safari 原生 HLS 无法使用 xhrSetup，需额外处理（见 patchNativeHlsM3u8）。
 */

const API_BASE =
  process.env.REACT_APP_API_BASE_URL ||
  (process.env.NODE_ENV === 'production' ? '/prod-api' : 'http://localhost:8080');

/**
 * hls.js xhrSetup 钩子：拦截 AES-128 key 请求，重定向到后端 API
 * 匹配路径：/open/key-s/{sourceId}  和  /open/key/{videoId}
 */
export function hlsXhrSetup(xhr, url) {
  const keyMatch = url.match(/\/open\/key(?:-s)?\/[^?#]*/);
  if (keyMatch) {
    const newUrl = `${API_BASE}${keyMatch[0]}`;
    xhr.open('GET', newUrl, true);
  }
}

/**
 * 返回包含 xhrSetup 的 hls.js 基础配置，调用方可扩展其他字段
 */
export function makeHlsConfig(extra = {}) {
  return {
    xhrSetup: hlsXhrSetup,
    ...extra,
  };
}

/**
 * 为 iOS Safari 原生 HLS 重写 m3u8 中的 key URI。
 *
 * 原生 HLS 不经过 hls.js，无法使用 xhrSetup 拦截 key 请求。
 * 浏览器会将相对路径 /open/key(-s)?/xxx 解析到 m3u8 所在的 CDN 域名，
 * 而 key 实际在后端，导致 404 无法解密。
 *
 * 此函数：
 *  1. 拉取原始 m3u8 文本
 *  2. 将 #EXT-X-KEY 中的相对 key URI 替换为绝对后端地址
 *  3. 返回包含修改后内容的 Blob URL（供 video.src 使用）
 *
 * @param {string} m3u8Url - 原始 m3u8 地址
 * @returns {Promise<{ url: string, isBlob: boolean }>}
 *   url：可直接赋值给 video.src 的地址；isBlob 为 true 时调用方需 revokeObjectURL 释放
 */
export async function patchNativeHlsM3u8(m3u8Url) {
  try {
    const resp = await fetch(m3u8Url);
    if (!resp.ok) return { url: m3u8Url, isBlob: false };
    const text = await resp.text();

    // 检查是否含有需要重写的 key 路径
    if (!/\/open\/key(?:-s)?\//.test(text)) {
      return { url: m3u8Url, isBlob: false };
    }

    // 将 URI="..." 或 URI='...' 中匹配的 key 路径替换为绝对后端地址
    const patched = text.replace(
      /(URI=["'])([^"']+)(["'])/g,
      (match, q1, uri, q2) => {
        const keyMatch = uri.match(/\/open\/key(?:-s)?\/[^"'?\s]*/);
        if (keyMatch) {
          return `${q1}${API_BASE}${keyMatch[0]}${q2}`;
        }
        return match;
      }
    );

    const blob = new Blob([patched], { type: 'application/x-mpegURL' });
    return { url: URL.createObjectURL(blob), isBlob: true };
  } catch (_) {
    return { url: m3u8Url, isBlob: false };
  }
}
