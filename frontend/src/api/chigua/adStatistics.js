import request from '@/utils/request'

// 查询广告统计列表
export function listAdStatistics(query) {
  return request({
    url: '/chigua/advertisement/statistics/list',
    method: 'get',
    params: query
  })
}

// 查询广告统计详细
export function getAdStatistics(id) {
  return request({
    url: '/chigua/advertisement/statistics/' + id,
    method: 'get'
  })
}

// 新增广告统计
export function addAdStatistics(data) {
  return request({
    url: '/chigua/advertisement/statistics',
    method: 'post',
    data: data
  })
}

// 修改广告统计
export function updateAdStatistics(data) {
  return request({
    url: '/chigua/advertisement/statistics',
    method: 'put',
    data: data
  })
}

// 删除广告统计
export function delAdStatistics(id) {
  return request({
    url: '/chigua/advertisement/statistics/' + id,
    method: 'delete'
  })
}

// 获取时间段内有记录的广告名列表（用于筛选下拉）
export function getAdNames(startDate, endDate) {
  return request({
    url: '/chigua/advertisement/statistics/ad-names',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 获取广告每日点击详情
export function getClickDetails(adId, startDate, endDate) {
  return request({
    url: '/chigua/advertisement/statistics/details/' + adId,
    method: 'get',
    params: { startDate, endDate }
  })
}

// 图表：每日点击/曝光趋势
export function getChartTrend(startDate, endDate) {
  return request({
    url: '/chigua/advertisement/statistics/chart/trend',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 图表：按广告类型聚合点击量
export function getChartByType(startDate, endDate) {
  return request({
    url: '/chigua/advertisement/statistics/chart/by-type',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 图表：按广告位置聚合点击量
export function getChartByPosition(startDate, endDate) {
  return request({
    url: '/chigua/advertisement/statistics/chart/by-position',
    method: 'get',
    params: { startDate, endDate }
  })
}

// 图表：Top N 广告点击排行
export function getChartTopAds(startDate, endDate, limit = 10) {
  return request({
    url: '/chigua/advertisement/statistics/chart/top-ads',
    method: 'get',
    params: { startDate, endDate, limit }
  })
}
