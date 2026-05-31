import request from '@/utils/request'

// 查询视频列表
export function listVideo(query) {
  return request({
    url: '/chigua/video/list',
    method: 'get',
    params: query
  })
}

// 查询视频详细
export function getVideo(id) {
  return request({
    url: '/chigua/video/' + id,
    method: 'get'
  })
}

// 查询视频原始内容（用于编辑）
export function getVideoRaw(id) {
  return request({
    url: '/chigua/video/' + id + '/raw',
    method: 'get'
  })
}

// 获取视频处理后的富文本内容（用于显示）
export function getVideoProcessedContent(id) {
  return request({
    url: '/chigua/video/' + id + '/processed-content',
    method: 'get'
  })
}

// 新增视频
export function addVideo(data) {
  return request({
    url: '/chigua/video',
    method: 'post',
    data: data
  })
}

// 修改视频
export function updateVideo(data) {
  return request({
    url: '/chigua/video',
    method: 'put',
    data: data
  })
}

// 删除视频
export function delVideo(id) {
  return request({
    url: '/chigua/video/' + id,
    method: 'delete'
  })
}

// 获取所有分类列表
export function getCategories() {
  return request({
    url: '/chigua/video/categories',
    method: 'get'
  })
}

// 获取所有标签列表
export function getTags() {
  return request({
    url: '/chigua/video/tags',
    method: 'get'
  })
}

// 分页获取标签列表
export function getTagsPage(query) {
  return request({
    url: '/chigua/video/tags/page',
    method: 'get',
    params: query
  })
}

// 根据ID列表获取标签信息
export function getTagsByIds(tagIds) {
  return request({
    url: '/chigua/video/tags/byIds',
    method: 'post',
    data: tagIds,
    // 该接口在进入编辑页时可能会被快速连续调用，用于补全已选标签名称
    // 禁用前端防重复提交校验，避免出现“数据正在处理，请勿重复提交”提示
    headers: { repeatSubmit: false }
  })
}

// 搜索标签
export function searchTags(keyword) {
  return request({
    url: '/chigua/video/searchTags',
    method: 'get',
    params: { keyword }
  })
}

// 根据已选标签推荐相关标签
export function recommendTags(selectedTagIds) {
  return request({
    url: '/chigua/video/recommendTags',
    method: 'post',
    data: selectedTagIds
  })
}

// 创建新标签
export function createTag(data) {
  return request({
    url: '/chigua/video/createTag',
    method: 'post',
    data: data
  })
}

// 获取视频的分类列表
export function getVideoCategories(id) {
  return request({
    url: '/chigua/video/' + id + '/categories',
    method: 'get'
  })
}

// 获取视频的标签列表
export function getVideoTags(id) {
  return request({
    url: '/chigua/video/' + id + '/tags',
    method: 'get'
  })
}

// 批量修改视频状态
export function updateVideoStatus(data) {
  return request({
    url: '/chigua/video/status',
    method: 'put',
    data: data
  })
}

// 批量设置推荐状态
export function updateVideoRecommended(data) {
  return request({
    url: '/chigua/video/recommended',
    method: 'put',
    data: data
  })
}

// 批量设置热门状态
export function updateVideoHot(data) {
  return request({
    url: '/chigua/video/hot',
    method: 'put',
    data: data
  })
}

// 批量设置分类
export function updateVideoCategory(data) {
  return request({
    url: '/chigua/video/category',
    method: 'put',
    data: data
  })
}

// 批量添加标签
export function addVideoTags(data) {
  return request({
    url: '/chigua/video/addTags',
    method: 'put',
    data: data
  })
}

// 批量移除标签
export function removeVideoTags(data) {
  return request({
    url: '/chigua/video/removeTags',
    method: 'put',
    data: data
  })
}

// 更新视频统计数据
export function updateVideoStatistics(data) {
  return request({
    url: '/chigua/video/statistics',
    method: 'put',
    data: data
  })
}

// 更新视频内容
export function updateVideoContent(data) {
  return request({
    url: '/chigua/video/content',
    method: 'put',
    data: data
  })
}

// 更新视频富文本中的视频URL签名
export function updateVideoContentSignatures(id) {
  return request({
    url: `/chigua/video/updateContentSignatures/${id}`,
    method: 'put'
  })
}

// 批量更新视频富文本中的视频URL签名
export function batchUpdateVideoContentSignatures(data) {
  return request({
    url: '/chigua/video/batchUpdateContentSignatures',
    method: 'put',
    data: data
  })
}

// 批量修改视频主分类
export function updateVideoPrimaryCategory(data) {
  return request({
    url: '/chigua/video/primaryCategory',
    method: 'put',
    data: data
  })
}

// 批量替换标签（清空原标签后设置新标签）
export function replaceVideoTags(data) {
  return request({
    url: '/chigua/video/replaceTags',
    method: 'put',
    data: data
  })
} 