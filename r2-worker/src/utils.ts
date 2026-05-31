import { ApiResponse } from './types';

// 生成唯一ID
export function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2);
}

// 生成文件键
export function generateFileKey(originalName: string): string {
  const timestamp = Date.now();
  const randomId = Math.random().toString(36).substring(2, 15);
  const extension = getFileExtension(originalName);
  return `${timestamp}_${randomId}.${extension}`;
}

// 获取当前时间戳
export function getCurrentTimestamp(): string {
  return new Date().toISOString();
}

// 创建API响应
export function createApiResponse<T>(
  success: boolean,
  data: T | null,
  message?: string,
  error?: string
): ApiResponse<T> {
  return {
    success,
    data,
    message,
    error,
  };
}

// 解析URL查询参数
export function parseQueryParams(input: URL | URLSearchParams): Record<string, string> {
  const params: Record<string, string> = {};
  const searchParams = input instanceof URL ? input.searchParams : input;
  searchParams.forEach((value, key) => {
    params[key] = value;
  });
  return params;
}

// 解析文件扩展名
export function getFileExtension(filename: string): string {
  return filename.split('.').pop()?.toLowerCase() || '';
}

// 检查是否为视频文件
export function isVideoFile(filename: string): boolean {
  const videoExtensions = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv', 'm4v', 'm3u8', 'ts', 'key'];
  const ext = getFileExtension(filename);
  return videoExtensions.includes(ext);
}

// 检查是否为图片文件
export function isImageFile(filename: string): boolean {
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'];
  const ext = getFileExtension(filename);
  return imageExtensions.includes(ext);
}

// 检查是否为文档文件
export function isDocumentFile(filename: string): boolean {
  const documentExtensions = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'];
  const ext = getFileExtension(filename);
  return documentExtensions.includes(ext);
}

// 检查是否为音频文件
export function isAudioFile(filename: string): boolean {
  const audioExtensions = ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a'];
  const ext = getFileExtension(filename);
  return audioExtensions.includes(ext);
}

// 验证文件类型
export function isValidFileType(filename: string, allowedTypes: string[]): boolean {
  const ext = getFileExtension(filename);
  return allowedTypes.includes(ext);
}

// 生成HMAC-SHA256签名
export async function generateSignedUrl(filePath: string, expires: number, downloads: string = '', secret: string): Promise<string> {
  const data = `${filePath}:${expires}:${downloads}`;
  return await generateHMAC(data, secret);
}

// 生成HMAC-SHA256签名
export async function generateHMAC(data: string, secret: string): Promise<string> {
  // 将密钥和数据转换为ArrayBuffer
  const keyBuffer = new TextEncoder().encode(secret);
  const dataBuffer = new TextEncoder().encode(data);

  // 导入密钥
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBuffer,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );

  // 生成签名
  const signature = await crypto.subtle.sign('HMAC', cryptoKey, dataBuffer);
  
  // 转换为hex字符串并截取前16位（保持URL友好的长度）
  const hashArray = Array.from(new Uint8Array(signature));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  
  // 返回前16位作为签名，足够安全且URL友好
  return hashHex.substring(0, 16);
}

// 验证签名URL
export async function verifySignedUrl(filePath: string, signature: string, expires: number, downloads: string = '', secret: string): Promise<boolean> {
  try {
    // 检查是否过期
    const currentTime = Math.floor(Date.now() / 1000);
    if (currentTime > expires) {
      return false;
    }

    // 重新生成签名并比较
    const expectedSignature = await generateSignedUrl(filePath, expires, downloads, secret);
    return signature === expectedSignature;
  } catch (error) {
    return false;
  }
}

// 格式化文件大小
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 格式化时长（秒转换为可读格式）
export function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);
  
  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  } else {
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}

// 生成视频播放URL（基于转码记录信息）
export async function generateVideoPlayUrl(workerUrl: string, domain: string, path: string, filename: string, type: 'video' | 'hls' = 'video', secret: string = 'pornhub-r2-worker-secret-key-2025'): Promise<string> {
  let filePath: string;
  
  if (type === 'hls') {
    // HLS文件路径：替换扩展名为.m3u8
    const hlsFilename = filename.replace(/\.[^/.]+$/, '.m3u8');
    filePath = `${domain}/${path}/${hlsFilename}`;
  } else {
    // 视频文件路径
    filePath = `${domain}/${path}/${filename}`;
  }
  
  // 生成签名
  const expires = Math.floor(Date.now() / 1000) + 3600; // 1小时过期
  const downloads = type === 'hls' ? '' : '3'; // HLS无限制，视频3次
  const signature = await generateSignedUrl(filePath, expires, downloads, secret);
  
  return `${workerUrl}/files/${encodeURIComponent(filePath)}?key=${filePath}&signature=${signature}&expires=${expires}&downloads=${downloads}&type=${type}`;
}

// 生成图片访问URL
export async function generateImageUrl(workerUrl: string, domain: string, imagePath: string, secret: string = 'pornhub-r2-worker-secret-key-2025'): Promise<string> {
  const filePath = imagePath.startsWith('http') ? imagePath : `${domain}/${imagePath}`;
  
  const expires = Math.floor(Date.now() / 1000) + 24 * 3600; // 24小时过期
  const downloads = '5'; // 图片5次下载
  const signature = await generateSignedUrl(filePath, expires, downloads, secret);
  
  return `${workerUrl}/files/${encodeURIComponent(filePath)}?key=${filePath}&signature=${signature}&expires=${expires}&downloads=${downloads}&type=image`;
}

// JSON响应辅助函数
export function jsonResponse(data: any, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  });
}

// 错误响应辅助函数
export function errorResponse(message: string, status: number = 400, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify({
    success: false,
    error: message,
    message: message
  }), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  });
} 