/**
 * EdgeOne 边缘函数：签名验证 + M3U8 处理
 *
 * 架构：客户端 → EdgeOne（验签 + M3U8处理 + 缓存）→ R2 S3（私有授权直连）
 * 签名算法与 chigua Java 后端一致：HMAC-SHA256("{key}:{expires}:{downloads}") 取前16位hex
 * 目录签名（M3U8子播放列表）：HMAC-SHA256("{dir}:{expires}") 取前16位hex
 */

const SIGNATURE_SECRET = 'VbJBrVDVrzICYlkQgDzBVyuhOoKsWpr0BSpJAERif7Y=';
const DIR_TOKEN_EXPIRES = 21600;  // 目录 token 有效期 6 小时（与 M3U8 签名有效期一致）
const MAX_PREVIEW_SEGMENTS = 20;

const SKIP_EXTS = new Set(['ts', 'm4s', 'key', 'jpeg', 'gif']);

// Java 后端生成的 URL 带有 /files/ 前缀，回源时需要剥离
const FILES_PREFIX = '/files/';


const SYS_PARAMS = new Set(['key', 'dir', 'expires', 'downloads', 'signature', 'n', 'decrypt', 'preview']);

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
  'Access-Control-Allow-Headers': '*',
};

// ===== 入口 =====
addEventListener('fetch', (event) => {
  event.respondWith(handleRequest(event));
});

async function handleRequest(event) {
  const { request } = event;
  const url = new URL(request.url);

  // OPTIONS 预检请求直接放行
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  // 剥离 /files/ 前缀（Java 后端生成的 URL 含此前缀，R2 存储路径不含）
  let pathname = url.pathname;
  if (pathname.startsWith(FILES_PREFIX)) {
    pathname = '/' + pathname.substring(FILES_PREFIX.length);
  }
  const path = pathname.substring(1);
  if (!path) return new Response('OK');

  const ext = getExt(path);

  // 免签资源直接回源，附加 CORS 头
  if (SKIP_EXTS.has(ext)) {
    const r = await fetch(buildOriginRequest(request, pathname));
    return addCors(r);
  }

  // 签名验证
  const err = await verify(url);
  if (err) {
    return new Response(
      JSON.stringify({ success: false, message: err }),
      { status: 403, headers: { 'Content-Type': 'application/json', ...CORS_HEADERS } }
    );
  }

  // 回源取文件（清空签名参数，只保留路径）
  const resp = await fetch(buildOriginRequest(request, pathname));
  if (!resp.ok) return addCors(resp);

  // M3U8 处理：预览截断 + 目录签名
  if (ext === 'm3u8') {
    const n = parseInt(url.searchParams.get('n') || '0', 10);
    const segLimit = n > 0 ? Math.min(n, MAX_PREVIEW_SEGMENTS) : 0;
    return processM3u8(resp, url, path, segLimit);
  }

  return addCors(resp);
}

/** 给 Response 附加 CORS 头（不可变的 Response 需要重新包装） */
function addCors(resp) {
  const headers = new Headers(resp.headers);
  Object.entries(CORS_HEADERS).forEach(([k, v]) => headers.set(k, v));

  // R2 自定义元数据中存储了原始 content-type（如 image/gif），
  // 以 x-amz-meta-contenttype 形式返回。
  // 前端 blobImageDecryption.js 读取 x-original-content-type 来决定解密后的 Blob 类型，
  // 缺失时默认 image/jpeg，导致 GIF 等非 JPEG 格式解密后无法正常显示。
  if (!headers.get('x-original-content-type')) {
    const metaContentType = headers.get('x-amz-meta-contenttype');
    if (metaContentType) {
      headers.set('x-original-content-type', metaContentType);
    }
  }

  return new Response(resp.body, { status: resp.status, statusText: resp.statusText, headers });
}

/**
 * 构造回源请求：使用剥离 /files/ 后的路径，清空 query 参数
 * 避免将签名参数（key/expires/signature 等）透传给 R2
 */
function buildOriginRequest(request, cleanPathname) {
  const newUrl = new URL(request.url);
  newUrl.pathname = cleanPathname;
  newUrl.search = '';
  return new Request(newUrl.toString(), {
    method: request.method,
    headers: request.headers,
  });
}

// =====================================================================
//                          签名验证
// =====================================================================

async function verify(url) {
  const sig = url.searchParams.get('signature');
  if (!sig) return '缺少签名参数';

  const resourceKey = url.searchParams.get('key');
  const dir = url.searchParams.get('dir');
  if (!resourceKey && !dir) return '缺少资源标识参数';

  const expires = parseInt(url.searchParams.get('expires') || '0', 10);
  if (!expires || Date.now() > expires * 1000) return '签名已过期';

  if (dir) {
    // 目录签名（子播放列表）：HMAC-SHA256("{dir}:{expires}")[:16]
    let pathname = url.pathname;
    if (pathname.startsWith(FILES_PREFIX)) pathname = '/' + pathname.substring(FILES_PREFIX.length);
    if (!pathname.substring(1).startsWith(dir)) return '路径不在签名目录范围内';
    const expected = await hmacSign16(`${dir}:${expires}`);
    if (sig !== expected) return '目录签名验证失败';
  } else {
    // 文件签名：HMAC-SHA256("{key}:{expires}:{downloads}")[:16]
    const downloads = url.searchParams.get('downloads') || '';
    const expected = await hmacSign16(`${resourceKey}:${expires}:${downloads}`);
    if (sig !== expected) return '签名验证失败';
  }

  return null;
}

// =====================================================================
//                        M3U8 处理
// =====================================================================

async function processM3u8(resp, url, objectKey, segLimit) {
  const text = await resp.text();
  const lines = text.split('\n');
  const origin = url.origin;

  // 收集子播放列表 + 密钥引用，计算公共目录
  const refs = collectRefs(lines, objectKey);
  const commonDir = findCommonDir(refs);

  // 为子引用生成目录签名 token
  let dirToken = null;
  if (commonDir) {
    dirToken = await genDirToken(commonDir, DIR_TOKEN_EXPIRES);
  }

  const body = segLimit > 0
    ? buildPreview(lines, objectKey, segLimit, dirToken, origin)
    : buildFull(lines, objectKey, dirToken, origin);

  return new Response(body, {
    status: 200,
    headers: {
      'Content-Type': 'application/x-mpegURL',
      'Cache-Control': 'no-cache, no-store',
      ...CORS_HEADERS,
    },
  });
}

/** 完整 M3U8：所有引用改写为完整绝对 URL（经 HLS 代理后才能正确解析） */
function buildFull(lines, key, dirToken, origin) {
  const out = [];
  for (const line of lines) {
    const t = line.trim();
    if (t.startsWith('#EXT-X-KEY:') && t.includes('URI=')) {
      out.push(rewriteKey(t, key, dirToken, origin));
    } else if (isPlaylistRef(t)) {
      const fp = resolve(key, t);
      out.push(fp && dirToken ? `${origin}/${fp}${dirToken}` : line);
    } else if (isSegment(t)) {
      out.push(`${origin}/${resolve(key, t)}`);
    } else {
      out.push(line);
    }
  }
  return out.filter(l => l !== '').join('\n');
}

/** 预览 M3U8：只保留前 N 段 + #EXT-X-ENDLIST */
function buildPreview(lines, key, segLimit, dirToken, origin) {
  const isMaster = lines.some(l => l.trim().startsWith('#EXT-X-STREAM-INF:'));

  // 主播放列表：不截断段，只注入目录签名（n 已透传到 dirToken）
  if (isMaster) return buildFull(lines, key, dirToken, origin);

  // 媒体播放列表：截断到前 N 段
  const segs = parseSegs(lines);
  const preview = segs.slice(0, Math.min(segLimit, segs.length));

  const out = ['#EXTM3U'];
  let maxDur = 0;
  for (const s of preview) maxDur = Math.max(maxDur, s.duration);

  // 复制头部标签
  for (const line of lines) {
    const t = line.trim();
    if (t === '#EXTM3U') continue;
    if (t.startsWith('#EXT-X-VERSION') ||
        t.startsWith('#EXT-X-PLAYLIST-TYPE') ||
        t.startsWith('#EXT-X-MEDIA-SEQUENCE')) {
      out.push(line);
    } else if (t.startsWith('#EXT-X-TARGETDURATION')) {
      out.push(`#EXT-X-TARGETDURATION:${Math.ceil(maxDur)}`);
    } else if (t.startsWith('#EXT-X-KEY:') && t.includes('URI=')) {
      out.push(rewriteKey(t, key, dirToken, origin));
    }
  }

  // 只输出前 N 段（完整绝对 URL）
  for (const s of preview) {
    out.push(`#EXTINF:${s.duration.toFixed(6)},`);
    out.push(`${origin}/${resolve(key, s.filename)}`);
  }

  out.push('#EXT-X-ENDLIST');
  return out.join('\n');
}

function parseSegs(lines) {
  const segs = [];
  let dur = 0;
  for (const line of lines) {
    const t = line.trim();
    if (t.startsWith('#EXTINF:')) {
      const m = t.match(/#EXTINF:([\d.]+)/);
      if (m) dur = parseFloat(m[1]);
    } else if (dur > 0 && !t.startsWith('#') && t.length > 0) {
      segs.push({ filename: t, duration: dur });
      dur = 0;
    }
  }
  return segs;
}

/**
 * 重写 #EXT-X-KEY URI
 * /open/key-s/ 和 /open/key/ 是 Java 后端接口，原样保留（前端 xhrSetup 会拦截处理）
 * 只有 R2 中真实存储的 .key 文件才改写为绝对 URL + 目录 token
 */
function rewriteKey(line, key, dirToken, origin) {
  const m = line.match(/URI="([^"]+)"/);
  if (!m?.[1]) return line;
  const uri = m[1];
  // Java 后端 key 接口，不改写
  if (uri.includes('/open/key')) return line;
  if (!dirToken) return line;
  const fp = resolve(key, uri);
  return line.replace(/URI="[^"]+"/, `URI="${origin}/${fp}${dirToken}"`);
}

// =====================================================================
//                      目录签名 Token
// =====================================================================

async function genDirToken(dir, expiresIn) {
  const expires = Math.floor(Date.now() / 1000) + expiresIn;
  // 目录签名数据："{dir}:{expires}"
  const sig = await hmacSign16(`${dir}:${expires}`);
  return `?dir=${encodeURIComponent(dir)}&expires=${expires}&signature=${sig}`;
}

// =====================================================================
//                    HMAC-SHA256（前 16 位 hex）
// =====================================================================

async function hmacSign16(data) {
  const enc = new TextEncoder();
  const cryptoKey = await crypto.subtle.importKey(
    'raw', enc.encode(SIGNATURE_SECRET),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  );
  const buf = await crypto.subtle.sign('HMAC', cryptoKey, enc.encode(data));
  return Array.from(new Uint8Array(buf))
    .map(b => b.toString(16).padStart(2, '0')).join('').substring(0, 16);
}

// =====================================================================
//                           工具
// =====================================================================

function getExt(p) {
  const i = p.lastIndexOf('.');
  return i >= 0 ? p.substring(i + 1).toLowerCase() : '';
}

function isPlaylistRef(l) {
  return l && !l.startsWith('#') && !l.startsWith('//') && l.toLowerCase().includes('.m3u8');
}

function isSegment(l) {
  if (!l || l.startsWith('#') || l.startsWith('//')) return false;
  const lw = l.toLowerCase();
  return ['.ts', '.m4s', '.mp4', '.m4v', '.m4a', '.jpeg'].some(e => lw.includes(e));
}

function resolve(cur, rel) {
  if (rel.startsWith('http://') || rel.startsWith('https://')) return rel;
  if (rel.startsWith('/')) return rel.substring(1);
  const dir = cur.substring(0, cur.lastIndexOf('/') + 1);
  const parts = (dir + rel).split('/');
  const out = [];
  for (const s of parts) {
    if (s === '..') out.pop();
    else if (s !== '.') out.push(s);
  }
  return out.join('/');
}

function collectRefs(lines, key) {
  const paths = [];
  for (const line of lines) {
    const t = line.trim();
    if (t.startsWith('#EXT-X-KEY:') && t.includes('URI=')) {
      const m = t.match(/URI="([^"]+)"/);
      // 排除 Java 后端 key 接口，它们不在 R2 中
      if (m?.[1] && !m[1].includes('/open/key')) {
        paths.push(resolve(key, m[1]));
      }
    } else if (isPlaylistRef(t)) {
      paths.push(resolve(key, t));
    }
  }
  return paths;
}

function findCommonDir(paths) {
  if (!paths.length) return '';
  const dirs = paths.map(p => {
    const i = p.lastIndexOf('/');
    return i >= 0 ? p.substring(0, i + 1) : '';
  });
  let c = dirs[0];
  for (let i = 1; i < dirs.length; i++) {
    while (c && !dirs[i].startsWith(c)) {
      const t = c.substring(0, c.length - 1);
      const j = t.lastIndexOf('/');
      c = j >= 0 ? t.substring(0, j + 1) : '';
    }
  }
  return c;
}
