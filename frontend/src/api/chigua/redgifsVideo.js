import request from '@/utils/request'

// 查询RedGifs视频列表
export function listRedGifsVideo(query) {
  return request({
    url: '/chigua/redgifs/video/list',
    method: 'get',
    params: query
  })
}

// 根据用户ID查询视频列表
export function listRedGifsVideoByUserId(userId, query) {
  return request({
    url: '/chigua/redgifs/video/list',
    method: 'get',
    params: { ...query, userId: userId }
  })
}

// 查询RedGifs视频详细
export function getRedGifsVideo(id) {
  return request({
    url: '/chigua/redgifs/video/' + id,
    method: 'get'
  })
}

// 新增RedGifs视频
export function addRedGifsVideo(data) {
  return request({
    url: '/chigua/redgifs/video',
    method: 'post',
    data: data
  })
}

// 修改RedGifs视频
export function updateRedGifsVideo(data) {
  return request({
    url: '/chigua/redgifs/video',
    method: 'put',
    data: data
  })
}

// 删除RedGifs视频
export function delRedGifsVideo(id) {
  return request({
    url: '/chigua/redgifs/video/' + id,
    method: 'delete'
  })
}

// 批量删除RedGifs视频
export function delRedGifsVideos(ids) {
  return request({
    url: '/chigua/redgifs/video/' + ids,
    method: 'delete'
  })
}

// 将视频封面同步为用户头像
export function syncPosterToUserAvatar(id) {
  return request({
    url: '/chigua/redgifs/video/' + id + '/sync-poster-to-user',
    method: 'put'
  })
}

// 更新视频状态
export function updateRedGifsVideoStatus(id, status) {
  return request({
    url: '/chigua/redgifs/video/status',
    method: 'put',
    data: {
      id,
      status
    }
  })
}
