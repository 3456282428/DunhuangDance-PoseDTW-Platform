import request from './request'

// ============ 知识库与AI模型管理模块 API ============

// ---- 管理员端：三端知识库统一管控 ----
export function getKbStats() {
  return request({ url: '/admin/knowledge/stats', method: 'get' })
}

export function getKbPage(scope, params) {
  return request({ url: `/admin/knowledge/${scope}/page`, method: 'get', params })
}

export function getKbDoc(scope, kbId) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}`, method: 'get' })
}

export function createKbDoc(scope, data) {
  return request({ url: `/admin/knowledge/${scope}`, method: 'post', data })
}

export function updateKbDoc(scope, kbId, data) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}`, method: 'put', data })
}

export function deleteKbDoc(scope, kbId) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}`, method: 'delete' })
}

// ---- 跨知识库关联：联动查询 / 关联图谱（可视化） ----
export function getKbRelated(scope, kbId) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}/related`, method: 'get' })
}

export function getKbGraph(scope, kbId) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}/graph`, method: 'get' })
}

// ---- 全局关联图谱（3D建模图谱数据源：实体节点+关联边+维度统计） ----
export function getKbRelationGraph() {
  return request({ url: '/admin/knowledge/relation-graph', method: 'get' })
}

// ---- 条目适配3D模型推荐（随知识库内容自动更新） ----
export function getKbModel3d(scope, kbId) {
  return request({ url: `/admin/knowledge/${scope}/${kbId}/model3d`, method: 'get' })
}

export function rebuildKbModel3d(scope = 'ALL') {
  return request({ url: '/admin/knowledge/model3d/rebuild', method: 'post', params: { scope } })
}

// ---- 管理员端：操作日志 / 备份与恢复 ----
export function getKbLogs(params) {
  return request({ url: '/admin/knowledge/logs', method: 'get', params })
}

export function createKbBackup(scope = 'ALL') {
  return request({ url: '/admin/knowledge/backup', method: 'post', params: { scope } })
}

export function getKbBackups() {
  return request({ url: '/admin/knowledge/backup/list', method: 'get' })
}

export function restoreKbBackup(backupId) {
  return request({ url: `/admin/knowledge/backup/${backupId}/restore`, method: 'post' })
}

// ---- 管理员端：AI模型管理 ----
export function getAiModelList() {
  return request({ url: '/admin/ai-model/list', method: 'get' })
}

export function createAiModel(data) {
  return request({ url: '/admin/ai-model', method: 'post', data })
}

export function updateAiModel(modelId, data) {
  return request({ url: `/admin/ai-model/${modelId}`, method: 'put', data })
}

export function deleteAiModel(modelId) {
  return request({ url: `/admin/ai-model/${modelId}`, method: 'delete' })
}

export function getAiBindingList() {
  return request({ url: '/admin/ai-model/binding/list', method: 'get' })
}

export function createAiBinding(data) {
  return request({ url: '/admin/ai-model/binding', method: 'post', data })
}

export function updateAiBindingEnabled(bindingId, enabled) {
  return request({ url: `/admin/ai-model/binding/${bindingId}/enabled`, method: 'put', params: { enabled } })
}

export function deleteAiBinding(bindingId) {
  return request({ url: `/admin/ai-model/binding/${bindingId}`, method: 'delete' })
}

export function getAiInvokeLogs() {
  return request({ url: '/admin/ai-model/invoke-logs', method: 'get' })
}

// ---- 教师端知识库 ----
export function getTeacherKbPage(params) {
  return request({ url: '/teacher/knowledge/page', method: 'get', params })
}

export function getTeacherKbDoc(kbId) {
  return request({ url: `/teacher/knowledge/${kbId}`, method: 'get' })
}

export function createTeacherKbDoc(data) {
  return request({ url: '/teacher/knowledge', method: 'post', data })
}

export function updateTeacherKbDoc(kbId, data) {
  return request({ url: `/teacher/knowledge/${kbId}`, method: 'put', data })
}

export function deleteTeacherKbDoc(kbId) {
  return request({ url: `/teacher/knowledge/${kbId}`, method: 'delete' })
}

// ---- 学生端知识库（只读） ----
export function getStudentKbPage(params) {
  return request({ url: '/student/knowledge/page', method: 'get', params })
}

export function getStudentKbDoc(kbId) {
  return request({ url: `/student/knowledge/${kbId}`, method: 'get' })
}

// ---- AI统一调用入口（三端通用） ----
export function aiInvoke(data) {
  return request({ url: '/ai/invoke', method: 'post', data })
}
