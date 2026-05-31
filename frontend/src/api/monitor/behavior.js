import request from '@/utils/request'

// 概览
export function getOverview(params) {
  return request({
    url: '/monitor/behavior/overview',
    method: 'get',
    params
  })
}

// IP 维度聚合列表
export function listIp(params) {
  return request({
    url: '/monitor/behavior/ip/list',
    method: 'get',
    params
  })
}

// 单 IP 的事件时间线
export function ipTimeline(ip, params) {
  return request({
    url: `/monitor/behavior/ip/${encodeURIComponent(ip)}/timeline`,
    method: 'get',
    params
  })
}

// 原始事件流
export function listEvent(params) {
  return request({
    url: '/monitor/behavior/event/list',
    method: 'get',
    params
  })
}
