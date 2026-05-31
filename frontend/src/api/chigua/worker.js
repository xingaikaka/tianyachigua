import axios from 'axios'

// Worker API基础URL
const WORKER_BASE_URL = 'https://chigua-r2-worker.xingaikaka.workers.dev'

// 创建Worker专用的axios实例
const workerRequest = axios.create({
  baseURL: WORKER_BASE_URL,
  timeout: 30000, // Worker可能需要更长时间处理文件
  headers: {
    'Content-Type': 'application/json'
  }
})

// ===== 文件上传API =====

// 通用文件上传
export function uploadFile(formData) {
  return workerRequest.post('/api/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 富文本专用文件上传
export function uploadForRichText(formData) {
  return workerRequest.post('/api/upload/richtext', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 批量删除文件
export function deleteFiles(filePaths) {
  return workerRequest.post('/api/upload/batch-delete', { filePaths })
}

// 获取文件信息
export function getFileInfo(filePath) {
  return workerRequest.get(`/api/upload/info/${encodeURIComponent(filePath)}`)
}

// ===== URL生成API =====

// 生成单个视频URL
export function generateVideoUrls(transcodeInfo) {
  return workerRequest.post('/api/urls/video', transcodeInfo)
}

// 批量生成视频URL
export function generateBatchUrls(transcodes) {
  return workerRequest.post('/api/urls/batch', { transcodes })
}

// 生成通用资源URL
export function generateResourceUrl(domain, filePath, resourceType) {
  return workerRequest.post('/api/urls/resource', {
    domain,
    filePath,
    resourceType
  })
}

// ===== 工具函数 =====

// 根据转码记录信息调用Worker生成URL
export function getWorkerVideoUrls(transcodeRecords) {
  // 转换Java后端的转码记录格式为Worker需要的格式
  const transcodes = transcodeRecords.map(record => ({
    id: record.id.toString(),
    transcode_id: record.transcodeId || record.id.toString(),
    domain: record.domain || 'default.domain.com',
    picdomain: record.picdomain,
    mp4domain: record.mp4domain,
    path: record.path || '',
    orgfile: record.orgfile || '',
    suffix: record.suffix,
    cover_image: record.coverImage,
    thumbnails: record.thumbnails,
    resolution: record.resolution,
    duration: record.duration
  }))
  
  return generateBatchUrls(transcodes)
}

// 处理Worker API响应，适配前端格式
export function adaptWorkerResponse(workerResponse) {
  // Worker返回格式：{ success: boolean, data: any, message: string }
  // 前端期望格式：{ data: { urls: {} } }
  if (workerResponse.data && workerResponse.data.success) {
    return {
      data: workerResponse.data.data,
      message: workerResponse.data.message
    }
  } else {
    throw new Error(workerResponse.data?.message || 'Worker API调用失败')
  }
} 