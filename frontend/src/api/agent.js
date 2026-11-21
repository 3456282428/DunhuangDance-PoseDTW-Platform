import request from './request'

// ---- Agent 智能体（三端通用桌宠入口） ----

/** 对话：意图识别 → 工具调用 → 反馈闭环（含敏感操作 confirm 响应） */
export function agentChat(data) {
  return request({ url: '/agent/chat', method: 'post', data, timeout: 120000 })
}

/** 敏感操作确认/拒绝 */
export function agentConfirm(data) {
  return request({ url: '/agent/confirm', method: 'post', data, timeout: 120000 })
}

/** 会话列表（按最后活跃倒序） */
export function listAgentSessions() {
  return request({ url: '/agent/sessions', method: 'get' })
}

/** 新建会话（可不带标题，首条消息自动生成标题） */
export function createAgentSession(data) {
  return request({ url: '/agent/sessions', method: 'post', data: data || {} })
}

/** 会话历史消息（切换会话时回填） */
export function getAgentMessages(sessionId) {
  return request({ url: `/agent/sessions/${sessionId}/messages`, method: 'get' })
}

/** 删除会话（含消息） */
export function deleteAgentSession(sessionId) {
  return request({ url: `/agent/sessions/${sessionId}`, method: 'delete' })
}
