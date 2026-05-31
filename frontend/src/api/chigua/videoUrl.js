import request from '@/utils/request'

// 查询视频地址列表
export function listVideoUrl(query) {
  return request({
    url: '/chigua/videoUrl/list',
    method: 'get',
    params: query
  })
}

// 根据视频ID查询地址列表
export function getUrlsByVideoId(videoId) {
  return request({
    url: '/chigua/videoUrl/video/' + videoId,
    method: 'get'
  })
}

// 查询视频地址详细
export function getVideoUrl(id) {
  return request({
    url: '/chigua/videoUrl/' + id,
    method: 'get'
  })
}

// 新增视频地址
export function addVideoUrl(data) {
  return request({
    url: '/chigua/videoUrl',
    method: 'post',
    data: data
  })
}

// 修改视频地址
export function updateVideoUrl(data) {
  return request({
    url: '/chigua/videoUrl',
    method: 'put',
    data: data
  })
}

// 删除视频地址
export function delVideoUrl(id) {
  return request({
    url: '/chigua/videoUrl/' + id,
    method: 'delete'
  })
}

// 设置主要地址
export function setPrimaryUrl(videoId, urlId) {
  return request({
    url: '/chigua/videoUrl/setPrimary',
    method: 'put',
    params: { videoId, urlId }
  })
}

// 获取视频的主要地址
export function getPrimaryUrl(videoId) {
  return request({
    url: '/chigua/videoUrl/primary/' + videoId,
    method: 'get'
  })
}

// 统计视频地址数量
export function countUrls(videoId) {
  return request({
    url: '/chigua/videoUrl/count/' + videoId,
    method: 'get'
  })
}

// 批量更新地址排序
export function updateUrlSort(urls) {
  return request({
    url: '/chigua/videoUrl/sort',
    method: 'put',
    data: urls
  })
}

// 检查播放地址是否已存在
export function checkVideoUrl(videoUrl, videoId, excludeId) {
  return request({
    url: '/chigua/videoUrl/checkUrl',
    method: 'get',
    params: { videoUrl, videoId, excludeId }
  })
}

// 更新播放次数
export function incrementPlayCount(id) {
  return request({
    url: '/chigua/videoUrl/play/' + id,
    method: 'put'
  })
}

// 根据清晰度查询地址列表
export function getUrlsByQuality(videoId, quality) {
  return request({
    url: '/chigua/videoUrl/quality',
    method: 'get',
    params: { videoId, quality }
  })
}

// 根据格式查询地址列表
export function getUrlsByFormat(videoId, format) {
  return request({
    url: '/chigua/videoUrl/format',
    method: 'get',
    params: { videoId, format }
  })
}

// 批量导入视频地址
export function batchImportUrls(videoId, urls) {
  return request({
    url: '/chigua/videoUrl/batchImport',
    method: 'post',
    params: { videoId },
    data: urls
  })
}

// 测试链接可用性
export function testVideoUrl(videoUrl) {
  return request({
    url: '/chigua/videoUrl/test',
    method: 'get',
    params: { videoUrl }
  })
} 