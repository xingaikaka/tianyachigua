import request from '@/utils/request'

// 查询标签列表
export function listTag(query) {
  return request({
    url: '/chigua/tag/list',
    method: 'get',
    params: query
  })
}

// 查询标签详细
export function getTag(id) {
  return request({
    url: '/chigua/tag/' + id,
    method: 'get'
  })
}

// 新增标签
export function addTag(data) {
  return request({
    url: '/chigua/tag',
    method: 'post',
    data: data
  })
}

// 修改标签
export function updateTag(data) {
  return request({
    url: '/chigua/tag',
    method: 'put',
    data: data
  })
}

// 删除标签
export function delTag(id) {
  return request({
    url: '/chigua/tag/' + id,
    method: 'delete'
  })
}

// 导出标签
export function exportTag(query) {
  return request({
    url: '/chigua/tag/export',
    method: 'post',
    params: query
  })
} 