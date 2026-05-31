import { FileService } from './fileService';
import { UrlGenerateService } from './transcodeService';
import { ApiResponse, TranscodeInfo, UrlGenerateRequest, WorkerEnv } from './types';
import { parseQueryParams, createApiResponse, jsonResponse, errorResponse, verifySignedUrl } from './utils';
import type { R2ObjectBody } from '@cloudflare/workers-types';
import { encryptSegmentPayload, isSegmentEncryptionActive, buildSegmentEncryptionMetadata } from './segmentEncryption';

const HLS_PLAYLIST_CACHE_CONTROL = 'public, max-age=300';
const HLS_SEGMENT_CACHE_CONTROL = 'public, max-age=31536000';

function rewriteHlsPlaylist(playlistText: string, filePath: string, requestUrl: URL): string {
  const basePathEnd = filePath.lastIndexOf('/') >= 0 ? filePath.lastIndexOf('/') + 1 : 0;
  const basePath = filePath.substring(0, basePathEnd);

  const lines = playlistText.split(/\r?\n/);
  const rewritten: string[] = [];

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      continue;
    }

    if (line.startsWith('#EXT-X-KEY')) {
      // 不直接透出密钥信息，由前端或密钥接口处理
      continue;
    }

    if (line.startsWith('#')) {
      rewritten.push(line);
      continue;
    }

    if (/^https?:\/\//i.test(line)) {
      // 已是绝对地址，保留
      rewritten.push(line);
      continue;
    }

    const segmentPath = `${basePath}${line}`;
    const encodedPath = encodeURIComponent(segmentPath);
    const segmentUrl = `${requestUrl.origin}/encrypted-ts/${encodedPath}`;
    rewritten.push(segmentUrl);
  }

  return rewritten.join('\n');
}

function buildPlaylistResponse(content: string, corsHeaders: Record<string, string>): Response {
  return new Response(content, {
    headers: {
      ...corsHeaders,
      'Content-Type': 'application/vnd.apple.mpegurl',
      'Cache-Control': HLS_PLAYLIST_CACHE_CONTROL,
      'Access-Control-Allow-Origin': '*',
    },
  });
}

function buildEncryptedSegmentResponse(
  encryptedPayload: Uint8Array,
  metadata: { algorithm: string; salt: string; segmentId: string },
  corsHeaders: Record<string, string>
): Response {
  const headers = new Headers(corsHeaders);
  headers.set('Content-Type', 'video/mp2t');
  headers.set('Cache-Control', HLS_SEGMENT_CACHE_CONTROL);
  headers.set('Access-Control-Allow-Origin', '*');
  headers.set('Content-Length', encryptedPayload.byteLength.toString());
  headers.set('X-Chigua-Encrypted', 'true');
  headers.set('X-Chigua-Enc-Alg', metadata.algorithm);
  headers.set('X-Chigua-Enc-Salt', metadata.salt);
  headers.set('X-Chigua-Enc-Segment', metadata.segmentId);

  return new Response(encryptedPayload, { headers });
}

async function handleProtectedPlaylist(
  filePath: string,
  url: URL,
  env: WorkerEnv,
  corsHeaders: Record<string, string>
): Promise<Response> {
  const object = await env.CHIGUA_MEDIA.get(filePath);
  if (!object) {
    return errorResponse('文件不存在', 404, corsHeaders);
  }

  const playlistText = await object.text();
  const rewrittenPlaylist = rewriteHlsPlaylist(playlistText, filePath, url);
  return buildPlaylistResponse(rewrittenPlaylist, corsHeaders);
}

function buildPassthroughHlsResponse(
  fileObject: R2ObjectBody,
  filePath: string,
  corsHeaders: Record<string, string>
): Response {
  const determineContentType = (targetPath: string): string => {
    if (targetPath.endsWith('.m3u8')) {
      return 'application/vnd.apple.mpegurl';
    }
    if (targetPath.endsWith('.ts')) {
      return 'video/mp2t';
    }
    if (targetPath.endsWith('.key')) {
      return 'application/octet-stream';
    }
    return fileObject.httpMetadata?.contentType || 'application/octet-stream';
  };

  return new Response(fileObject.body, {
    headers: {
      ...corsHeaders,
      'Content-Type': determineContentType(filePath),
      'Cache-Control': HLS_SEGMENT_CACHE_CONTROL,
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
      'Access-Control-Allow-Headers': 'Range',
    },
  });
}

export default {
  async fetch(request: Request, env: WorkerEnv, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // CORS headers
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    // Handle preflight requests
    if (method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      // ===== API routes =====
      if (path.startsWith('/api/')) {
        
        // ===== 文件上传API =====
        if (path.startsWith('/api/upload')) {
          const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
          const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
          const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey);
          
          // 通用文件上传
          if (path === '/api/upload' && method === 'POST') {
            try {
              const formData = await request.formData();
              const file = formData.get('file') as File | null;
              const fileType = formData.get('type') as string || 'document';
              const customKey = formData.get('customKey') as string || undefined;
              
              if (!file || typeof file === 'string') {
                return jsonResponse(createApiResponse(false, null, '缺少有效的文件参数'), corsHeaders);
              }
              
              const result = await fileService.uploadFile(file, fileType, customKey);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '文件上传处理失败', String(error)), corsHeaders);
            }
          }
          
          // 富文本专用上传
          if (path === '/api/upload/richtext' && method === 'POST') {
            try {
              const formData = await request.formData();
              const file = formData.get('file') as File | null;
              
              if (!file || typeof file === 'string') {
                return jsonResponse(createApiResponse(false, null, '缺少有效的文件参数'), corsHeaders);
              }
              
              const result = await fileService.uploadForRichText(file);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '富文本文件上传失败', String(error)), corsHeaders);
            }
          }
          
          // 批量文件删除
          if (path === '/api/upload/batch-delete' && method === 'POST') {
            try {
              const { filePaths } = await request.json() as { filePaths: string[] };
              
              if (!filePaths || !Array.isArray(filePaths)) {
                return jsonResponse(createApiResponse(false, null, '缺少文件路径参数'), corsHeaders);
              }
              
              const result = await fileService.deleteFiles(filePaths);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '批量删除文件失败', String(error)), corsHeaders);
            }
          }
          
          // 获取文件信息
          if (path.startsWith('/api/upload/info/') && method === 'GET') {
            const filePath = decodeURIComponent(path.replace('/api/upload/info/', ''));
            const result = await fileService.getFileInfo(filePath);
            return jsonResponse(result, corsHeaders);
          }
        }
        
        // ===== URL生成API =====
        if (path.startsWith('/api/urls')) {
          const urlService = new UrlGenerateService(url.origin);
          
          // 单个视频URL生成
          if (path === '/api/urls/video' && method === 'POST') {
            try {
              const transcodeInfo = await request.json() as TranscodeInfo;
              const result = await urlService.generateVideoUrls(transcodeInfo);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '生成视频URL失败', String(error)), corsHeaders);
            }
          }
          
          // 批量视频URL生成
          if (path === '/api/urls/batch' && method === 'POST') {
            try {
              const request_data = await request.json() as UrlGenerateRequest;
              const result = await urlService.generateBatchUrls(request_data);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '批量生成URL失败', String(error)), corsHeaders);
            }
          }
          
          // 通用资源URL生成
          if (path === '/api/urls/resource' && method === 'POST') {
            try {
              const { domain, filePath, resourceType } = await request.json() as {
                domain: string;
                filePath: string;
                resourceType: 'video' | 'image' | 'document';
              };
              
              const result = await urlService.generateResourceUrl(domain, filePath, resourceType);
              return jsonResponse(result, corsHeaders);
            } catch (error) {
              return jsonResponse(createApiResponse(false, null, '生成资源URL失败', String(error)), corsHeaders);
            }
          }
        }
        
        // API路由未找到
        return errorResponse('API接口不存在', 404, corsHeaders);
      }
      
      // ===== HLS 加密分片访问 =====
      if (path.startsWith('/encrypted-ts/')) {
        if (!isSegmentEncryptionActive(env)) {
          return errorResponse('HLS分片加密未启用', 412, corsHeaders);
        }

        if (method !== 'GET') {
          return errorResponse('不支持的请求方法', 405, corsHeaders);
        }

        const encodedKey = path.replace('/encrypted-ts/', '');
        const objectKey = decodeURIComponent(encodedKey);

        try {
          const object = await env.CHIGUA_MEDIA.get(objectKey);
          if (!object) {
            return errorResponse('文件不存在', 404, corsHeaders);
          }

          const payload = await object.arrayBuffer();
          const encryptedPayload = await encryptSegmentPayload(payload, objectKey, env);
          const metadata = buildSegmentEncryptionMetadata(objectKey, env);

          return buildEncryptedSegmentResponse(encryptedPayload, metadata, corsHeaders);
        } catch (error) {
          console.error('加密分片处理失败', error);
          return errorResponse('分片处理失败', 500, corsHeaders);
        }
      }

      // ===== 简化的文件上传路由（向后兼容） =====
      if (path === '/upload' && method === 'POST') {
        try {
          const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
          const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
          const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey);
          
          const formData = await request.formData();
          const file = formData.get('file') as File | null;
          const fileType = formData.get('type') as string || 'image'; // 默认图片类型
          const customKey = formData.get('customKey') as string || undefined;
          
          if (!file || typeof file === 'string') {
            return jsonResponse(createApiResponse(false, null, '缺少有效的文件参数'), corsHeaders);
          }
          
          const result = await fileService.uploadFile(file, fileType, customKey);
          
          // 转换为标准响应格式（兼容ImageUpload组件）
          if (result.success && result.data) {
            const compatResponse = {
              success: true,
              key: result.data.filePath,
              filename: result.data.fileName,
              url: result.data.previewUrl,
              size: result.data.size,
              type: fileType,
              message: '上传成功'
            };
            return jsonResponse(compatResponse, corsHeaders);
          } else {
            return jsonResponse({
              success: false,
              message: result.message || '上传失败',
              error: result.error
            }, corsHeaders);
          }
        } catch (error) {
          return jsonResponse({
            success: false,
            message: '文件上传处理失败',
            error: String(error)
          }, corsHeaders);
        }
      }

      // ===== 文件访问服务 =====
      if (path.startsWith('/files/')) {
        const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
        const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
        const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey);
        const filePath = decodeURIComponent(path.replace('/files/', ''));
        
        // 从查询参数获取签名信息
        const key = url.searchParams.get('key');
        const signature = url.searchParams.get('signature');
        const expiresStr = url.searchParams.get('expires');
        const downloads = url.searchParams.get('downloads') || '';

        const isHlsFile = filePath.endsWith('.ts') || filePath.endsWith('.m3u8') || filePath.endsWith('.key');
        const encryptionActive = isSegmentEncryptionActive(env);

        if (isHlsFile && encryptionActive) {
          if (filePath.endsWith('.key')) {
            return errorResponse('密钥不对外提供', 403, corsHeaders);
          }

          if (filePath.endsWith('.m3u8')) {
            return await handleProtectedPlaylist(filePath, url, env, corsHeaders);
          }

          if (filePath.endsWith('.ts')) {
            const encryptedUrl = `${url.origin}/encrypted-ts/${encodeURIComponent(filePath)}`;
            return Response.redirect(encryptedUrl, 302);
          }
        }

        // 🔧 特殊处理：HLS文件的相对路径访问
        if (!signature || !expiresStr) {
          if (isHlsFile) {
            const fileObject = await fileService.getFile(filePath);
            if (!fileObject) {
              return errorResponse('文件不存在', 404, corsHeaders);
            }

            return buildPassthroughHlsResponse(fileObject, filePath, corsHeaders);
          }

          return errorResponse('缺少签名参数', 400, corsHeaders);
        }
        
        const expires = parseInt(expiresStr);
        const actualKey = key || filePath; // 使用key参数或文件路径
        
        const isValidSignature = await verifySignedUrl(actualKey, signature, expires, downloads, secret);
        if (!isValidSignature) {
          return errorResponse('签名验证失败或已过期', 403, corsHeaders);
        }
        
        // 🔐 检查是否请求解密（默认返回加密数据，提供强保护）
        const shouldDecrypt = url.searchParams.get('decrypt') === 'true';
        
        let fileObject;
        if (shouldDecrypt) {
          // 明确请求解密：Worker解密后返回
          fileObject = await fileService.getFile(filePath);
        } else {
          // 默认情况：返回加密数据，前端无法直接使用
          fileObject = await fileService.getEncryptedFile(filePath);
        }
        
        if (!fileObject) {
          return errorResponse('文件不存在', 404, corsHeaders);
        }
        
        // 设置响应头
        const headers = new Headers(corsHeaders);
        
        // 🎯 智能Content-Type设置函数
        const getCorrectContentType = (filePath: string, originalContentType?: string): string => {
          // 根据文件扩展名强制设置正确的MIME类型（优先级最高）
          if (filePath.endsWith('.m3u8')) {
            return 'application/vnd.apple.mpegurl'; // HLS播放列表
          } else if (filePath.endsWith('.ts')) {
            return 'video/mp2t'; // MPEG-2传输流
          } else if (filePath.endsWith('.mp4')) {
            return 'video/mp4'; // MP4视频
          } else if (filePath.endsWith('.jpg') || filePath.endsWith('.jpeg')) {
            return 'image/jpeg'; // JPEG图片
          } else if (filePath.endsWith('.png')) {
            return 'image/png'; // PNG图片
          } else if (filePath.endsWith('.webp')) {
            return 'image/webp'; // WebP图片
          }
          // 如果没有匹配的扩展名，使用原始Content-Type
          return originalContentType || 'application/octet-stream';
        };

        // 根据是否解密设置不同的响应头
        if (shouldDecrypt) {
          // 解密模式：返回正确的content-type
          const correctContentType = getCorrectContentType(filePath, fileObject.httpMetadata?.contentType);
          headers.set('Content-Type', correctContentType);
          headers.set('X-Decrypted', 'true');
        } else {
          // 加密模式：返回特殊标识，前端无法直接使用
          const isEncrypted = fileObject.customMetadata?.encrypted === 'true';
          if (isEncrypted) {
            headers.set('Content-Type', 'application/x-chigua-encrypted');
            headers.set('X-Encrypted', 'true');
            const originalContentType = fileObject.customMetadata?.contentType || fileObject.httpMetadata?.contentType;
            const correctContentType = getCorrectContentType(filePath, originalContentType);
            headers.set('X-Original-Content-Type', correctContentType);
          } else {
            // 未加密文件正常返回正确的Content-Type
            const correctContentType = getCorrectContentType(filePath, fileObject.httpMetadata?.contentType);
            headers.set('Content-Type', correctContentType);
          }
        }
        
        // 设置缓存控制
        const fileType = url.searchParams.get('type') || 'document';
        switch (fileType) {
          case 'image':
            headers.set('Cache-Control', 'public, max-age=86400'); // 1天
            break;
          case 'video':
            headers.set('Cache-Control', 'public, max-age=3600'); // 1小时
            headers.set('Accept-Ranges', 'bytes'); // 支持断点续传
            break;
          default:
            headers.set('Cache-Control', 'public, max-age=300'); // 5分钟
            break;
        }
        
        headers.set('Content-Length', fileObject.size.toString());
        headers.set('ETag', fileObject.etag);
        
        return new Response(fileObject.body, { headers });
      }
      
      // ===== 转码系统路径兼容性处理 =====
      // 支持转码系统直接生成的路径格式（不带/files/前缀）
      if (path.startsWith('/videos/') || path.startsWith('/images/') || path.startsWith('/documents/')) {
        const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
        const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
        const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey);
        const filePath = decodeURIComponent(path.substring(1)); // 移除开头的 /
        
        console.log('🎬 转码系统路径访问:', filePath);
        
        // 检查是否是HLS相关文件（允许无签名访问）
        const isHlsFile = filePath.endsWith('.ts') || filePath.endsWith('.m3u8') || filePath.endsWith('.key');
        const encryptionActive = isSegmentEncryptionActive(env);
        
        if (isHlsFile) {
          if (encryptionActive) {
            if (filePath.endsWith('.key')) {
              return errorResponse('密钥不对外提供', 403, corsHeaders);
            }

            if (filePath.endsWith('.m3u8')) {
              return await handleProtectedPlaylist(filePath, url, env, corsHeaders);
            }

            if (filePath.endsWith('.ts')) {
              const encryptedUrl = `${url.origin}/encrypted-ts/${encodeURIComponent(filePath)}`;
              return Response.redirect(encryptedUrl, 302);
            }
          }

          const fileObject = await fileService.getFile(filePath);
          if (!fileObject) {
            return errorResponse('文件不存在', 404, corsHeaders);
          }
          
          const passthrough = buildPassthroughHlsResponse(fileObject, filePath, corsHeaders);
          return passthrough;
        }
        
        // 非HLS文件需要签名访问，重定向到/files/路径
        return Response.redirect(`${url.origin}/files/${filePath}`, 302);
      }
      
      // ===== 默认路由 =====
      return new Response('Chigua Media Worker - File Service API', {
        headers: {
          'Content-Type': 'text/plain',
          ...corsHeaders,
        },
      });
      
    } catch (error) {
      console.error('Worker处理请求失败:', error);
      return errorResponse('服务器内部错误', 500, corsHeaders);
    }
  },
}; 
