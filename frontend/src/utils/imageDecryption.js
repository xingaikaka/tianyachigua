/**
 * 管理后台图片解密工具
 * 与 chigua-web 的 imageDecryption.js 逻辑完全一致，仅 env key 前缀不同
 */

const MASTER_KEY = process.env.VUE_APP_IMAGE_ENCRYPTION_KEY || ''

const textEncoder = new TextEncoder()

/**
 * 从文件路径派生文件专用 AES-GCM 密钥（PBKDF2，100000 次迭代）
 */
async function deriveFileKey(filePath) {
  const masterKeyBuffer = textEncoder.encode(MASTER_KEY)
  const saltBuffer = textEncoder.encode(filePath + '_chigua_salt_2025')

  const masterKey = await crypto.subtle.importKey(
    'raw', masterKeyBuffer, { name: 'PBKDF2' }, false, ['deriveKey']
  )
  const fileKey = await crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt: saltBuffer, iterations: 100000, hash: 'SHA-256' },
    masterKey,
    { name: 'AES-GCM', length: 256 },
    true,
    ['decrypt']
  )
  return crypto.subtle.exportKey('raw', fileKey)
}

/**
 * AES-GCM 解密：前 12 字节为 IV，其余为密文
 */
async function decryptBuffer(encryptedBuffer, filePath) {
  const arr = new Uint8Array(encryptedBuffer)
  const iv = arr.slice(0, 12)
  const ciphertext = arr.slice(12)

  const fileKeyBuffer = await deriveFileKey(filePath)
  const cryptoKey = await crypto.subtle.importKey(
    'raw', fileKeyBuffer, { name: 'AES-GCM' }, false, ['decrypt']
  )
  return crypto.subtle.decrypt({ name: 'AES-GCM', iv, tagLength: 128 }, cryptoKey, ciphertext)
}

/**
 * 从签名 URL 里提取 R2 资源路径（即 key 参数，用于密钥派生）
 */
function extractFilePath(url) {
  try {
    const urlObj = new URL(url)
    const key = urlObj.searchParams.get('key')
    if (key) return key
    let path = urlObj.pathname
    if (path.startsWith('/files/')) path = path.substring(7)
    return decodeURIComponent(path)
  } catch (_) {
    return ''
  }
}

/** Blob URL 缓存：同一资源只解密一次，key = signedUrl 中的 key 参数（资源路径） */
const blobCache = new Map()   // resourceKey → blobUrl
/** 正在进行中的请求去重：避免同一 URL 并发发起多次 fetch */
const pendingMap = new Map()  // signedUrl → Promise<blobUrl>

/** 并发控制：最多同时 4 个 fetch，防止大量请求堵塞浏览器连接池 */
const MAX_CONCURRENT = 4
let activeCount = 0
const waitQueue = []

function acquireSlot() {
  if (activeCount < MAX_CONCURRENT) {
    activeCount++
    return Promise.resolve()
  }
  return new Promise(resolve => waitQueue.push(resolve))
}

function releaseSlot() {
  const next = waitQueue.shift()
  if (next) {
    next()
  } else {
    activeCount--
  }
}

/** 从签名 URL 中提取资源 key，作为缓存键（忽略过期时间等动态参数） */
export function getCacheKey(url) {
  try {
    return new URL(url).searchParams.get('key') || url
  } catch (_) {
    return url
  }
}

/**
 * 同步查询缓存，已解密则直接返回 Blob URL，否则返回 null
 * @param {string} signedUrl CDN 签名 URL
 * @returns {string|null}
 */
export function getCachedBlobUrl(signedUrl) {
  if (!signedUrl) return null
  return blobCache.get(getCacheKey(signedUrl)) || null
}

/**
 * 获取解密后的 Blob URL
 * - 同一资源（相同 key）命中缓存直接返回，无需重复请求
 * - 并发请求同一 URL 时复用同一 Promise，防止重复 fetch
 * - 若图片加密（content-type = application/x-chigua-encrypted 或 x-encrypted: true）则解密
 * @param {string} signedUrl CDN 签名 URL
 * @returns {Promise<string>} blob URL
 */
export function getDecryptedBlobUrl(signedUrl) {
  if (!signedUrl) return Promise.resolve(signedUrl)

  const cacheKey = getCacheKey(signedUrl)

  // 命中缓存，直接返回
  if (blobCache.has(cacheKey)) {
    return Promise.resolve(blobCache.get(cacheKey))
  }

  // 已有相同请求在途，复用同一 Promise
  if (pendingMap.has(signedUrl)) {
    return pendingMap.get(signedUrl)
  }

  const promise = (async () => {
    await acquireSlot()
    try {
      const resp = await fetch(signedUrl)
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

      const contentType = resp.headers.get('content-type') || ''
      const isEncrypted =
        resp.headers.get('x-encrypted') === 'true' ||
        contentType === 'application/x-chigua-encrypted'

      const originalContentType = resp.headers.get('x-original-content-type') || 'image/jpeg'
      const buffer = await resp.arrayBuffer()

      let finalBuffer = buffer
      let finalType = contentType

      if (isEncrypted) {
        if (!MASTER_KEY) throw new Error('VUE_APP_IMAGE_ENCRYPTION_KEY 未配置')
        const filePath = extractFilePath(signedUrl)
        finalBuffer = await decryptBuffer(buffer, filePath)
        finalType = originalContentType
      }

      const blob = new Blob([finalBuffer], { type: finalType })
      const blobUrl = URL.createObjectURL(blob)

      blobCache.set(cacheKey, blobUrl)
      return blobUrl
    } finally {
      releaseSlot()
      pendingMap.delete(signedUrl)
    }
  })()

  pendingMap.set(signedUrl, promise)
  return promise
}
