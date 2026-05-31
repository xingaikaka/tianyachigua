/**
 * HLS.js 公共工具（Vue 管理端）
 *
 * m3u8 中的 key URI 是相对路径 /open/key-s/{sourceId}，hls.js 会将其解析到
 * m3u8 所在的 CDN 域名，而 key 实际由 Java 后端提供。
 * xhrSetup 拦截 key 请求并重定向到正确的后端地址。
 *
 * iOS Safari 原生 HLS 不走 hls.js，需要 patchNativeHlsM3u8 额外处理。
 */

const API_BASE = process.env.VUE_APP_BASE_API || 'http://localhost:8080';

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
 * 返回包含 xhrSetup 的 hls.js 基础配置，调用方可按需扩展
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
 * 浏览器会将相对路径 /open/key(-s)?/xxx 解析到 CDN 域名，导致 404。
 *
 * 此函数：
 *  1. 拉取原始 m3u8 文本
 *  2. 将 #EXT-X-KEY URI 替换为绝对后端地址
 *  3. 返回含修改内容的 Blob URL（调用方使用完毕后需 revokeObjectURL）
 *
 * @param {string} m3u8Url
 * @returns {Promise<{ url: string, isBlob: boolean }>}
 */
export async function patchNativeHlsM3u8(m3u8Url) {
  try {
    const resp = await fetch(m3u8Url);
    if (!resp.ok) return { url: m3u8Url, isBlob: false };
    const text = await resp.text();

    if (!/\/open\/key(?:-s)?\//.test(text)) {
      return { url: m3u8Url, isBlob: false };
    }

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
