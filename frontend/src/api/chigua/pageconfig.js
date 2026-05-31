import request from '@/utils/request'

// 查询页面配置列表
export function listPageConfig(query) {
  return request({
    url: '/chigua/pageconfig/list',
    method: 'get',
    params: query
  })
}

// 查询页面配置详细
export function getPageConfig(configId) {
  return request({
    url: '/chigua/pageconfig/' + configId,
    method: 'get'
  })
}

// 根据配置键值查询页面配置
export function getPageConfigByKey(configKey) {
  return request({
    url: '/chigua/pageconfig/key/' + configKey,
    method: 'get'
  })
}

// 根据配置类型查询页面配置列表
export function getPageConfigsByType(configType) {
  return request({
    url: '/chigua/pageconfig/type/' + configType,
    method: 'get'
  })
}

// 根据配置分类查询页面配置列表
export function getPageConfigsByCategory(configCategory) {
  return request({
    url: '/chigua/pageconfig/category/' + configCategory,
    method: 'get'
  })
}

// 新增页面配置
export function addPageConfig(data) {
  return request({
    url: '/chigua/pageconfig',
    method: 'post',
    data: data
  })
}

// 修改页面配置
export function updatePageConfig(data) {
  return request({
    url: '/chigua/pageconfig',
    method: 'put',
    data: data
  })
}

// 删除页面配置
export function delPageConfig(configIds) {
  return request({
    url: '/chigua/pageconfig/' + configIds,
    method: 'delete'
  })
}

// 获取当前配置版本号
export function getCurrentVersion() {
  return request({
    url: '/chigua/pageconfig/version',
    method: 'get'
  })
}

// 强制刷新配置缓存
export function refreshCache() {
  return request({
    url: '/chigua/pageconfig/refresh',
    method: 'post'
  })
}

// 清理配置缓存
export function clearCache() {
  return request({
    url: '/chigua/pageconfig/clear',
    method: 'post'
  })
}