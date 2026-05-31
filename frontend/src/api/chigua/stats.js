import request from '@/utils/request'

const statsAdminHeaders = () => {
  const token = process.env.VUE_APP_STATS_ADMIN_TOKEN
  return token ? { 'X-Admin-Token': token } : {}
}

// 获取统计概览
export function getStatsOverview(date = 'today') {
  return request({
    url: '/admin/stats/overview',
    method: 'get',
    params: { date }
  })
}

// 别名，保持向后兼容
export const getOverview = getStatsOverview

// 获取Top视频列表
export function getTopVideos(params) {
  return request({
    url: '/admin/stats/videos/top',
    method: 'get',
    params
  })
}

// 获取分类点击排行
export function getCategoryRanking(period = 'day', limit = 10) {
  return request({
    url: '/admin/stats/category/ranking',
    method: 'get',
    params: { period, limit }
  })
}

// 获取时序数据
export function getTimeSeries(metric, period = 'day', points) {
  return request({
    url: '/admin/stats/timeseries',
    method: 'get',
    params: { metric, period, points }
  })
}

// 手动触发数据聚合
export function triggerAggregate(date) {
  return request({
    url: '/admin/stats/aggregate',
    method: 'post',
    params: { date },
    headers: statsAdminHeaders()
  })
}

// ========== 新增Top100相关统计接口 ==========

// 获取Top100综合统计面板
export function getTop100Dashboard(startDate, endDate) {
  return request({
    url: '/admin/stats/top100/dashboard',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 获取活跃视频统计
export function getActiveVideoStats(date) {
  return request({
    url: '/admin/stats/videos/active',
    method: 'get',
    params: { date }
  })
}



// 获取分类播放量统计
// 注：该接口前端目前未独立调用，分类播放统计通过 getTop100Dashboard 接口的 categoryStats 字段获取
export function getCategoryPlayStats(date) {
  return request({
    url: '/admin/stats/category/plays',
    method: 'get',
    params: { date }
  })
}

// 获取快速上升视频排行
export function getRisingVideos(date, limit = 20) {
  return request({
    url: '/admin/stats/videos/rising',
    method: 'get',
    params: { date, limit }
  })
}

// 清理7天前的播放记录
export function cleanupPlayRecords() {
  return request({
    url: '/admin/stats/cleanup/play-records',
    method: 'post',
    headers: statsAdminHeaders()
  })
}

// 获取每日播放趋势（固定7天）
export function getDailyPlayTrend() {
  return request({
    url: '/admin/stats/daily-play-trend',
    method: 'get'
  })
}

// 获取留存统计数据
export function getRetentionData(params) {
  return request({
    url: '/admin/stats/retention',
    method: 'get',
    params
  })
}

// 获取搜索关键词排行
// params: { date?, startDate?, endDate?, limit? }
// date 单日（优先 Redis），startDate+endDate 日期范围（MySQL聚合）
export function getSearchKeywords(params) {
  return request({
    url: '/admin/stats/search/keywords',
    method: 'get',
    params
  })
}

// 获取24小时活跃时段分布
// params: { date? } 默认今日
export function getHourlyActivity(date) {
  return request({
    url: '/admin/stats/hourly-activity',
    method: 'get',
    params: date ? { date } : {}
  })
}

// 获取零结果搜索关键词排行
// params: { date?, limit? }
export function getNoResultKeywords(params) {
  return request({
    url: '/admin/stats/search/no-result-keywords',
    method: 'get',
    params
  })
}

// 获取内容健康度分布
// params: { days? } 默认 30 天
export function getContentHealth(days) {
  return request({
    url: '/admin/stats/content/health',
    method: 'get',
    params: days ? { days } : {}
  })
}
