import request from './request'

// ============ 在学课程 ============
export function getMyCourses() {
  return request({
    url: '/student/course/list',
    method: 'get'
  })
}

export function getCourseSchedule(enrollmentId) {
  return request({
    url: `/student/course/${enrollmentId}/schedule`,
    method: 'get'
  })
}

export function getMyAppointments() {
  return request({
    url: '/student/course/appointments',
    method: 'get'
  })
}

// ============ 我的订单与报名缴费 ============
export function getMyOrders(params) {
  return request({
    url: '/student/order/list',
    method: 'get',
    params
  })
}

export function createOrder(data) {
  return request({
    url: '/student/order/create',
    method: 'post',
    data
  })
}

export function payOrder(paymentId) {
  return request({
    url: `/student/order/${paymentId}/pay`,
    method: 'post'
  })
}

export function applyRefund(paymentId, reason) {
  return request({
    url: `/student/order/${paymentId}/refund`,
    method: 'post',
    params: { reason }
  })
}

// ============ 考勤记录 ============
export function getMyAttendance(params) {
  return request({
    url: '/student/attendance/list',
    method: 'get',
    params
  })
}

export function getMyAttendanceSummary() {
  return request({
    url: '/student/attendance/summary',
    method: 'get'
  })
}

// ============ 作业 ============
export function getMyHomework(params) {
  return request({
    url: '/student/homework/list',
    method: 'get',
    params
  })
}

export function submitHomework(homeworkId, data) {
  return request({
    url: `/student/homework/${homeworkId}/submit`,
    method: 'post',
    data
  })
}

export function getHomeworkDetail(homeworkId) {
  return request({
    url: `/student/homework/${homeworkId}`,
    method: 'get'
  })
}

// 触发我的作业舞蹈AI智能评分
export function triggerAiScore(homeworkId, force = false) {
  return request({
    url: `/student/homework/${homeworkId}/ai-score`,
    method: 'post',
    params: { force }
  })
}

// 查询AI智能评分结果
export function getAiScoreResult(homeworkId) {
  return request({
    url: `/student/homework/${homeworkId}/ai-score`,
    method: 'get'
  })
}

// 生成AI教练语义点评（通道二·豆包多模态语义增强；force=true强制重新分析）
// 豆包分析需10~30秒，单独放宽超时避免默认10s超时导致前端先失败
export function triggerAiSemantics(homeworkId, force = false) {
  return request({
    url: `/student/homework/${homeworkId}/ai-semantics`,
    method: 'post',
    params: force ? { force: true } : {},
    timeout: 180000
  })
}

// ============ 个人信息与进度 ============
export function getMyProfile() {
  return request({
    url: '/student/profile/info',
    method: 'get'
  })
}

export function updateMyProfile(data) {
  return request({
    url: '/student/profile/info',
    method: 'put',
    data
  })
}

export function getLearningProgress() {
  return request({
    url: '/student/profile/progress',
    method: 'get'
  })
}

// ============ 机构咨询与反馈 ============
export function getNewsList() {
  return request({
    url: '/student/profile/news',
    method: 'get'
  })
}

export function getOrganizationInfo() {
  return request({
    url: '/student/profile/organization',
    method: 'get'
  })
}

export function submitFeedback(data) {
  return request({
    url: '/student/profile/feedback',
    method: 'post',
    data
  })
}

export function getMyFeedback() {
  return request({
    url: '/student/profile/feedback/list',
    method: 'get'
  })
}

// ============ 请假/缓考/补考申请 ============
export function submitStudentRequest(data) {
  return request({
    url: '/student/request/submit',
    method: 'post',
    data
  })
}

export function getMyRequests() {
  return request({
    url: '/student/request/list',
    method: 'get'
  })
}
