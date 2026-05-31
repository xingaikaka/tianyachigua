import { FileService } from './fileService';
import { UrlGenerateService } from './transcodeService';
import { ApiResponse, TranscodeInfo, UrlGenerateRequest, WorkerEnv } from './types';
import { parseQueryParams, createApiResponse, jsonResponse, errorResponse, verifySignedUrl } from './utils';

export default {
  async fetch(request: Request, env: WorkerEnv, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // CORS headers（支持 Range 请求）
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS, HEAD',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, Range',
      'Access-Control-Expose-Headers': 'Content-Length, Content-Range, Accept-Ranges, Content-Type, ETag, Last-Modified',
    };

    // Handle preflight requests
    if (method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      // ===== API routes =====
      if (path.startsWith('/api/')) {
        
        // ===== 缓存统计API =====
        if (path === '/api/cache/stats' && method === 'GET') {
          const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
          const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
          const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey, env);
          
          const stats = fileService.getCacheStats();
          
          return jsonResponse({
            success: true,
            cacheEnabled: env.ENABLE_EDGE_CACHE === 'true',
            stats: {
              hits: stats.hits,
              misses: stats.misses,
              hitRate: `${stats.hitRate.toFixed(2)}%`,
              savedR2Reads: stats.savedR2Reads,
              costSavings: stats.estimatedCostSavings
            },
            message: '缓存统计信息'
          }, corsHeaders);
        }
        
        // ===== 缓存测试API（仅开发/测试环境使用） =====
        if (path.startsWith('/api/cache/test/') && method === 'GET') {
          try {
            const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
            const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
            const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey, env);
            
            const filePath = decodeURIComponent(path.replace('/api/cache/test/', ''));
            const queryParams = parseQueryParams(url.searchParams);
            const cachedResponse = await fileService.getFileWithCache(filePath, queryParams, ctx);
            
            // 添加缓存相关headers
            const headers = new Headers(cachedResponse.headers);
            Object.entries(corsHeaders).forEach(([key, value]) => {
              headers.set(key, value);
            });
            
            return new Response(cachedResponse.body, {
              status: cachedResponse.status,
              headers
            });
          } catch (error) {
            console.error('Cache test failed:', error);
            return jsonResponse({
              success: false,
              error: String(error),
              message: `Cache test failed: ${error}`
            }, corsHeaders);
          }
        }
        
        // ===== 列出文件API（调试用） =====
        if (path === '/api/debug/list' && method === 'GET') {
          try {
            const prefix = url.searchParams.get('prefix') || '';
            const limit = parseInt(url.searchParams.get('limit') || '10');
            
            const listed = await env.CHIGUA_MEDIA.list({ prefix, limit });
            
            return jsonResponse({
              success: true,
              files: listed.objects.map(obj => ({
                key: obj.key,
                size: obj.size,
                uploaded: obj.uploaded
              })),
              truncated: listed.truncated
            }, corsHeaders);
          } catch (error) {
            return errorResponse(`列表失败: ${error}`, 500, corsHeaders);
          }
        }

        // ===== 文件上传API =====
        if (path.startsWith('/api/upload')) {
          const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
          const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
          const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey, env);
          
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

      // ===== 文件访问服务（带缓存） =====
      if (path.startsWith('/files/')) {
        const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
        const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
        const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey, env);
        const filePath = decodeURIComponent(path.replace('/files/', ''));
        
        // 从查询参数获取签名信息
        const key = url.searchParams.get('key');
        const signature = url.searchParams.get('signature');
        const expiresStr = url.searchParams.get('expires');
        const downloads = url.searchParams.get('downloads') || '';
        
        // 🔧 特殊处理：HLS 及 video*.jpeg 伪装文件可无签名访问（不影响其他图片的签名和加密策略）
        if (!signature || !expiresStr) {
          const isHlsFile = filePath.endsWith('.ts') || filePath.endsWith('.m3u8') || filePath.endsWith('.key');
          // video*.jpeg 是伪装成 .jpeg 的 MPEG-TS 分片（上传时被图片加密逻辑加密），需要先解密再返回
          const isVideoPreview = /video\d+\.(jpeg|jpg)$/i.test(filePath);
          const canAccessWithoutSignature = isHlsFile || isVideoPreview;
          
          if (canAccessWithoutSignature) {
            // 🔓 video*.jpeg：上传时被加密，必须用 getFile()（自动解密），不能走缓存路径
            if (isVideoPreview) {
              const fileObject = await fileService.getFile(filePath);
              if (!fileObject) {
                return errorResponse('文件不存在', 404, corsHeaders);
              }
              const responseHeaders = new Headers({
                'Content-Type': 'video/mp2t', // 解密后实际内容为 MPEG-TS
                'Cache-Control': 'public, max-age=86400',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
                'Access-Control-Allow-Headers': 'Range',
                'Access-Control-Expose-Headers': 'Content-Length, Content-Range, Accept-Ranges, Content-Type, ETag, Last-Modified',
              });
              return new Response(fileObject.body, {
                status: 200,
                headers: responseHeaders,
              });
            }

            // 🚀 HLS 文件（.ts/.m3u8/.key）走 CDN 缓存
            const queryParams = parseQueryParams(url.searchParams);
            const cachedResponse = await fileService.getFileWithCache(filePath, queryParams, ctx);
            
            if (cachedResponse.status === 404) {
              return errorResponse('文件不存在', 404, corsHeaders);
            }
            
            const getHlsContentType = (filePath: string): string => {
              if (filePath.endsWith('.m3u8')) return 'application/vnd.apple.mpegurl';
              if (filePath.endsWith('.ts')) return 'video/mp2t';
              if (filePath.endsWith('.key')) return 'application/octet-stream';
              return 'application/octet-stream';
            };
            
            const responseHeaders = new Headers(cachedResponse.headers);
            Object.entries(corsHeaders).forEach(([key, value]) => {
              responseHeaders.set(key, value);
            });
            responseHeaders.set('Content-Type', getHlsContentType(filePath));
            if (filePath.endsWith('.m3u8')) {
              responseHeaders.set('Cache-Control', 'public, max-age=21300');
            } else {
              responseHeaders.set('Cache-Control', 'public, max-age=86400');
            }
            responseHeaders.set('Access-Control-Allow-Origin', '*');
            responseHeaders.set('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
            responseHeaders.set('Access-Control-Allow-Headers', 'Range');
            
            return new Response(cachedResponse.body, {
              status: cachedResponse.status,
              headers: responseHeaders
            });
          }
          
          return errorResponse('缺少签名参数', 400, corsHeaders);
        }
        
        const expires = parseInt(expiresStr);
        const actualKey = key || filePath; // 使用key参数或文件路径
        
        const isValidSignature = await verifySignedUrl(actualKey, signature, expires, downloads, secret);
        if (!isValidSignature) {
          return errorResponse('签名验证失败或已过期', 403, corsHeaders);
        }
        
        // 🔐 检查是否请求解密
        const shouldDecrypt = url.searchParams.get('decrypt') === 'true';
        const isPreview = url.searchParams.get('preview') === 'true';
        
        // 🎯 检查是否为 Range 请求（用于视频快进）
        const rangeHeader = request.headers.get('Range');
        
        // ⚠️ decrypt=true 不使用缓存（参考 pornhub-r2-worker 设计）
        // 原因：解密是动态操作，每次都应从R2读取并实时解密
        let fileObject;
        
        // 🚀 Range 请求优先处理（视频快进关键）
        if (rangeHeader) {
          // Range 请求不使用缓存，直接从 R2 读取指定范围
          console.log(`📹 Range request detected: ${rangeHeader} for ${filePath}`);
          
          // 先获取文件元数据以获取总大小
          const fileHead = await env.CHIGUA_MEDIA.head(filePath);
          
          if (!fileHead) {
            return errorResponse('文件不存在', 404, corsHeaders);
          }
          
          const fileSize = fileHead.size;
          
          // 解析 Range 头：bytes=start-end
          const match = rangeHeader.match(/bytes=(\d+)-(\d*)/);
          
          if (!match) {
            return errorResponse('Invalid Range header', 416, corsHeaders);
          }
          
          const start = parseInt(match[1], 10);
          const end = match[2] ? parseInt(match[2], 10) : fileSize - 1;
          
          // 验证范围有效性
          if (start >= fileSize || end >= fileSize || start > end) {
            const headers = new Headers(corsHeaders);
            headers.set('Content-Range', `bytes */${fileSize}`);
            return new Response('Range Not Satisfiable', {
              status: 416,
              headers
            });
          }
          
          // 从 R2 读取指定范围
          const rangedObject = await env.CHIGUA_MEDIA.get(filePath, {
            range: { offset: start, length: end - start + 1 }
          });
          
          if (!rangedObject) {
            return errorResponse('文件不存在', 404, corsHeaders);
          }
          
          // 🎯 智能Content-Type设置
          const getCorrectContentType = (filePath: string, originalContentType?: string): string => {
            if (filePath.endsWith('.mp4')) return 'video/mp4';
            if (filePath.endsWith('.webm')) return 'video/webm';
            if (filePath.endsWith('.m3u8')) return 'application/vnd.apple.mpegurl';
            if (filePath.endsWith('.ts')) return 'video/mp2t';
            return originalContentType || 'application/octet-stream';
          };
          
          // 返回 206 Partial Content
          const rangeHeaders = new Headers(corsHeaders);
          rangeHeaders.set('Content-Type', getCorrectContentType(filePath, fileHead.httpMetadata?.contentType));
          rangeHeaders.set('Content-Length', (end - start + 1).toString());
          rangeHeaders.set('Content-Range', `bytes ${start}-${end}/${fileSize}`);
          rangeHeaders.set('Accept-Ranges', 'bytes');
          rangeHeaders.set('Cache-Control', 'public, max-age=3600');
          rangeHeaders.set('ETag', fileHead.etag);
          
          console.log(`✅ Range response: ${start}-${end}/${fileSize}`);
          
          return new Response(rangedObject.body, {
            status: 206,
            headers: rangeHeaders
          });
        }
        
        // 非 Range 请求的正常处理流程
        if (shouldDecrypt || isPreview) {
          // 🔓 请求解密：直接从R2读取并解密，不使用缓存
          console.log(`🔓 Decrypt request, bypassing cache: ${filePath}`);
          fileObject = await fileService.getFile(filePath);
        } else {
          // 🚀 普通请求：优先尝试缓存
          const queryParams = parseQueryParams(url.searchParams);
          const cachedResponse = await fileService.getFileWithCache(filePath, queryParams, ctx);
          
          if (cachedResponse.status === 200) {
            // 缓存命中！添加CORS头并返回
            const headers = new Headers(cachedResponse.headers);
            Object.entries(corsHeaders).forEach(([key, value]) => {
              headers.set(key, value);
            });
            
            return new Response(cachedResponse.body, {
              status: cachedResponse.status,
              headers
            });
          }
          
          // 缓存未命中，从R2读取
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

        // 🎯 根据文件扩展名自动设置缓存策略（根据签名有效期调整）
        // 🔐 重要：缓存时间不能超过签名有效期，避免签名过期后CDN仍返回缓存内容
        // 签名有效期：图片2小时、M3U8 6小时、其他3600小时
        const getCacheControl = (filePath: string): string => {
          const lowerPath = filePath.toLowerCase();
          if (lowerPath.endsWith('.webp') || lowerPath.endsWith('.jpg') || lowerPath.endsWith('.jpeg') || 
              lowerPath.endsWith('.png') || lowerPath.endsWith('.gif') || lowerPath.endsWith('.svg')) {
            return 'public, max-age=6900'; // 图片：1小时55分钟（签名2小时，留5分钟安全边界）
          } else if (lowerPath.endsWith('.m3u8')) {
            return 'public, max-age=21300'; // HLS播放列表：5小时55分钟（签名6小时，留5分钟安全边界）
          } else if (lowerPath.endsWith('.ts')) {
            return 'public, max-age=86400'; // HLS分片：1天（TS文件通常不需要签名，可以长期缓存）
          } else if (lowerPath.endsWith('.mp4') || lowerPath.endsWith('.webm') || lowerPath.endsWith('.avi')) {
            return 'public, max-age=3600'; // 视频：1小时（签名3600小时，缓存时间可以更长）
          } else {
            // 尝试从type参数获取（向后兼容）
            const fileType = url.searchParams.get('type');
            if (fileType === 'image') {
              return 'public, max-age=6900'; // 1小时55分钟
            } else if (fileType === 'video') {
              return 'public, max-age=3600'; // 1小时
            }
            return 'public, max-age=3600'; // 默认：1小时
          }
        };

        // 根据是否解密设置不同的响应头
        if (shouldDecrypt || isPreview) {
          // 🔓 解密模式：返回正确的content-type
          const correctContentType = getCorrectContentType(filePath, fileObject.httpMetadata?.contentType);
          headers.set('Content-Type', correctContentType);
          headers.set('X-Decrypted', 'true');
          headers.set('X-Cache-Status', 'BYPASS'); // 标记跳过缓存
          // 解密模式不设置Cache-Control（不缓存）
        } else {
          // 加密模式或普通文件
          const isEncrypted = fileObject.customMetadata?.encrypted === 'true';
          
          if (isEncrypted) {
            // 返回加密标识
            headers.set('Content-Type', 'application/x-chigua-encrypted');
            headers.set('X-Encrypted', 'true');
            const originalContentType = fileObject.customMetadata?.contentType || fileObject.httpMetadata?.contentType;
            const correctContentType = getCorrectContentType(filePath, originalContentType);
            headers.set('X-Original-Content-Type', correctContentType);
          } else {
            // 未加密文件正常返回
            const correctContentType = getCorrectContentType(filePath, fileObject.httpMetadata?.contentType);
            headers.set('Content-Type', correctContentType);
          }
          
          // 🚀 设置缓存控制（在响应创建前设置）
          const cacheControl = getCacheControl(filePath);
          headers.set('Cache-Control', cacheControl);
          
          // 视频文件支持断点续传
          if (filePath.toLowerCase().endsWith('.mp4') || filePath.toLowerCase().endsWith('.ts') || 
              filePath.toLowerCase().endsWith('.webm')) {
            headers.set('Accept-Ranges', 'bytes');
          }
        }
        
        headers.set('Content-Length', fileObject.size.toString());
        headers.set('ETag', fileObject.etag);
        
        // 🚀 设置缓存状态（在响应创建前设置）
        if (!shouldDecrypt && !isPreview) {
          headers.set('X-Cache-Status', 'MISS'); // 标记为缓存未命中（从R2读取）
        }
        
        // 创建响应
        const response = new Response(fileObject.body, { headers });
        
        // 🚀 只有非decrypt请求才存入缓存
        if (!shouldDecrypt && !isPreview) {
          const contentType = headers.get('Content-Type') || 'application/octet-stream';
          const queryParams = parseQueryParams(url.searchParams);
          
          // 异步存入缓存（不阻塞响应）
          await fileService.cacheService.put(
            filePath,
            response.clone(),
            contentType,
            fileObject.size,
            queryParams,
            ctx
          );
        }
        
        return response;
      }
      
      // ===== 转码系统路径兼容性处理（带缓存） =====
      // 支持转码系统直接生成的路径格式（不带/files/前缀）
      if (path.startsWith('/videos/') || path.startsWith('/images/') || path.startsWith('/documents/')) {
        const secret = env.SIGNATURE_SECRET || 'pornhub-r2-worker-secret-key-2025';
        const encryptionKey = env.IMAGE_ENCRYPTION_KEY || '';
        const fileService = new FileService(env.CHIGUA_MEDIA, url.origin, secret, encryptionKey, env);
        const filePath = decodeURIComponent(path.substring(1)); // 移除开头的 /
        
        console.log('🎬 转码系统路径访问:', filePath);
        
        // 🎯 检查是否为 Range 请求（支持 MP4 快进）
        const rangeHeader = request.headers.get('Range');
        const isVideoFile = filePath.endsWith('.mp4') || filePath.endsWith('.webm');
        
        if (rangeHeader && isVideoFile) {
          console.log(`📹 Range request for video: ${rangeHeader} for ${filePath}`);
          
          // 获取文件元数据
          const fileHead = await env.CHIGUA_MEDIA.head(filePath);
          
          if (!fileHead) {
            return errorResponse('视频文件不存在', 404, corsHeaders);
          }
          
          const fileSize = fileHead.size;
          
          // 解析 Range 头
          const match = rangeHeader.match(/bytes=(\d+)-(\d*)/);
          
          if (!match) {
            return errorResponse('Invalid Range header', 416, corsHeaders);
          }
          
          const start = parseInt(match[1], 10);
          const end = match[2] ? parseInt(match[2], 10) : fileSize - 1;
          
          // 验证范围有效性
          if (start >= fileSize || end >= fileSize || start > end) {
            const headers = new Headers(corsHeaders);
            headers.set('Content-Range', `bytes */${fileSize}`);
            return new Response('Range Not Satisfiable', {
              status: 416,
              headers
            });
          }
          
          // 从 R2 读取指定范围
          const rangedObject = await env.CHIGUA_MEDIA.get(filePath, {
            range: { offset: start, length: end - start + 1 }
          });
          
          if (!rangedObject) {
            return errorResponse('视频文件不存在', 404, corsHeaders);
          }
          
          // 返回 206 Partial Content
          const rangeHeaders = new Headers(corsHeaders);
          rangeHeaders.set('Content-Type', filePath.endsWith('.mp4') ? 'video/mp4' : 'video/webm');
          rangeHeaders.set('Content-Length', (end - start + 1).toString());
          rangeHeaders.set('Content-Range', `bytes ${start}-${end}/${fileSize}`);
          rangeHeaders.set('Accept-Ranges', 'bytes');
          rangeHeaders.set('Cache-Control', 'public, max-age=3600');
          rangeHeaders.set('ETag', fileHead.etag);
          
          console.log(`✅ Range response for video: ${start}-${end}/${fileSize}`);
          
          return new Response(rangedObject.body, {
            status: 206,
            headers: rangeHeaders
          });
        }
        
        // 检查是否是HLS相关文件（允许无签名访问）
        const isHlsFile = filePath.endsWith('.ts') || filePath.endsWith('.m3u8') || filePath.endsWith('.key');
        
        if (isHlsFile) {
          // 🚀 HLS文件使用CDN缓存优化
          const queryParams = parseQueryParams(url.searchParams);
          const cachedResponse = await fileService.getFileWithCache(filePath, queryParams, ctx);
          
          // 设置正确的Content-Type
          const getHlsContentType = (filePath: string): string => {
            if (filePath.endsWith('.m3u8')) {
              return 'application/vnd.apple.mpegurl';
            } else if (filePath.endsWith('.ts')) {
              return 'video/mp2t';
            } else if (filePath.endsWith('.key')) {
              return 'application/octet-stream';
            }
            return 'application/octet-stream';
          };
          
          // 构建响应头
          const responseHeaders = new Headers(cachedResponse.headers);
          Object.entries(corsHeaders).forEach(([key, value]) => {
            responseHeaders.set(key, value);
          });
          responseHeaders.set('Content-Type', getHlsContentType(filePath));
          // 🎯 根据HLS文件类型设置缓存时间（根据签名有效期调整）
          // M3U8签名6小时，TS文件通常不需要签名
          if (filePath.endsWith('.m3u8')) {
            responseHeaders.set('Cache-Control', 'public, max-age=21300'); // HLS播放列表：5小时55分钟（签名6小时，留5分钟安全边界）
          } else {
            responseHeaders.set('Cache-Control', 'public, max-age=86400'); // HLS分片：1天（TS文件通常不需要签名）
          }
          responseHeaders.set('Access-Control-Allow-Origin', '*');
          responseHeaders.set('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
          responseHeaders.set('Access-Control-Allow-Headers', 'Range');
          
          return new Response(cachedResponse.body, {
            status: cachedResponse.status,
            headers: responseHeaders
          });
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