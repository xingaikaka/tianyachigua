import request from '@/utils/request'

// 查询分类视频排序列表
export function listCategoryVideoSort(query) {
  return request({
    url: '/chigua/categoryVideoSort/list',
    method: 'get',
    params: query
  })
}

// 根据分类ID查询排序视频列表
export function listByCategory(categoryId) {
  return request({
    url: `/chigua/categoryVideoSort/listByCategory/${categoryId}`,
    method: 'get'
  })
}

// 获取分类下可添加排序的视频列表
export function getAvailableVideos(categoryId, params = {}) {
  return request({
    url: `/chigua/categoryVideoSort/availableVideos/${categoryId}`,
    method: 'get',
    params: {
      title: params.title,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 30
    }
  })
}

// 获取所有分类列表
export function getCategories() {
  return request({
    url: '/chigua/categoryVideoSort/categories',
    method: 'get'
  })
}

// 查询分类视频排序详细
export function getCategoryVideoSort(id) {
  return request({
    url: `/chigua/categoryVideoSort/${id}`,
    method: 'get'
  })
}

// 新增分类视频排序
export function addCategoryVideoSort(data) {
  return request({
    url: '/chigua/categoryVideoSort',
    method: 'post',
    data: data
  })
}

// 修改分类视频排序
export function updateCategoryVideoSort(data) {
  return request({
    url: '/chigua/categoryVideoSort',
    method: 'put',
    data: data
  })
}

// 批量设置分类视频排序
export function batchSetCategoryVideoSort(data) {
  return request({
    url: '/chigua/categoryVideoSort/batchSet',
    method: 'post',
    data: data
  })
}

// 删除分类视频排序
export function delCategoryVideoSort(id) {
  return request({
    url: `/chigua/categoryVideoSort/${id}`,
    method: 'delete'
  })
}

// 移除分类视频排序
export function removeCategoryVideoSort(categoryId, videoId) {
  return request({
    url: `/chigua/categoryVideoSort/remove/${categoryId}/${videoId}`,
    method: 'delete'
  })
}
