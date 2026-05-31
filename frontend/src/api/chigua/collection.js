import request from '@/utils/request'

// 查询合集列表
export function listCollection(query) {
  return request({
    url: '/chigua/collection/list',
    method: 'get',
    params: query
  })
}

// 查询合集详细
export function getCollection(id) {
  return request({
    url: '/chigua/collection/' + id,
    method: 'get'
  })
}

// 新增合集
export function addCollection(data) {
  return request({
    url: '/chigua/collection',
    method: 'post',
    data: data
  })
}

// 修改合集
export function updateCollection(data) {
  return request({
    url: '/chigua/collection',
    method: 'put',
    data: data
  })
}

// 删除合集
export function delCollection(id) {
  return request({
    url: '/chigua/collection/' + id,
    method: 'delete'
  })
}

// 查询合集中的视频列表
export function getCollectionVideos(collectionId) {
  return request({
    url: '/chigua/collection/' + collectionId + '/videos',
    method: 'get'
  })
}

// 查询不在指定合集中的视频列表
export function getAvailableVideos(collectionId, query) {
  return request({
    url: '/chigua/collection/' + collectionId + '/available-videos',
    method: 'get',
    params: query
  })
}

// 添加视频到合集
export function addVideosToCollection(collectionId, videoIds) {
  return request({
    url: '/chigua/collection/' + collectionId + '/videos',
    method: 'post',
    data: videoIds
  })
}

// 从合集中移除视频
export function removeVideosFromCollection(collectionId, videoIds) {
  return request({
    url: '/chigua/collection/' + collectionId + '/videos',
    method: 'delete',
    data: videoIds
  })
}

// 更新合集中视频的排序
export function updateVideoSort(collectionId, videoSortData) {
  return request({
    url: '/chigua/collection/' + collectionId + '/videos/sort',
    method: 'put',
    data: videoSortData
  })
}

// 查询所有分类列表
export function getAllCategories() {
  return request({
    url: '/chigua/collection/categories',
    method: 'get'
  })
}

// 批量修改合集状态
export function updateCollectionStatus(status, ids) {
  return request({
    url: '/chigua/collection/status/' + status,
    method: 'put',
    data: ids
  })
}

// 更新合集观看次数
export function updateViewCount(collectionId) {
  return request({
    url: '/chigua/collection/' + collectionId + '/view',
    method: 'post'
  })
} 