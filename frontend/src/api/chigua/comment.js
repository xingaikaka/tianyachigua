import request from '@/utils/request'

// 查询评论列表
export function listComment(query) {
  return request({
    url: '/system/comment/list',
    method: 'get',
    params: query
  })
}

// 查询待审核评论列表
export function listPendingComment(query) {
  return request({
    url: '/system/comment/pending',
    method: 'get',
    params: query
  })
}

// 查询评论详细
export function getComment(id) {
  return request({
    url: '/system/comment/' + id,
    method: 'get'
  })
}

// 获取评论统计信息
export function getCommentStatistics(query) {
  return request({
    url: '/system/comment/statistics',
    method: 'get',
    params: query
  })
}

// 新增评论
export function addComment(data) {
  return request({
    url: '/system/comment',
    method: 'post',
    data: data
  })
}

// 修改评论
export function updateComment(data) {
  return request({
    url: '/system/comment',
    method: 'put',
    data: data
  })
}

// 删除评论
export function delComment(id) {
  return request({
    url: '/system/comment/' + id,
    method: 'delete'
  })
}

// 审核评论
export function auditComment(id, data) {
  return request({
    url: '/system/comment/' + id + '/audit',
    method: 'put',
    data: data
  })
}

// 批量审核评论
export function batchAuditComment(data) {
  return request({
    url: '/system/comment/batch-audit',
    method: 'put',
    data: data
  })
}

// 置顶/取消置顶评论
export function stickyComment(id, data) {
  return request({
    url: '/system/comment/' + id + '/sticky',
    method: 'put',
    data: data
  })
}

// 导出评论
export function exportComment(query) {
  return request({
    url: '/system/comment/export',
    method: 'post',
    params: query
  })
}

// ================ Web端API ================

// 根据视频ID查询已通过的评论列表
export function getCommentsByVideoId(videoId) {
  return request({
    url: '/web/api/comment/video/' + videoId,
    method: 'get'
  })
}

// 提交评论
export function submitComment(data) {
  return request({
    url: '/web/api/comment/submit',
    method: 'post',
    data: data
  })
} 