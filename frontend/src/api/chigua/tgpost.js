import request from '@/utils/request'

/** 帖子列表（分页） */
export function listTgPost(query) {
  return request({ url: '/chigua/tgpost/list', method: 'get', params: query })
}

/** 查询某帖子的媒体列表 */
export function getTgMedia(postId) {
  return request({ url: `/chigua/tgpost/media/${postId}`, method: 'get' })
}

/** 修改帖子（状态/排序等） */
export function updateTgPost(data) {
  return request({ url: '/chigua/tgpost', method: 'put', data })
}

/** 删除帖子（含媒体，支持逗号分隔多个） */
export function delTgPost(ids) {
  return request({ url: `/chigua/tgpost/${ids}`, method: 'delete' })
}

/** 删除单条媒体 */
export function delTgMedia(mediaId) {
  return request({ url: `/chigua/tgpost/media/${mediaId}`, method: 'delete' })
}

/** 切换帖子置顶状态 */
export function toggleTgPostTop(id, isTop) {
  return request({ url: `/chigua/tgpost/top/${id}`, method: 'put', params: { isTop } })
}

/** 切换媒体推荐状态 */
export function toggleTgMediaRecommend(id, isRecommend) {
  return request({ url: `/chigua/tgpost/media/recommend/${id}`, method: 'put', params: { isRecommend } })
}
