import request from './request'

export function getMessageList() {
  return request({ url: '/message/list', method: 'get' })
}

export function getUnreadCount() {
  return request({ url: '/message/unread-count', method: 'get' })
}

export function markMessageRead(messageId) {
  return request({ url: `/message/${messageId}/read`, method: 'put' })
}

export function markAllMessageRead() {
  return request({ url: '/message/read-all', method: 'put' })
}
