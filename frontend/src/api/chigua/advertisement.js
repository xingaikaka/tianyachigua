import request from '@/utils/request'

// 查询广告列表
export function listAdvertisement(query) {
  return request({
    url: '/chigua/advertisement/list',
    method: 'get',
    params: query
  })
}

// 查询广告详细
export function getAdvertisement(id) {
  return request({
    url: '/chigua/advertisement/' + id,
    method: 'get'
  })
}

// 新增广告
export function addAdvertisement(data) {
  return request({
    url: '/chigua/advertisement',
    method: 'post',
    data: data
  })
}

// 修改广告
export function updateAdvertisement(data) {
  return request({
    url: '/chigua/advertisement',
    method: 'put',
    data: data
  })
}

// 删除广告
export function delAdvertisement(id) {
  return request({
    url: '/chigua/advertisement/' + id,
    method: 'delete'
  })
}

// 获取分类列表
export function getCategoryList() {
  return request({
    url: '/chigua/advertisement/categoryList',
    method: 'get'
  })
}

// 按位置查询广告
export function getAdvertisementByPosition(position) {
  return request({
    url: '/chigua/advertisement/position/' + position,
    method: 'get'
  })
}

// 按分类查询广告
export function getAdvertisementByCategory(categoryId) {
  return request({
    url: '/chigua/advertisement/category/' + categoryId,
    method: 'get'
  })
}

// 广告点击统计
export function clickAdvertisement(id) {
  return request({
    url: '/chigua/advertisement/click/' + id,
    method: 'post'
  })
}

// 广告展示统计
export function impressionAdvertisement(id) {
  return request({
    url: '/chigua/advertisement/impression/' + id,
    method: 'post'
  })
} 