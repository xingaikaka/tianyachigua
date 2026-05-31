import request from '@/utils/request'

// 查询RedGifs用户列表
export function listRedGifsUser(query) {
  return request({
    url: '/chigua/redgifs/user/list',
    method: 'get',
    params: query
  })
}

// 查询RedGifs用户详细
export function getRedGifsUser(id) {
  return request({
    url: '/chigua/redgifs/user/' + id,
    method: 'get'
  })
}

// 新增RedGifs用户
export function addRedGifsUser(data) {
  return request({
    url: '/chigua/redgifs/user',
    method: 'post',
    data: data
  })
}

// 修改RedGifs用户
export function updateRedGifsUser(data) {
  return request({
    url: '/chigua/redgifs/user',
    method: 'put',
    data: data
  })
}

// 删除RedGifs用户
export function delRedGifsUser(id) {
  return request({
    url: '/chigua/redgifs/user/' + id,
    method: 'delete'
  })
}

// 批量删除RedGifs用户
export function delRedGifsUsers(ids) {
  return request({
    url: '/chigua/redgifs/user/' + ids,
    method: 'delete'
  })
}

// 更新用户状态
export function updateRedGifsUserStatus(id, status) {
  return request({
    url: '/chigua/redgifs/user/status',
    method: 'put',
    data: {
      id,
      status
    }
  })
}

// 更新用户推荐状态
export function updateRedGifsUserRecommended(id, recommended) {
  return request({
    url: '/chigua/redgifs/user/recommended',
    method: 'put',
    data: {
      id,
      recommended
    }
  })
}

// 更新用户排序权重
export function updateRedGifsUserSort(id, sortOrder) {
  return request({
    url: '/chigua/redgifs/user/sort',
    method: 'put',
    data: {
      id,
      sortOrder
    }
  })
}
