import request from '@/utils/request'
import { generateResourceUrl as workerGenerateResourceUrl, adaptWorkerResponse } from './worker'

// 查询视频转码记录列表
export function listTranscode(query) {
  return request({
    url: '/chigua/transcode/list',
    method: 'get',
    params: query
  })
}

// 查询未使用的转码记录列表
export function listUnusedTranscodes(query) {
  return request({
    url: '/chigua/transcode/unused',
    method: 'get',
    params: query
  })
}

// 查询可用于富文本选择的转码记录列表
export function listRichTextVideos(query) {
  return request({
    url: '/chigua/transcode/richtext/list',
    method: 'get',
    params: query
  })
}

// 生成视频富文本HTML代码
export function generateVideoHtml(data) {
  return request({
    url: '/chigua/transcode/richtext/generateHtml',
    method: 'post',
    data: data
  })
}

// 查询视频转码记录详细信息
export function getTranscode(id) {
  return request({
    url: '/chigua/transcode/' + id,
    method: 'get'
  })
}

// 根据转码ID获取转码记录详细信息
export function getTranscodeByTranscodeId(transcodeId) {
  return request({
    url: '/chigua/transcode/transcode/' + transcodeId,
    method: 'get'
  })
}

// 新增视频转码记录
export function addTranscode(data) {
  return request({
    url: '/chigua/transcode',
    method: 'post',
    data: data
  })
}

// 修改视频转码记录
export function updateTranscode(data) {
  return request({
    url: '/chigua/transcode',
    method: 'put',
    data: data
  })
}

// 删除视频转码记录
export function delTranscode(ids) {
  return request({
    url: '/chigua/transcode/' + ids,
    method: 'delete'
  })
}

// 标记转码记录为已使用
export function markTranscodeAsUsed(transcodeId, videoId) {
  return request({
    url: `/chigua/transcode/markUsed/${transcodeId}/${videoId}`,
    method: 'put'
  })
}

// 获取转码视频的播放URL（带签名）- 🔧 改为调用Java后端统一生成
export function getVideoUrl(transcodeId) {
  return request({
    url: `/chigua/transcode/api/${transcodeId}/urls`,
    method: 'get'
  })
}

// 获取转码视频预览HTML - 调用Java后端
export function getVideoPreview(transcodeId) {
  return request({
    url: `/chigua/transcode/api/${transcodeId}/preview`,
    method: 'get'
  })
}

// 批量获取转码视频的播放URL（带签名）- 🔧 改为调用Java后端
export function getVideoUrls(transcodeIds) {
  // 并发调用Java后端的单个URL生成接口
  const promises = transcodeIds.map(id => getVideoUrl(id))
  
  return Promise.all(promises).then(responses => {
    // 转换为批量格式
    const urls = {}
    responses.forEach((response, index) => {
      if (response.data) {
        urls[transcodeIds[index]] = response.data
      }
    })
    
    return {
      data: {
        urls: urls
      }
    }
  })
}

// 生成单个资源的签名URL - 🔧 暂时保留调用Worker，因为这是通用资源URL生成
export function getResourceUrl(path, type) {
  // 这个接口用于通用资源URL生成，暂时保留Worker调用
  // 如果需要也可以迁移到Java后端
  return workerGenerateResourceUrl('default.domain.com', path, type).then(adaptWorkerResponse)
}

// 验证URL签名是否有效
export function verifySignature(key, signature, expires, downloads) {
  return request({
    url: '/chigua/transcode/api/verify-signature',
    method: 'get',
    params: { key, signature, expires, downloads }
  })
} 