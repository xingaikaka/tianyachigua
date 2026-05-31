import { 
  TranscodeInfo, 
  VideoUrls, 
  BatchUrlsResponse, 
  UrlGenerateRequest,
  ApiResponse 
} from './types';
import { 
  createApiResponse, 
  generateVideoPlayUrl,
  generateImageUrl,
  generateSignedUrl
} from './utils';

export class UrlGenerateService {
  private workerUrl: string;
  private signatureSecret: string;

  constructor(workerUrl: string, signatureSecret: string = 'pornhub-r2-worker-secret-key-2025') {
    this.workerUrl = workerUrl;
    this.signatureSecret = signatureSecret;
  }

  // 根据转码记录信息生成视频URL
  async generateVideoUrls(transcodeInfo: TranscodeInfo): Promise<ApiResponse<VideoUrls>> {
    try {
      const urls: VideoUrls = {};

      // 生成MP4直接播放URL
      if (transcodeInfo.domain && transcodeInfo.path && transcodeInfo.orgfile) {
        urls.videoUrl = generateVideoPlayUrl(
          this.workerUrl, 
          transcodeInfo.mp4domain || transcodeInfo.domain,
          transcodeInfo.path, 
          transcodeInfo.orgfile,
          'video'
        );
      }

      // 生成HLS播放URL
      if (transcodeInfo.domain && transcodeInfo.path && transcodeInfo.orgfile) {
        urls.m3u8Url = generateVideoPlayUrl(
          this.workerUrl,
          transcodeInfo.mp4domain || transcodeInfo.domain,
          transcodeInfo.path,
          transcodeInfo.orgfile,
          'hls'
        );
      }

      // 生成封面图片URL
      if (transcodeInfo.cover_image) {
        urls.coverUrl = generateImageUrl(
          this.workerUrl,
          transcodeInfo.picdomain || transcodeInfo.domain,
          transcodeInfo.cover_image
        );
      } else if (transcodeInfo.thumbnails && Array.isArray(transcodeInfo.thumbnails) && transcodeInfo.thumbnails.length > 0) {
        // 使用第一个缩略图作为封面
        urls.coverUrl = generateImageUrl(
          this.workerUrl,
          transcodeInfo.picdomain || transcodeInfo.domain,
          transcodeInfo.thumbnails[0]
        );
      } else if (transcodeInfo.orgfile) {
        // 生成默认缩略图路径
        const thumbFile = transcodeInfo.orgfile.replace(/\.[^/.]+$/, '_thumb.jpg');
        urls.coverUrl = generateImageUrl(
          this.workerUrl,
          transcodeInfo.picdomain || transcodeInfo.domain,
          `${transcodeInfo.path}/${thumbFile}`
        );
      }

      // 设置推荐的播放URL（优先HLS，其次MP4）
      urls.preferredPlayUrl = urls.m3u8Url || urls.videoUrl;

      return createApiResponse(true, urls, 'URL生成成功');

    } catch (error) {
      console.error('生成视频URL失败:', error);
      return createApiResponse(false, null, '生成视频URL失败', String(error));
    }
  }

  // 批量生成视频URL
  async generateBatchUrls(request: UrlGenerateRequest): Promise<ApiResponse<BatchUrlsResponse>> {
    try {
      const urlsMap: Record<string, VideoUrls> = {};
      
      // 并行生成所有视频的URL
      const promises = request.transcodes.map(async (transcodeInfo) => {
        const result = await this.generateVideoUrls(transcodeInfo);
        if (result.success && result.data) {
          urlsMap[transcodeInfo.transcode_id] = result.data;
        }
      });

      await Promise.all(promises);

      const response: BatchUrlsResponse = {
        urls: urlsMap
      };

      return createApiResponse(true, response, `成功生成${Object.keys(urlsMap).length}个视频的URL`);

    } catch (error) {
      console.error('批量生成视频URL失败:', error);
      return createApiResponse(false, null, '批量生成视频URL失败', String(error));
    }
  }

  // 生成单个资源的访问URL（通用方法）
  async generateResourceUrl(
    domain: string, 
    filePath: string, 
    resourceType: 'video' | 'image' | 'document' = 'video'
  ): Promise<ApiResponse<string>> {
    try {
      let url: string;
      
      switch (resourceType) {
        case 'video':
          // 视频文件直接访问
          url = generateVideoPlayUrl(this.workerUrl, domain, '', filePath, 'video');
          break;
        case 'image':
          url = generateImageUrl(this.workerUrl, domain, filePath);
          break;
                 case 'document':
         default:
           // 文档等其他文件类型
           const expires = Math.floor(Date.now() / 1000) + 3600;
           const signature = generateSignedUrl(`${domain}/${filePath}`, expires);
           url = `${this.workerUrl}/files/${encodeURIComponent(`${domain}/${filePath}`)}?signature=${signature}&expires=${expires}`;
           break;
      }

      return createApiResponse(true, url, 'URL生成成功');

    } catch (error) {
      console.error('生成资源URL失败:', error);
      return createApiResponse(false, null, '生成资源URL失败', String(error));
    }
  }

  // 验证转码记录信息是否完整
  private validateTranscodeInfo(transcodeInfo: TranscodeInfo): { valid: boolean; error?: string } {
    if (!transcodeInfo.transcode_id) {
      return { valid: false, error: '缺少转码ID' };
    }

    if (!transcodeInfo.domain) {
      return { valid: false, error: '缺少域名信息' };
    }

    if (!transcodeInfo.path) {
      return { valid: false, error: '缺少路径信息' };
    }

    if (!transcodeInfo.orgfile) {
      return { valid: false, error: '缺少文件名信息' };
    }

    return { valid: true };
  }
} 