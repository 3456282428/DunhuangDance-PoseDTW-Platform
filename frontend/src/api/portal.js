import request from './request'

// 课程列表（游客公开浏览，无需登录）
export function getPortalCourses(params) {
  return request({
    url: '/portal/courses',
    method: 'get',
    params
  })
}

// 课程详情
export function getPortalCourseDetail(classId) {
  return request({
    url: `/portal/course/${classId}`,
    method: 'get'
  })
}

// 课程可预约的排班时间（未来待上课）
export function getPortalCourseSchedules(classId) {
  return request({
    url: `/portal/course/${classId}/schedules`,
    method: 'get'
  })
}

// 提交试听预约
export function submitTrialBooking(data) {
  return request({
    url: '/portal/trial',
    method: 'post',
    data
  })
}

// 按手机号查询我的试听记录
export function getMyTrialBookings(phone) {
  return request({
    url: '/portal/my-bookings',
    method: 'get',
    params: { phone }
  })
}

// 机构公开信息
export function getPortalOrg() {
  return request({
    url: '/portal/org',
    method: 'get'
  })
}

// 门户自定义内容（轮播图/主视觉/特色卡片）
export function getPortalContent() {
  return request({ url: '/portal/content', method: 'get' })
}
