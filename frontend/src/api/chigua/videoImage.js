import request from '@/utils/request'

// 查询视频图片列表
export function listVideoImage(query) {
  return request({
    url: '/chigua/videoImage/list',
    method: 'get',
    params: query
  })
}

// 根据视频ID查询图片列表
export function getImagesByVideoId(videoId) {
  return request({
    url: '/chigua/videoImage/video/' + videoId,
    method: 'get'
  })
}

// 查询视频图片详细
export function getVideoImage(id) {
  return request({
    url: '/chigua/videoImage/' + id,
    method: 'get'
  })
}

// 新增视频图片
export function addVideoImage(data) {
  return request({
    url: '/chigua/videoImage',
    method: 'post',
    data: data
  })
}

// 修改视频图片
export function updateVideoImage(data) {
  return request({
    url: '/chigua/videoImage',
    method: 'put',
    data: data
  })
}

// 删除视频图片
export function delVideoImage(id) {
  return request({
    url: '/chigua/videoImage/' + id,
    method: 'delete'
  })
}

// 设置主图
export function setPrimaryImage(videoId, imageId) {
  return request({
    url: '/chigua/videoImage/setPrimary',
    method: 'put',
    params: { videoId, imageId }
  })
}

// 获取视频的主图
export function getPrimaryImage(videoId) {
  return request({
    url: '/chigua/videoImage/primary/' + videoId,
    method: 'get'
  })
}

// 统计视频图片数量
export function countImages(videoId) {
  return request({
    url: '/chigua/videoImage/count/' + videoId,
    method: 'get'
  })
}

// 批量更新图片排序
export function updateImageSort(images) {
  return request({
    url: '/chigua/videoImage/sort',
    method: 'put',
    data: images
  })
}

// 检查图片URL是否已存在
export function checkImageUrl(imageUrl, videoId, excludeId) {
  return request({
    url: '/chigua/videoImage/checkUrl',
    method: 'get',
    params: { imageUrl, videoId, excludeId }
  })
}

// 批量上传图片
export function batchUploadImages(videoId, imageUrls) {
  return request({
    url: '/chigua/videoImage/batchUpload',
    method: 'post',
    params: { videoId },
    data: imageUrls
  })
} 