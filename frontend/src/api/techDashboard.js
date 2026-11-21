import axios from 'axios'
import request from './request'
import { useUserStore } from '@/store/user'

// ============ 技术可视化仪表盘 API ============

// 仪表盘总览（存储统计/文档趋势/模型列表/组件列表/拓扑/日志）
export function getDashboardOverview(days = 30) {
  return request({ url: '/dashboard/overview', method: 'get', params: { days } })
}

// 当前角色权限声明
export function getDashboardPermissions() {
  return request({ url: '/dashboard/permissions', method: 'get' })
}

// 开源组件列表（platform/category/updateStatus/keyword 筛选）
export function getDashboardComponents(params) {
  return request({ url: '/dashboard/components', method: 'get', params })
}

// 开源组件详情（版本迭代历史 + 依赖上下游）
export function getComponentDetail(componentId) {
  return request({ url: `/dashboard/components/${componentId}`, method: 'get' })
}

// 触发开源组件同步（管理员）
export function syncComponents() {
  return request({ url: '/dashboard/components/sync', method: 'post', timeout: 60000 })
}

// 模型性能指标曲线
export function getModelMetrics(modelId, limit = 60) {
  return request({ url: `/dashboard/models/${modelId}/metrics`, method: 'get', params: { limit } })
}

// 触发模型更新（登记升级意向，管理员）
export function upgradeModel(modelId, version) {
  return request({ url: `/dashboard/models/${modelId}/upgrade`, method: 'post', params: { version } })
}

// 手动触发一次模型指标采样（管理员）
export function collectMetrics() {
  return request({ url: '/dashboard/metrics/collect', method: 'post', timeout: 30000 })
}

// API 调用与用量（真实访问留痕：流量/分布/端点排行/占比/汇总，管理员）
export function getApiUsage(days = 30) {
  return request({ url: '/dashboard/api-usage', method: 'get', params: { days } })
}

// 数据源连接健康检查（GitHub/Gitee/本地库）
export function getSourcesHealth() {
  return request({ url: '/dashboard/sources/health', method: 'get', timeout: 30000 })
}

// 仪表盘操作日志
export function getDashboardLogs(limit = 20) {
  return request({ url: '/dashboard/logs', method: 'get', params: { limit } })
}

// 导出统计报表（CSV 文件下载，管理员）
export function exportDashboardReport() {
  const userStore = useUserStore()
  return axios.create({ baseURL: '/api', timeout: 60000, responseType: 'blob' })(
    {
      url: '/dashboard/export',
      method: 'get',
      headers: userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}
    }
  )
}
