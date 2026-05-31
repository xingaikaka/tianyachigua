import { R2Bucket } from '@cloudflare/workers-types';
import { 
  FileUploadRequest, 
  FileUploadResponse, 
  FileAccessRequest,
  RichTextUploadResponse,
  ApiResponse,
  FileTypeConfig,
  WorkerEnv
} from './types';
import { 
  createApiResponse, 
  generateFileKey,
  getFileExtension,
  isValidFileType,
  getCurrentTimestamp,
  generateSignedUrl
} from './utils';
import { ImageEncryption, EncryptionConfig, EncryptedFileMetadata } from './imageEncryption';
import { SecureCacheService } from './secureCacheService';
import { buildCacheConfig } from './cacheConfig';

export class FileService {
  private r2Bucket: R2Bucket;
  private workerUrl: string;
  private signatureSecret: string;
  private imageEncryption: ImageEncryption;
  public cacheService: SecureCacheService; // 公开以便外部访问
  
  // 文件类型配置
  private fileTypeConfigs: Record<string, FileTypeConfig> = {
    image: {
      allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'],
      maxSize: 10 * 1024 * 1024, // 10MB
      mimeTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml', 'image/bmp']
    },
    video: {
      allowedExtensions: ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv', 'm4v', 'm3u8', 'ts', 'key'],
      maxSize: 500 * 1024 * 1024, // 500MB
      mimeTypes: ['video/mp4', 'video/avi', 'video/quicktime', 'video/webm', 'video/x-msvideo', 'application/x-mpegURL', 'application/vnd.apple.mpegurl', 'video/mp2t', 'application/octet-stream']
    },
    document: {
      allowedExtensions: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'],
      maxSize: 50 * 1024 * 1024, // 50MB
      mimeTypes: ['application/pdf', 'application/msword', 'text/plain']
    },
    audio: {
      allowedExtensions: ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a'],
      maxSize: 100 * 1024 * 1024, // 100MB
      mimeTypes: ['audio/mpeg', 'audio/wav', 'audio/ogg', 'audio/aac']
    }
  };

  constructor(
    r2Bucket: R2Bucket, 
    workerUrl: string, 
    signatureSecret: string = 'pornhub-r2-worker-secret-key-2025',
    encryptionMasterKey: string = '',
    env?: WorkerEnv
  ) {
    this.r2Bucket = r2Bucket;
    this.workerUrl = workerUrl;
    this.signatureSecret = signatureSecret;
    
    // 初始化图片加密模块
    const encryptionConfig: EncryptionConfig = {
      enabled: encryptionMasterKey.length >= 32,
      masterKey: encryptionMasterKey || 'default-chigua-encryption-key-2025-must-be-32-chars-minimum',
      algorithm: 'AES-GCM',
      keyLength: 256
    };
    
    this.imageEncryption = new ImageEncryption(encryptionConfig);
    
    // 验证加密配置
    const validation = this.imageEncryption.validateConfig();
    if (!validation.valid) {
      console.warn('⚠️ 图片加密配置无效:', validation.error);
    } else {
      console.log('🔐 图片加密模块已启用');
    }
    
    // 初始化缓存服务
    const cacheConfig = env ? buildCacheConfig(env) : { enabled: false, defaultTTL: 3600, maxCacheSize: 10485760, cacheableTypes: [], excludePatterns: [], ttlStrategies: {} };
    this.cacheService = new SecureCacheService(cacheConfig);
  }

  // 文件上传
  async uploadFile(file: File, fileType: string, customKey?: string): Promise<ApiResponse<FileUploadResponse>> {
    try {
      // 验证文件类型
      const validation = this.validateFile(file, fileType);
      if (!validation.valid) {
        return createApiResponse(false, null, validation.error!, 'INVALID_FILE');
      }

      // 生成文件路径
      const filePath = customKey || this.generateFilePath(file, fileType);
      
      // 🔐 检查是否需要加密
      const shouldEncrypt = this.imageEncryption.shouldEncrypt(filePath, file.type);
      let fileContentToUpload: ReadableStream | ArrayBuffer;
      let encryptionMetadata: EncryptedFileMetadata = { encrypted: false };
      let finalContentType = file.type;

      if (shouldEncrypt) {
        console.log('🔐 文件需要加密:', filePath);
        // 读取文件内容进行加密
        const fileBuffer = await file.arrayBuffer();
        const encryptionResult = await this.imageEncryption.encryptFile(fileBuffer, filePath);
        
        fileContentToUpload = encryptionResult.encryptedContent;
        encryptionMetadata = encryptionResult.metadata;
        
        // 加密后的文件使用特殊的Content-Type来标识
        finalContentType = 'application/x-chigua-encrypted';
        console.log('✅ 文件加密完成:', filePath);
      } else {
        fileContentToUpload = file.stream();
      }
      
      // 准备元数据（包含加密信息）
      const metadata = {
        originalName: file.name,
        fileType: fileType,
        uploadedAt: getCurrentTimestamp(),
        contentType: file.type, // 保存原始content-type
        size: file.size.toString(),
        // 加密相关元数据
        encrypted: encryptionMetadata.encrypted.toString(),
        encryptionAlgorithm: encryptionMetadata.algorithm || '',
        encryptionKeyDerivation: encryptionMetadata.keyDerivation || '',
        encryptionIV: encryptionMetadata.iv || ''
      };

      // 上传到R2
      const result = await this.r2Bucket.put(filePath, fileContentToUpload, {
        httpMetadata: {
          contentType: finalContentType,
          cacheControl: 'public, max-age=31536000' // 1年缓存
        },
        customMetadata: metadata
      });

      // 生成预览URL（临时签名URL）
      const previewUrl = await this.generatePreviewUrl(filePath);

      const response: FileUploadResponse = {
        success: true,
        filePath: filePath,
        fileName: file.name,
        size: file.size,
        contentType: file.type,
        previewUrl: previewUrl,
        etag: result.etag
      };

      return createApiResponse(true, response, '文件上传成功');

    } catch (error) {
      console.error('文件上传失败:', error);
      return createApiResponse(false, null, '文件上传失败', String(error));
    }
  }

  // 富文本专用上传
  async uploadForRichText(file: File): Promise<ApiResponse<RichTextUploadResponse>> {
    try {
      // 自动检测文件类型
      const fileType = this.detectFileType(file);
      
      // 使用通用上传
      const uploadResult = await this.uploadFile(file, fileType);
      
      if (!uploadResult.success || !uploadResult.data) {
        return createApiResponse(false, null, uploadResult.message || '上传失败', uploadResult.error);
      }

      // 转换为富文本格式
      const richTextResponse: RichTextUploadResponse = {
        success: true,
        resourceKey: uploadResult.data.filePath,
        url: uploadResult.data.previewUrl || '',
        type: fileType
      };

      console.log('🎯 Worker richtext响应:', richTextResponse);
      return createApiResponse(true, richTextResponse, '富文本文件上传成功');

    } catch (error) {
      console.error('富文本文件上传失败:', error);
      return createApiResponse(false, null, '富文本文件上传失败', String(error));
    }
  }

  // 获取文件（用于文件访问服务，支持自动解密）
  async getFile(filePath: string): Promise<R2ObjectBody | null> {
    try {
      const fileObject = await this.r2Bucket.get(filePath);
      if (!fileObject) {
        return null;
      }

      // 🔓 检查是否需要解密
      const encryptionMetadata = this.parseEncryptionMetadata(fileObject.customMetadata);
      
      if (encryptionMetadata.encrypted) {
        console.log('🔓 检测到加密文件，开始解密:', filePath);
        
        // 读取加密内容
        const encryptedContent = await fileObject.arrayBuffer();
        
        // 解密文件内容
        const decryptedContent = await this.imageEncryption.decryptFile(
          encryptedContent, 
          filePath, 
          encryptionMetadata
        );
        
        // 创建解密后的响应对象
        const decryptedResponse = new Response(decryptedContent, {
          headers: {
            'Content-Type': fileObject.customMetadata?.contentType || 'application/octet-stream',
            'Content-Length': decryptedContent.byteLength.toString(),
          }
        });
        
        // 构造类似R2ObjectBody的对象
        // 🔓 更新 customMetadata，标记为已解密
        const updatedMetadata = {
          ...fileObject.customMetadata,
          encrypted: 'false',  // 标记为已解密
          decrypted: 'true',   // 添加解密标记
          originallyEncrypted: 'true' // 保留原始加密状态信息
        };
        
        const decryptedBody = {
          body: decryptedResponse.body,
          bodyUsed: false,
          arrayBuffer: () => Promise.resolve(decryptedContent),
          text: () => new TextDecoder().decode(decryptedContent),
          json: () => JSON.parse(new TextDecoder().decode(decryptedContent)),
          blob: () => Promise.resolve(new Blob([decryptedContent])),
          httpMetadata: {
            contentType: fileObject.customMetadata?.contentType || 'application/octet-stream',
            cacheControl: fileObject.httpMetadata?.cacheControl,
            contentDisposition: fileObject.httpMetadata?.contentDisposition,
            contentEncoding: fileObject.httpMetadata?.contentEncoding,
            contentLanguage: fileObject.httpMetadata?.contentLanguage,
          },
          customMetadata: updatedMetadata, // 使用更新后的 metadata
          range: fileObject.range,
          checksums: fileObject.checksums,
          etag: fileObject.etag,
          size: decryptedContent.byteLength,
          version: fileObject.version,
          uploaded: fileObject.uploaded,
          writeHttpMetadata: fileObject.writeHttpMetadata
        } as R2ObjectBody;
        
        console.log('✅ 文件解密完成:', filePath);
        return decryptedBody;
      }
      
      // 文件未加密，直接返回
      return fileObject;
    } catch (error) {
      console.error('获取文件失败:', error);
      return null;
    }
  }

  // 获取加密文件（不解密，用于强保护模式）
  async getEncryptedFile(filePath: string): Promise<R2ObjectBody | null> {
    try {
      const fileObject = await this.r2Bucket.get(filePath);
      if (!fileObject) {
        return null;
      }

      // 直接返回加密文件，不进行解密
      console.log('🔐 返回加密数据（强保护模式）:', filePath);
      return fileObject;
    } catch (error) {
      console.error('获取加密文件失败:', error);
      return null;
    }
  }

  // 解析加密元数据
  private parseEncryptionMetadata(customMetadata?: Record<string, string>): EncryptedFileMetadata {
    if (!customMetadata) {
      return { encrypted: false };
    }

    return {
      encrypted: customMetadata.encrypted === 'true',
      algorithm: customMetadata.encryptionAlgorithm || undefined,
      keyDerivation: customMetadata.encryptionKeyDerivation || undefined,
      iv: customMetadata.encryptionIV || undefined
    };
  }

  // 检查文件是否存在
  async fileExists(filePath: string): Promise<boolean> {
    try {
      const object = await this.r2Bucket.head(filePath);
      return object !== null;
    } catch (error) {
      return false;
    }
  }

  // 批量删除文件
  async deleteFiles(filePaths: string[]): Promise<ApiResponse<number>> {
    try {
      const deletePromises = filePaths.map(path => this.r2Bucket.delete(path));
      await Promise.all(deletePromises);
      
      return createApiResponse(true, filePaths.length, `成功删除${filePaths.length}个文件`);
    } catch (error) {
      console.error('批量删除文件失败:', error);
      return createApiResponse(false, null, '批量删除文件失败', String(error));
    }
  }

  // 获取文件信息
  async getFileInfo(filePath: string): Promise<ApiResponse<any>> {
    try {
      const object = await this.r2Bucket.head(filePath);
      
      if (!object) {
        return createApiResponse(false, null, '文件不存在', 'FILE_NOT_FOUND');
      }

      const info = {
        size: object.size,
        etag: object.etag,
        lastModified: object.uploaded,
        contentType: object.httpMetadata?.contentType,
        customMetadata: object.customMetadata
      };

      return createApiResponse(true, info, '获取文件信息成功');
    } catch (error) {
      console.error('获取文件信息失败:', error);
      return createApiResponse(false, null, '获取文件信息失败', String(error));
    }
  }

  // 私有方法

  // 验证文件
  private validateFile(file: File, fileType: string): { valid: boolean; error?: string } {
    const config = this.fileTypeConfigs[fileType];
    
    if (!config) {
      return { valid: false, error: `不支持的文件类型: ${fileType}` };
    }

    // 检查文件大小
    if (file.size > config.maxSize) {
      const maxSizeMB = Math.round(config.maxSize / 1024 / 1024);
      return { valid: false, error: `文件大小超过限制 (${maxSizeMB}MB)` };
    }

    // 检查文件扩展名
    const extension = getFileExtension(file.name);
    if (!config.allowedExtensions.includes(extension)) {
      return { valid: false, error: `不支持的文件格式: ${extension}` };
    }

    // 检查MIME类型
    if (file.type && !config.mimeTypes.some(mime => file.type.startsWith(mime.split('/')[0]))) {
      return { valid: false, error: `不支持的MIME类型: ${file.type}` };
    }

    return { valid: true };
  }

  // 生成文件路径
  private generateFilePath(file: File, fileType: string): string {
    const timestamp = Date.now();
    const randomId = Math.random().toString(36).substring(2, 15);
    const extension = getFileExtension(file.name);
    const dateFolder = new Date().toISOString().slice(0, 10).replace(/-/g, '/');
    
    return `${fileType}s/${dateFolder}/${timestamp}_${randomId}.${extension}`;
  }

  // 自动检测文件类型
  private detectFileType(file: File): string {
    const extension = getFileExtension(file.name);
    const mimeType = file.type;

    // 根据扩展名和MIME类型检测
    for (const [type, config] of Object.entries(this.fileTypeConfigs)) {
      if (config.allowedExtensions.includes(extension) || 
          config.mimeTypes.some(mime => mimeType.startsWith(mime.split('/')[0]))) {
        return type;
      }
    }

    return 'document'; // 默认为文档类型
  }

  // 生成预览URL
  private async generatePreviewUrl(filePath: string): Promise<string> {
    // 生成24小时有效的预览URL
    const expires = Math.floor(Date.now() / 1000) + 24 * 3600;
    const downloads = '5'; // 图片默认5次下载
    const signature = await generateSignedUrl(filePath, expires, downloads, this.signatureSecret);
    
    let previewUrl = `${this.workerUrl}/files/${encodeURIComponent(filePath)}?key=${filePath}&signature=${signature}&expires=${expires}&downloads=${downloads}`;
    
    // 🔐 为图片类型添加解密参数，确保后台管理系统可以直接显示
    if (this.isImageFile(filePath)) {
      previewUrl += '&decrypt=true';
      console.log('🔐 为图片添加解密参数:', filePath);
    }
    
    console.log('🔗 生成预览URL:', { filePath, workerUrl: this.workerUrl, previewUrl });
    return previewUrl;
  }

  // 检查是否为图片文件
  private isImageFile(filePath: string): boolean {
    const extension = getFileExtension(filePath).toLowerCase();
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'];
    return imageExtensions.includes(extension);
  }

  // ===== 缓存相关方法 =====

  /**
   * 带缓存的文件获取
   * 
   * 核心流程：
   * 1. 查询缓存
   * 2. 缓存命中 → 直接返回
   * 3. 缓存未命中 → 从R2读取 → 存入缓存 → 返回
   * 
   * @param filePath 文件路径
   * @param queryParams 查询参数
   * @param ctx ExecutionContext（用于异步缓存操作）
   */
  async getFileWithCache(
    filePath: string,
    queryParams: Record<string, string> = {},
    ctx?: ExecutionContext
  ): Promise<Response> {
    // 1. 查询缓存
    const cachedResponse = await this.cacheService.get(filePath, queryParams);
    if (cachedResponse) {
      return cachedResponse; // 缓存命中
    }
    
    // 2. 从 R2 读取
    const object = await this.r2Bucket.get(filePath);
    
    if (!object) {
      return new Response(JSON.stringify({
        success: false,
        error: 'FILE_NOT_FOUND',
        message: `文件不存在: ${filePath}`
      }), {
        status: 404,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    // 3. 构建响应
    const contentType = object.httpMetadata?.contentType || 'application/octet-stream';
    const headers = new Headers({
      'Content-Type': contentType,
      'Content-Length': object.size.toString(),
      'ETag': object.httpEtag,
      'Last-Modified': object.uploaded.toUTCString(),
      'X-Cache-Status': 'MISS'
    });

    const response = new Response(object.body, {
      status: 200,
      headers
    });

    // 4. 异步存入缓存（不阻塞响应）
    await this.cacheService.put(
      filePath,
      response.clone(),
      contentType,
      object.size,
      queryParams,
      ctx
    );

    return response;
  }

  /**
   * 删除单个文件（同时清除缓存）
   * 
   * @param filePath 文件路径
   */
  async deleteFile(filePath: string): Promise<ApiResponse<any>> {
    try {
      // 1. 删除 R2 文件
      await this.r2Bucket.delete(filePath);
      
      // 2. 清除缓存
      await this.cacheService.invalidate(filePath);
      
      return createApiResponse(true, null, '文件删除成功');
    } catch (error) {
      console.error('删除文件失败:', error);
      return createApiResponse(false, null, '文件删除失败', String(error));
    }
  }

  /**
   * 获取缓存统计信息
   */
  getCacheStats() {
    return this.cacheService.getStats();
  }

  /**
   * 重置缓存统计
   */
  resetCacheStats() {
    this.cacheService.resetStats();
  }
} 