import request from './request'

// ============ 首页概览 ============
export function getDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

// ============ 用户管理 ============
export function getUserList(params) {
  return request({
    url: '/admin/user/list',
    method: 'get',
    params
  })
}

export function createUser(data) {
  return request({
    url: '/admin/user',
    method: 'post',
    data
  })
}

export function updateUser(userId, data) {
  return request({
    url: `/admin/user/${userId}`,
    method: 'put',
    data
  })
}

export function deleteUser(userId) {
  return request({
    url: `/admin/user/${userId}`,
    method: 'delete'
  })
}

// 管理员重置指定用户密码（学员/教师等）
export function resetUserPassword(userId, newPassword) {
  return request({
    url: `/admin/user/${userId}/reset-password`,
    method: 'put',
    params: { newPassword }
  })
}

// ============ 课程类别 ============
export function getCategoryList() {
  return request({
    url: '/admin/category/list',
    method: 'get'
  })
}

export function createCategory(data) {
  return request({
    url: '/admin/category',
    method: 'post',
    data
  })
}

export function updateCategory(categoryId, data) {
  return request({
    url: `/admin/category/${categoryId}`,
    method: 'put',
    data
  })
}

export function deleteCategory(categoryId) {
  return request({
    url: `/admin/category/${categoryId}`,
    method: 'delete'
  })
}

// ============ 班级管理 ============
export function getClassList(params) {
  return request({
    url: '/admin/class/list',
    method: 'get',
    params
  })
}

export function createClass(data) {
  return request({
    url: '/admin/class',
    method: 'post',
    data
  })
}

export function updateClass(classId, data) {
  return request({
    url: `/admin/class/${classId}`,
    method: 'put',
    data
  })
}

export function deleteClass(classId) {
  return request({
    url: `/admin/class/${classId}`,
    method: 'delete'
  })
}

// ============ 排课管理 ============
export function getScheduleList(params) {
  return request({
    url: '/admin/schedule/list',
    method: 'get',
    params
  })
}

export function createSchedule(data) {
  return request({
    url: '/admin/schedule',
    method: 'post',
    data
  })
}

export function updateSchedule(scheduleId, data) {
  return request({
    url: `/admin/schedule/${scheduleId}`,
    method: 'put',
    data
  })
}

export function deleteSchedule(scheduleId) {
  return request({
    url: `/admin/schedule/${scheduleId}`,
    method: 'delete'
  })
}

// ============ 报名管理 ============
export function getStudentOptions() {
  return request({
    url: '/admin/enrollment/student-options',
    method: 'get'
  })
}

export function getEnrollmentList(params) {
  return request({
    url: '/admin/enrollment/list',
    method: 'get',
    params
  })
}

export function createEnrollment(data) {
  return request({
    url: '/admin/enrollment',
    method: 'post',
    data
  })
}

export function updateEnrollmentStatus(enrollmentId, status) {
  return request({
    url: `/admin/enrollment/${enrollmentId}/status`,
    method: 'put',
    params: { status }
  })
}

// ============ 考勤管理 ============
export function getAttendanceList(params) {
  return request({
    url: '/admin/attendance/list',
    method: 'get',
    params
  })
}

export function updateAttendance(attendanceId, status) {
  return request({
    url: `/admin/attendance/${attendanceId}`,
    method: 'put',
    params: { status }
  })
}

// ============ 缴费管理 ============
export function getPaymentList(params) {
  return request({
    url: '/admin/payment/list',
    method: 'get',
    params
  })
}

export function createPayment(data) {
  return request({
    url: '/admin/payment',
    method: 'post',
    data
  })
}

export function refundPayment(paymentId, remark) {
  return request({
    url: `/admin/payment/${paymentId}/refund`,
    method: 'post',
    params: { remark }
  })
}

// ============ 财务管理 ============
export function getStudentAccount(studentId) {
  return request({
    url: '/admin/finance/student-account',
    method: 'get',
    params: { studentId }
  })
}

export function getArrearsList() {
  return request({ url: '/admin/finance/arrears', method: 'get' })
}

export function renewPayment(data) {
  return request({ url: '/admin/finance/renew', method: 'post', data })
}

export function getIncomeStat(dimension) {
  return request({
    url: '/admin/finance/income-stat',
    method: 'get',
    params: { dimension }
  })
}

export function getRefundStat() {
  return request({ url: '/admin/finance/refund-stat', method: 'get' })
}

export function getTopStudents(limit = 10) {
  return request({
    url: '/admin/finance/top-students',
    method: 'get',
    params: { limit }
  })
}

// ============ 统计报表 ============
export function getRenewalRate(params) {
  return request({
    url: '/admin/statistics/renewal-rate',
    method: 'get',
    params
  })
}

export function getAttendanceRate(params) {
  return request({
    url: '/admin/statistics/attendance-rate',
    method: 'get',
    params
  })
}

export function getClassroomUtilization(params) {
  return request({
    url: '/admin/statistics/classroom-utilization',
    method: 'get',
    params
  })
}

export function getTeacherHours(params) {
  return request({
    url: '/admin/statistics/teacher-hours',
    method: 'get',
    params
  })
}

// ============ 系统设置 ============
export function getClassroomList() {
  return request({
    url: '/admin/system/classroom/list',
    method: 'get'
  })
}

export function addClassroom(data) {
  return request({
    url: '/admin/system/classroom',
    method: 'post',
    data
  })
}

export function updateClassroom(classroomId, data) {
  return request({
    url: `/admin/system/classroom/${classroomId}`,
    method: 'put',
    data
  })
}

export function deleteClassroom(classroomId) {
  return request({
    url: `/admin/system/classroom/${classroomId}`,
    method: 'delete'
  })
}

// ============ 游客试听预约（CRM线索） ============
export function getVisitorBookingList(params) {
  return request({
    url: '/admin/visitor-booking/list',
    method: 'get',
    params
  })
}

export function handleVisitorBooking(bookingId, status) {
  return request({
    url: `/admin/visitor-booking/${bookingId}/status`,
    method: 'put',
    params: { status }
  })
}

// ============ 班级教师下拉选项 ============
export function getTeacherOptions() {
  return request({
    url: '/admin/class/teacher-options',
    method: 'get'
  })
}

// ============ 系统通用配置 ============
export function getSystemConfig(key) {
  return request({ url: `/admin/system/config/${key}`, method: 'get' })
}
export function saveSystemConfig(key, data) {
  return request({ url: `/admin/system/config/${key}`, method: 'put', data })
}

// ============ 课程产品（类别与课程整合） ============
export function getCourseList(params) {
  return request({ url: '/admin/course/list', method: 'get', params })
}
export function createCourse(data) {
  return request({ url: '/admin/course', method: 'post', data })
}
export function updateCourse(courseId, data) {
  return request({ url: `/admin/course/${courseId}`, method: 'put', data })
}
export function deleteCourse(courseId) {
  return request({ url: `/admin/course/${courseId}`, method: 'delete' })
}

// ============ 师资信息管理 ============
export function getStaffList(params) {
  return request({ url: '/admin/staff/list', method: 'get', params })
}
export function createStaff(data) {
  return request({ url: '/admin/staff', method: 'post', data })
}
export function updateStaff(teacherId, data) {
  return request({ url: `/admin/staff/${teacherId}`, method: 'put', data })
}
export function deleteStaff(teacherId) {
  return request({ url: `/admin/staff/${teacherId}`, method: 'delete' })
}
export function resetStaffPassword(teacherId) {
  return request({ url: `/admin/staff/${teacherId}/reset-password`, method: 'post' })
}
export function staffLeave(teacherId) {
  return request({ url: `/admin/staff/${teacherId}/leave`, method: 'post' })
}

// 教师工资支出
export function getTeacherSalary() {
  return request({ url: '/admin/finance/teacher-salary', method: 'get' })
}

// 教师工资账户明细
export function getTeacherAccount(teacherId) {
  return request({ url: '/admin/finance/teacher-account', method: 'get', params: { teacherId } })
}

export function deleteEnrollment(enrollmentId) {
  return request({ url: `/admin/enrollment/${enrollmentId}`, method: 'delete' })
}

// 调课申请审批
export function getRescheduleList(params) {
  return request({ url: '/admin/schedule/reschedule/list', method: 'get', params })
}
export function approveReschedule(requestId, data) {
  return request({ url: `/admin/schedule/reschedule/${requestId}/approve`, method: 'put', data })
}
export function rejectReschedule(requestId, data) {
  return request({ url: `/admin/schedule/reschedule/${requestId}/reject`, method: 'put', data })
}

