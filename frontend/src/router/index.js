import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/portal'
  },
  {
    path: '/portal',
    name: 'Portal',
    component: () => import('@/views/portal/PortalHome.vue'),
    meta: { title: '艺培学堂 · 课程门户' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '首页概览' } },
      { path: 'user', name: 'AdminUser', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户与角色管理' } },
      { path: 'category', name: 'AdminCategory', component: () => import('@/views/admin/CourseCategory.vue'), meta: { title: '课程产品配置' } },
      { path: 'class', name: 'AdminClass', component: () => import('@/views/admin/CourseClass.vue'), meta: { title: '班级管理' } },
      { path: 'staff', name: 'AdminStaff', component: () => import('@/views/admin/StaffManage.vue'), meta: { title: '师资信息管理' } },
      { path: 'schedule', name: 'AdminSchedule', component: () => import('@/views/admin/ScheduleManage.vue'), meta: { title: '课程排班管理' } },
      { path: 'enrollment', name: 'AdminEnrollment', component: () => import('@/views/admin/EnrollmentManage.vue'), meta: { title: '报名管理' } },
      { path: 'payment', name: 'AdminPayment', component: () => import('@/views/admin/PaymentManage.vue'), meta: { title: '订单与财务' } },
      { path: 'visitor', name: 'AdminVisitor', component: () => import('@/views/admin/VisitorBooking.vue'), meta: { title: '游客试听预约' } },
      { path: 'exam', name: 'AdminExam', component: () => import('@/views/admin/ExamManage.vue'), meta: { title: '考级比赛管理' } },
      { path: 'statistics', name: 'AdminStatistics', component: () => import('@/views/admin/Statistics.vue'), meta: { title: '运营数据统计' } },
      { path: 'knowledge', name: 'AdminKnowledge', component: () => import('@/views/admin/KnowledgeManage.vue'), meta: { title: '知识库与AI模型管理' } },
      { path: 'ai-model', name: 'AdminAiModel', component: () => import('@/views/admin/AiModelManage.vue'), meta: { title: 'AI模型管理' } },
      { path: 'system', name: 'AdminSystem', component: () => import('@/views/admin/SystemConfig.vue'), meta: { title: '系统基础设置' } }
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', name: 'TeacherDashboard', component: () => import('@/views/teacher/Dashboard.vue'), meta: { title: '教师首页' } },
      { path: 'profile', name: 'TeacherProfile', component: () => import('@/views/teacher/Profile.vue'), meta: { title: '个人授课主页' } },
      { path: 'schedule', name: 'TeacherSchedule', component: () => import('@/views/teacher/Schedule.vue'), meta: { title: '课程排班管理' } },
      { path: 'appointment', name: 'TeacherAppointment', component: () => import('@/views/teacher/Appointment.vue'), meta: { title: '预约学员查询' } },
      { path: 'attendance', name: 'TeacherAttendance', component: () => import('@/views/teacher/Attendance.vue'), meta: { title: '教学考勤管理' } },
      { path: 'homework', name: 'TeacherHomework', component: () => import('@/views/teacher/Homework.vue'), meta: { title: '作业管理' } },
      { path: 'homework/ai-report/:submissionId', name: 'TeacherAiReport', component: () => import('@/views/teacher/AiReportPage.vue'), meta: { title: 'AI评分报告' } },
      { path: 'exam', name: 'TeacherExam', component: () => import('@/views/teacher/Exam.vue'), meta: { title: '考级与比赛' } },
      { path: 'achievement', name: 'TeacherAchievement', component: () => import('@/views/teacher/Achievement.vue'), meta: { title: '教学成果统计' } },
      { path: 'progress', name: 'TeacherProgress', component: () => import('@/views/teacher/StudentProgress.vue'), meta: { title: '学员进度跟踪' } },
      { path: 'knowledge', name: 'TeacherKnowledge', component: () => import('@/views/teacher/Knowledge.vue'), meta: { title: '教学知识库' } },
      { path: 'feedback', name: 'TeacherFeedback', component: () => import('@/views/teacher/Feedback.vue'), meta: { title: '反馈回复处理' } }
    ]
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/courses' },
      { path: 'courses', name: 'StudentCourses', component: () => import('@/views/student/Schedule.vue'), meta: { title: '我的课表' } },
      { path: 'appointments', name: 'StudentAppointments', component: () => import('@/views/student/MyAppointments.vue'), meta: { title: '我的预约' } },
      { path: 'orders', name: 'StudentOrders', component: () => import('@/views/student/MyOrders.vue'), meta: { title: '我的订单' } },
      { path: 'attendance', name: 'StudentCoursesList', component: () => import('@/views/student/MyCourses.vue'), meta: { title: '在学课程' } },
      { path: 'attendance-records', name: 'StudentAttendanceRecords', component: () => import('@/views/student/MyAttendance.vue'), meta: { title: '考勤记录' } },
      { path: 'homework', name: 'StudentHomework', component: () => import('@/views/student/MyHomework.vue'), meta: { title: '我的作业' } },
      { path: 'homework/ai-report/:homeworkId', name: 'StudentAiReport', component: () => import('@/views/teacher/AiReportPage.vue'), meta: { title: 'AI评分报告' } },
      { path: 'exam', name: 'StudentExam', component: () => import('@/views/student/MyExam.vue'), meta: { title: '考级与比赛' } },
      { path: 'knowledge', name: 'StudentKnowledge', component: () => import('@/views/student/MyKnowledge.vue'), meta: { title: '学习知识库' } },
      { path: 'dunhuang', name: 'StudentDunhuang', component: () => import('@/views/student/DunhuangGallery.vue'), meta: { title: '敦煌展馆' } },
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/MyProfile.vue'), meta: { title: '个人信息' } },
      { path: 'feedback', name: 'StudentFeedback', component: () => import('@/views/student/Feedback.vue'), meta: { title: '互动反馈' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：检查登录状态和角色权限
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.token) {
    // 需要登录但未登录，跳转登录页
    next('/login')
  } else if (to.meta.role && to.meta.role !== userStore.role) {
    // 角色不匹配，跳转到登录页或相应首页
    next('/login')
  } else {
    next()
  }
})

export default router
