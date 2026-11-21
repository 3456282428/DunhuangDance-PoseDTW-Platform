import request from './request'

// ============ 首页概括 ============
export function getTeacherDashboardSummary() {
  return request({
    url: '/teacher/dashboard/summary',
    method: 'get'
  })
}

export function getTeacherTodaySchedule() {
  return request({
    url: '/teacher/dashboard/today-schedule',
    method: 'get'
  })
}

export function getTeacherTodos() {
  return request({
    url: '/teacher/dashboard/todos',
    method: 'get'
  })
}

// ============ 个人授课主页 ============
export function getTeacherProfile() {
  return request({
    url: '/teacher/profile/info',
    method: 'get'
  })
}

export function updateTeacherProfile(data) {
  return request({
    url: '/teacher/profile/info',
    method: 'put',
    data
  })
}

export function getTeacherClasses() {
  return request({
    url: '/teacher/profile/classes',
    method: 'get'
  })
}

// ============ 课程排班管理 ============
export function getTeacherSchedule(params) {
  return request({
    url: '/teacher/schedule/list',
    method: 'get',
    params
  })
}

export function getScheduleDetail(scheduleId) {
  return request({
    url: `/teacher/schedule/${scheduleId}`,
    method: 'get'
  })
}

export function requestReschedule(scheduleId, data) {
  return request({
    url: `/teacher/schedule/${scheduleId}/reschedule-request`,
    method: 'post',
    data
  })
}

export function checkScheduleConflict(scheduleId, params) {
  return request({
    url: '/teacher/schedule/conflict-check',
    method: 'get',
    params: { scheduleId, ...params }
  })
}

export function getMyRescheduleRequests() {
  return request({
    url: '/teacher/schedule/reschedule-requests',
    method: 'get'
  })
}

// ============ 预约学员查询 ============
export function getTeacherAppointments(params) {
  return request({
    url: '/teacher/appointment/list',
    method: 'get',
    params
  })
}

export function confirmAppointment(appointmentId, action) {
  return request({
    url: `/teacher/appointment/${appointmentId}/confirm`,
    method: 'put',
    params: { action }
  })
}

// ============ 教学考勤管理 ============
export function getAttendanceStudents(scheduleId) {
  return request({
    url: `/teacher/attendance/${scheduleId}/students`,
    method: 'get'
  })
}

export function batchUpdateAttendance(scheduleId, data) {
  return request({
    url: `/teacher/attendance/${scheduleId}/batch-update`,
    method: 'post',
    data
  })
}

export function getAttendanceHistory(params) {
  return request({
    url: '/teacher/attendance/history',
    method: 'get',
    params
  })
}

// ============ 作业管理 ============
export function assignHomework(data) {
  return request({
    url: '/teacher/homework/assign',
    method: 'post',
    data
  })
}

export function getTeacherHomeworkList(params) {
  return request({
    url: '/teacher/homework/list',
    method: 'get',
    params
  })
}

export function getHomeworkSubmissions(homeworkId) {
  return request({
    url: `/teacher/homework/${homeworkId}/submissions`,
    method: 'get'
  })
}

export function gradeHomework(homeworkId, data) {
  return request({
    url: `/teacher/homework/${homeworkId}/grade`,
    method: 'put',
    data
  })
}

// 触发学员提交的舞蹈AI智能评分（force=true强制重新评分）
export function triggerAiScore(submissionId, force = false) {
  return request({
    url: `/teacher/homework/${submissionId}/ai-score`,
    method: 'post',
    params: force ? { force: true } : {}
  })
}

// 查询AI智能评分结果
export function getAiScoreResult(submissionId) {
  return request({
    url: `/teacher/homework/${submissionId}/ai-score`,
    method: 'get'
  })
}

// 生成AI教练语义点评（通道二·豆包多模态语义增强；force=true强制重新分析）
// 豆包分析需10~30秒，单独放宽超时避免默认10s超时导致前端先失败
export function triggerAiSemantics(submissionId, force = false) {
  return request({
    url: `/teacher/homework/${submissionId}/ai-semantics`,
    method: 'post',
    params: force ? { force: true } : {},
    timeout: 180000
  })
}

// ============ 教学成果统计 ============
export function getAchievementStats() {
  return request({
    url: '/teacher/achievement/stats',
    method: 'get'
  })
}

export function getExamPassRateByClass() {
  return request({
    url: '/teacher/achievement/exam-pass-rate',
    method: 'get'
  })
}

export function getCompetitionAwardsByClass() {
  return request({
    url: '/teacher/achievement/competition-awards',
    method: 'get'
  })
}

export function getHomeworkCompletionRate() {
  return request({
    url: '/teacher/achievement/homework-completion',
    method: 'get'
  })
}

// ============ 学员进度跟踪 ============
export function getClassStudentsProgress(classId) {
  return request({
    url: `/teacher/student-progress/class/${classId}`,
    method: 'get'
  })
}

export function getStudentProgressDetail(studentId, classId) {
  return request({
    url: `/teacher/student-progress/student/${studentId}`,
    method: 'get',
    params: { classId }
  })
}

export function addProgressComment(studentId, classId, comment) {
  return request({
    url: `/teacher/student-progress/student/${studentId}/comment`,
    method: 'post',
    params: { classId, comment }
  })
}

// ============ 反馈回复处理 ============
export function getTeacherFeedbackList(status) {
  return request({
    url: '/teacher/feedback/list',
    method: 'get',
    params: { status }
  })
}

export function getFeedbackDetail(feedbackId) {
  return request({
    url: `/teacher/feedback/${feedbackId}`,
    method: 'get'
  })
}

export function replyFeedback(feedbackId, replyContent) {
  return request({
    url: `/teacher/feedback/${feedbackId}/reply`,
    method: 'post',
    params: { replyContent }
  })
}

// ============ 学员请假/缓考/补考申请处理 ============
export function getStudentRequests() {
  return request({
    url: '/teacher/request/list',
    method: 'get'
  })
}

export function handleStudentRequest(requestId, action, remark) {
  return request({
    url: `/teacher/request/${requestId}/handle`,
    method: 'put',
    params: { action, remark }
  })
}
