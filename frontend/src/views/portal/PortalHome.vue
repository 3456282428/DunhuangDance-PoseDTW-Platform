<template>
  <div class="portal">
    <!-- 顶部导航 -->
    <header class="portal-nav">
      <div class="nav-inner">
        <div class="logo">
          <img v-if="org.logoUrl" class="logo-img" :src="org.logoUrl" alt="logo" />
          <span v-else class="logo-icon">{{ logoChar }}</span>
          <span class="logo-text">{{ brandName }}</span>
        </div>
        <nav class="nav-menu">
          <a href="#courses">课程</a>
          <a href="#about">关于我们</a>
          <a href="#my">我的试听</a>
        </nav>
        <div class="nav-actions">
          <!-- 已登录：点击用户按钮展示基础信息 -->
          <el-popover v-if="currentUser" placement="bottom-end" :width="264" trigger="click">
            <template #reference>
              <button class="user-btn" type="button">
                <span class="user-avatar">{{ avatarText }}</span>
                <span class="user-name">{{ currentUser.name }}</span>
                <el-icon class="user-arrow"><ArrowDown /></el-icon>
              </button>
            </template>
            <div class="user-card">
              <div class="uc-head">
                <span class="uc-avatar">{{ avatarText }}</span>
                <div class="uc-head-info">
                  <div class="uc-name">{{ currentUser.name }}</div>
                  <el-tag size="small" :type="currentUser.type === 'system' ? 'success' : 'warning'">
                    {{ currentUser.role }}
                  </el-tag>
                </div>
              </div>
              <div class="uc-row"><span class="uc-label">账号</span><span>{{ currentUser.account || '—' }}</span></div>
              <div class="uc-row"><span class="uc-label">手机号</span><span>{{ currentUser.phone || '—' }}</span></div>
              <div class="uc-actions">
                <el-button v-if="currentUser.type === 'system'" size="small" type="primary" @click="goWorkspace">进入工作台</el-button>
                <el-button size="small" :type="currentUser.type === 'system' ? 'danger' : 'default'" @click="handleLogout">
                  {{ currentUser.type === 'system' ? '退出登录' : '退出游客' }}
                </el-button>
              </div>
            </div>
          </el-popover>
          <!-- 未登录 -->
          <template v-else>
            <el-button text @click="loginVisible = true">游客登录</el-button>
            <el-button text @click="goSystemLogin">登录</el-button>
          </template>
          <el-button type="primary" round @click="openBooking(null)">预约试听</el-button>
        </div>
      </div>
    </header>

    <!-- 主视觉 -->
    <section class="hero">
      <el-carousel v-if="banners.length" class="hero-carousel" height="460px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="(b,i) in banners" :key="i">
          <img class="hero-img" :src="b" alt="banner" />
        </el-carousel-item>
      </el-carousel>
      <div v-else class="hero-bg"></div>
      <div class="hero-content">
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroSubtitle }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="scrollTo('#courses')">{{ btnBrowse }}</el-button>
          <el-button size="large" round plain @click="openBooking(null)">{{ btnTrial }}</el-button>
        </div>
        <div class="hero-stats">
          <div v-for="s in heroStats" :key="s.label" class="stat">
            <div class="num">{{ s.num }}</div>
            <div class="label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 课程浏览 -->
    <section id="courses" class="section">
      <div class="section-head">
        <h2>课程中心</h2>
        <p>按兴趣选择，开启艺术之旅</p>
      </div>

      <div class="filter-bar">
        <div class="filter-group">
          <span class="filter-label">类别</span>
          <el-radio-group v-model="filters.categoryId" @change="loadCourses">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button v-for="c in categories" :key="c.categoryId" :value="c.categoryId">
              {{ c.categoryName }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-group">
          <span class="filter-label">年龄段</span>
          <el-select v-model="filters.ageGroup" placeholder="全部年龄" clearable style="width:130px" @change="loadCourses">
            <el-option v-for="a in ageOptions" :key="a" :label="a" :value="a" />
          </el-select>
          <span class="filter-label" style="margin-left:12px">难度</span>
          <el-select v-model="filters.level" placeholder="全部难度" clearable style="width:120px" @change="loadCourses">
            <el-option v-for="l in levelOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </div>
        <div class="filter-search">
          <el-input v-model="filters.keyword" placeholder="搜索课程名称" clearable style="width:220px" @keyup.enter="loadCourses" @clear="loadCourses">
            <template #append>
              <el-button @click="loadCourses">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <el-row :gutter="20" v-loading="loading">
        <el-col :xs="24" :sm="12" :md="8" v-for="course in courses" :key="course.classId" style="margin-bottom:20px">
          <el-card class="course-card" shadow="hover" :body-style="{padding:'0'}">
            <div class="course-cover" :style="{background: coverColor(course.className)}">
              <span class="course-cover-icon">{{ coverIcon(course.categoryName) }}</span>
              <span class="course-category">{{ course.categoryName }}</span>
              <span class="course-level">{{ course.level }}</span>
            </div>
            <div class="course-body">
              <h3 class="course-name">{{ course.className }}</h3>
              <div class="course-tags">
                <el-tag size="small" type="info">{{ course.ageGroup || '全年龄' }}</el-tag>
                <el-tag size="small" type="success">{{ course.teacherName }} 老师</el-tag>
                <el-tag size="small">{{ course.totalHours }}课时</el-tag>
                <el-tag size="small" type="warning">单次{{ course.lessonDuration || 45 }}分钟</el-tag>
              </div>
              <p class="course-desc">{{ course.description }}</p>
              <div class="course-meta">
                <span class="course-price"><b>¥{{ course.tuition }}</b>/期</span>
                <span class="course-count">已报 {{ course.currentStudents }}/{{ course.maxStudents }}</span>
              </div>
              <div class="course-actions">
                <el-button size="small" @click="showDetail(course)">详情</el-button>
                <el-button size="small" type="primary" @click="openBooking(course)">预约试听</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && courses.length===0" description="暂无符合条件的课程" />
    </section>

    <!-- 我的试听 -->
    <section id="my" class="section alt">
      <div class="section-head">
        <h2>我的试听</h2>
        <p>{{ visitorLogged ? `已登录游客账号（${visitorName}）` : '登录游客账号后自动显示我的试听记录' }}</p>
      </div>
      <div class="my-booking">
        <div v-if="!visitorLogged" class="not-login">
          <el-button type="primary" @click="loginVisible = true">游客登录查看我的试听</el-button>
        </div>
        <template v-else>
          <el-table v-if="myBookings.length" :data="myBookings" stripe>
            <el-table-column prop="bookingId" label="编号" width="70" />
            <el-table-column prop="className" label="试听课程" />
            <el-table-column prop="teacherName" label="教师" />
            <el-table-column prop="preferredTime" label="期望时间" width="170" />
            <el-table-column label="状态" width="110">
              <template #default="{row}">
                <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="该账号暂无试听预约" />
        </template>
      </div>
    </section>

    <!-- 关于我们 -->
    <section id="about" class="section">
      <div class="section-head">
        <h2>关于我们</h2>
        <p>{{ org.name || '专业师资，用心教育' }}</p>
      </div>
      <div class="about-intro">
        <p>{{ org.introduction || '专业师资，用心教育，让孩子在艺术中快乐成长。' }}</p>
      </div>
      <div class="about-grid">
        <div class="about-card" v-for="f in aboutFeatures" :key="f.title">
          <div class="about-icon">{{ f.icon }}</div>
          <h4>{{ f.title }}</h4>
          <p>{{ f.desc }}</p>
        </div>
      </div>
      <div class="org-contact">
        <el-card shadow="never">
          <div class="contact-row"><span class="c-label">🏠 地址</span><span>{{ org.address || '—' }}</span></div>
          <div class="contact-row"><span class="c-label">📞 电话</span><span>{{ org.phone || '—' }}</span></div>
          <div class="contact-row"><span class="c-label">✉️ 邮箱</span><span>{{ org.email || '—' }}</span></div>
        </el-card>
      </div>
      <div class="faq">
        <h4>常见问题</h4>
        <el-collapse accordion>
          <el-collapse-item v-for="q in faqs" :key="q.q" :title="q.q">
            <div>{{ q.a }}</div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>

    <footer class="portal-footer">
      <div>© 2026 {{ org.name || '艺培学堂' }}</div>
    </footer>

    <!-- 游客登录弹窗 -->
    <el-dialog v-model="loginVisible" title="游客登录 / 注册" width="400px">
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="loginForm.name" placeholder="请输入称呼" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="loginForm.phone" placeholder="用于关联试听记录" maxlength="11" />
        </el-form-item>
      </el-form>
      <div class="login-tip">游客登录后即可预约试听并查看我的试听记录</div>
      <template #footer>
        <el-button @click="loginVisible = false">取消</el-button>
        <el-button type="primary" @click="doVisitorLogin">登录</el-button>
      </template>
    </el-dialog>

    <!-- 课程详情抽屉 -->
    <el-drawer v-model="detailVisible" title="课程详情" size="420px">
      <div v-if="currentDetail">
        <div class="detail-cover" :style="{background: coverColor(currentDetail.className)}">
          <span class="detail-icon">{{ coverIcon(currentDetail.categoryName) }}</span>
        </div>
        <h3 class="detail-name">{{ currentDetail.className }}</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="课程类别">{{ currentDetail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="授课教师">{{ currentDetail.teacherName }} 老师</el-descriptions-item>
          <el-descriptions-item label="上课教室">{{ currentDetail.classroomName }}</el-descriptions-item>
          <el-descriptions-item label="学期">{{ currentDetail.semester }}</el-descriptions-item>
          <el-descriptions-item label="总课时">{{ currentDetail.totalHours }} 课时</el-descriptions-item>
        <el-descriptions-item label="单次时长">{{ currentDetail.lessonDuration || 45 }} 分钟/节</el-descriptions-item>
          <el-descriptions-item label="年龄段">{{ currentDetail.ageGroup || '不限' }}</el-descriptions-item>
          <el-descriptions-item label="难度">{{ currentDetail.level || '不限' }}</el-descriptions-item>
          <el-descriptions-item label="费用">¥{{ currentDetail.tuition }}/期</el-descriptions-item>
        </el-descriptions>
        <div class="detail-desc">
          <h4>课程简介</h4>
          <p>{{ currentDetail.description || '暂无简介' }}</p>
        </div>
        <el-button type="primary" style="width:100%;margin-top:10px" @click="openBooking(currentDetail)">立即预约试听</el-button>
      </div>
    </el-drawer>

    <!-- 试听预约弹窗 -->
    <el-dialog v-model="bookingVisible" title="预约免费试听" width="500px" class="booking-dialog">
      <div class="booking-step" v-if="!visitorLogged">
        <p>预约试听前请先登录游客账号</p>
        <el-button type="primary" @click="bookingVisible = false; loginVisible = true">去登录</el-button>
      </div>
      <el-form v-else ref="bookFormRef" :model="bookingForm" :rules="bookRules" label-width="100px">
        <el-form-item label="课程" prop="classId">
          <el-select v-model="bookingForm.classId" placeholder="请选择课程" style="width:100%" @change="loadSchedules">
            <el-option v-for="c in courses" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" v-if="selectedCourse">
          <span class="teacher-line">{{ selectedCourse.teacherName }} 老师 · {{ selectedCourse.classroomName }}</span>
        </el-form-item>
        <el-form-item label="您的姓名" prop="name">
          <el-input v-model="bookingForm.name" placeholder="请输入称呼" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="bookingForm.phone" placeholder="用于接收预约确认" maxlength="11" />
        </el-form-item>
        <el-form-item label="期望时间" prop="preferredTime">
          <template v-if="scheduleSlots.length">
            <el-select v-model="bookingForm.preferredTime" placeholder="请选择可预约的排班时间" style="width:100%">
              <el-option v-for="s in scheduleSlots" :key="s.scheduleId" :label="slotLabel(s)" :value="s" />
            </el-select>
            <div class="slot-tip">以下为该课程可预约的上课时间</div>
          </template>
          <template v-else>
            <el-date-picker v-model="bookingForm.preferredTime" type="datetime" placeholder="选择期望试听时间" style="width:100%" />
            <div class="slot-tip">该课程暂无可选排班，可自行选择时间</div>
          </template>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" type="textarea" :rows="2" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getPortalCourses, getPortalCourseDetail, getPortalCourseSchedules,
  submitTrialBooking, getMyTrialBookings, getPortalOrg, getPortalContent
} from '@/api/portal'

const router = useRouter()
const userStore = useUserStore()
const courses = ref([])
const loading = ref(false)
const categories = ref([])
const ageOptions = ['3-6岁', '7-12岁', '12岁以上']
const levelOptions = ['入门', '初级', '中级', '高级']
const filters = reactive({ categoryId: null, ageGroup: '', level: '', keyword: '' })

const heroStats = ref([
  { num: '15+', label: '资深教师' },
  { num: '500+', label: '在读学员' },
  { num: '30+', label: '开设班级' }
])
const aboutFeatures = ref([
  { icon: '🎓', title: '专业师资', desc: '科班出身教师团队，多年一线教学经验' },
  { icon: '🏆', title: '考级指导', desc: '系统对接艺术考级与各类比赛' },
  { icon: '💡', title: '个性化教学', desc: '小班与一对一结合，因材施教' },
  { icon: '🏫', title: '环境优越', desc: '专业教室，安全舒适的学习氛围' }
])
// 默认敦煌宣传图（取色自莫高窟壁画：第217窟壁画、第428窟飞天、莫高窟外景、第257窟九色鹿本生）
const DEFAULT_DUNHUANG_BANNERS = [
  '/images/banner/banner1_mural.jpg',
  '/images/banner/banner2_apsaras.jpg',
  '/images/banner/banner3_mogao.jpg',
  '/images/banner/banner4_deer.jpg'
]
const banners = ref([...DEFAULT_DUNHUANG_BANNERS])
const heroTitle = ref('发现孩子的艺术天赋')
const heroSubtitle = ref('钢琴 · 舞蹈 · 美术 · 声乐 · 吉他 一站式艺术启蒙与进阶')
const btnBrowse = ref('浏览课程')
const btnTrial = ref('免费试听')
const brandName = computed(() => {
  if (!org.value?.name) return '艺培学堂'
  return org.value.name.replace(/艺术培训中心|培训中心|艺术中心/g, '')
})
const logoChar = computed(() => {
  if (org.value?.shortName) return org.value.shortName.slice(0, 1)
  return org.value?.name ? org.value.name.slice(0, 1) : '艺'
})
const faqs = [
  { q: '如何预约试听？', a: '在课程中心选择心仪课程，点击“预约试听”，登录游客账号后即可选择排班时间提交。' },
  { q: '试听是免费的吗？', a: '首次试听免费，可体验完整的一节课。' },
  { q: '请假制度是怎样的？', a: '学员可在“我的预约”中提交请假申请，请假课时顺延，由教务统一处理。' },
  { q: '如何报名缴费？', a: '试听后可直接在线报名缴费，或到校由教务老师协助办理。' }
]

// 关于我们（数据库）
const org = ref({})

// 游客账号
const visitorLogged = ref(false)
const visitorName = ref('')
const visitorPhone = ref('')

// 当前登录用户（系统账号优先，其次游客账号）
const roleLabel = r => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学员' }[r] || r)
const currentUser = computed(() => {
  if (userStore.token) {
    return {
      type: 'system',
      name: userStore.realName || userStore.username,
      account: userStore.username,
      phone: '',
      role: roleLabel(userStore.role)
    }
  }
  if (visitorLogged.value) {
    return { type: 'visitor', name: visitorName.value, account: '游客账号', phone: visitorPhone.value, role: '游客' }
  }
  return null
})
const avatarText = computed(() => currentUser.value ? currentUser.value.name.slice(0, 1) : '')

function goWorkspace() {
  const r = userStore.role
  router.push(r === 'ADMIN' ? '/admin/dashboard' : r === 'TEACHER' ? '/teacher/dashboard' : '/student/courses')
}

function handleLogout() {
  if (currentUser.value && currentUser.value.type === 'system') {
    userStore.logout()
    ElMessage.success('已退出登录')
  } else {
    visitorLogout()
  }
}
const loginVisible = ref(false)
const loginFormRef = ref(null)
const loginForm = reactive({ name: '', phone: '' })
const loginRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号', trigger: 'blur' }
  ]
}

// 我的试听
const myBookings = ref([])

// 详情
const detailVisible = ref(false)
const currentDetail = ref(null)

// 预约
const bookingVisible = ref(false)
const bookFormRef = ref(null)
const submitting = ref(false)
const scheduleSlots = ref([])
const bookingForm = reactive({ classId: null, name: '', phone: '', preferredTime: null, remark: '' })
const bookRules = {
  classId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号', trigger: 'blur' }
  ],
  preferredTime: [{ required: true, message: '请选择期望时间', trigger: 'change' }]
}

const selectedCourse = computed(() => courses.value.find(c => c.classId === bookingForm.classId))

function slotLabel(s) {
  return `${s.courseDate}  ${s.startTime.slice(0, 5)} - ${s.endTime.slice(0, 5)}`
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await getPortalCourses({
      categoryId: filters.categoryId || undefined,
      ageGroup: filters.ageGroup || undefined,
      level: filters.level || undefined,
      keyword: filters.keyword || undefined
    })
    courses.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getPortalCourses({})
    const set = new Map()
    ;(res.data || []).forEach(c => set.set(c.categoryId, { categoryId: c.categoryId, categoryName: c.categoryName }))
    categories.value = Array.from(set.values())
  } catch (e) {}
}

async function loadOrg() {
  try {
    const res = await getPortalOrg()
    if (res.data) {
      org.value = res.data
      if (org.value.name && heroStats.value[1]) heroStats.value[1].num = org.value.studentCount || heroStats.value[1].num
    }
  } catch (e) {}
}

async function loadPortalContent() {
  try {
    const res = await getPortalContent()
    const c = res.data
    if (!c) return
    if (Array.isArray(c.banners) && c.banners.length) banners.value = c.banners
    if (c.heroTitle) heroTitle.value = c.heroTitle
    if (c.heroSubtitle) heroSubtitle.value = c.heroSubtitle
    if (c.btnBrowse) btnBrowse.value = c.btnBrowse
    if (c.btnTrial) btnTrial.value = c.btnTrial
    if (Array.isArray(c.stats) && c.stats.length) heroStats.value = c.stats.filter(s => s && (s.num || s.label))
    if (Array.isArray(c.features) && c.features.length) aboutFeatures.value = c.features
  } catch (e) {}
}

async function showDetail(course) {
  const res = await getPortalCourseDetail(course.classId)
  currentDetail.value = res.data
  detailVisible.value = true
}

function openBooking(course) {
  bookingForm.classId = course ? course.classId : null
  bookingForm.name = visitorName.value
  bookingForm.phone = localStorage.getItem('visitor_phone') || ''
  bookingForm.preferredTime = null
  bookingForm.remark = ''
  scheduleSlots.value = []
  bookingVisible.value = true
  if (course && visitorLogged.value) loadSchedules()
}

async function loadSchedules() {
  scheduleSlots.value = []
  bookingForm.preferredTime = null
  if (!bookingForm.classId) return
  try {
    const res = await getPortalCourseSchedules(bookingForm.classId)
    scheduleSlots.value = res.data || []
  } catch (e) {}
}

async function submitBooking() {
  await bookFormRef.value.validate()
  let preferred = null
  if (bookingForm.preferredTime && bookingForm.preferredTime.scheduleId) {
    preferred = `${bookingForm.preferredTime.courseDate}T${bookingForm.preferredTime.startTime}`
  } else if (bookingForm.preferredTime) {
    preferred = new Date(bookingForm.preferredTime).toISOString()
  }
  submitting.value = true
  try {
    await submitTrialBooking({
      classId: bookingForm.classId,
      name: bookingForm.name,
      phone: bookingForm.phone,
      preferredTime: preferred,
      remark: bookingForm.remark
    })
    ElMessage.success('预约提交成功，教务老师将尽快与您联系')
    bookingVisible.value = false
    loadMyBookings()
  } finally {
    submitting.value = false
  }
}

async function loadMyBookings() {
  if (!visitorLogged.value) { myBookings.value = []; return }
  const res = await getMyTrialBookings(localStorage.getItem('visitor_phone'))
  myBookings.value = res.data || []
}

async function doVisitorLogin() {
  await loginFormRef.value.validate()
  visitorName.value = loginForm.name
  visitorPhone.value = loginForm.phone
  visitorLogged.value = true
  localStorage.setItem('visitor_name', loginForm.name)
  localStorage.setItem('visitor_phone', loginForm.phone)
  loginVisible.value = false
  ElMessage.success(`欢迎，${loginForm.name}`)
  loadMyBookings()
}

function visitorLogout() {
  visitorLogged.value = false
  visitorName.value = ''
  visitorPhone.value = ''
  localStorage.removeItem('visitor_name')
  localStorage.removeItem('visitor_phone')
  myBookings.value = []
  ElMessage.success('已退出游客账号')
}

function goSystemLogin() {
  router.push('/login')
}

function statusType(s) {
  if (s === '已确认') return 'success'
  if (s === '已取消') return 'info'
  return 'warning'
}
function coverColor(name) {
  // 敦煌矿物色板：石青、石绿、朱砂、土黄、赭石、孔雀蓝、铜绿
  const colors = ['#1685a9', '#2c9678', '#c13a2a', '#c89a2e', '#955539', '#0eb83a', '#5fae9c']
  let h = 0
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) % colors.length
  return `linear-gradient(135deg, ${colors[h]}, ${colors[(h + 3) % colors.length]})`
}
function coverIcon(cat) {
  return { 乐器: '🎹', 舞蹈: '💃', 美术: '🎨', 声乐: '🎤' }[cat] || '🎵'
}
function scrollTo(sel) {
  document.querySelector(sel)?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
  loadCourses()
  loadCategories()
  loadOrg()
  loadPortalContent()
  const name = localStorage.getItem('visitor_name')
  const phone = localStorage.getItem('visitor_phone')
  if (name && phone) {
    visitorName.value = name
    visitorPhone.value = phone
    visitorLogged.value = true
    loadMyBookings()
  }
})
</script>

<style scoped>
/* ============ 敦煌风门户：沙金 + 壁画赭石 + 朱砂 + 石青 + 石绿 ============ */
.portal { min-height: 100vh; background: var(--dh-cream, #f5f0e6); color: var(--dh-ink, #2c1810); }

/* 顶部导航：深茶褐底 + 金边（仿洞窟藻井边框） */
.portal-nav {
  position: sticky; top: 0; z-index: 100;
  background: linear-gradient(180deg, rgba(93,63,35,0.96) 0%, rgba(67,44,35,0.96) 100%);
  backdrop-filter: blur(6px);
  box-shadow: 0 2px 12px rgba(44,24,16,0.25);
  border-bottom: 2px solid rgba(201,168,76,0.5);
}
.nav-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; padding: 12px 20px; }
.logo { display: flex; align-items: center; gap: 10px; }
/* Logo 图标：朱砂 → 土黄 渐变 */
.logo-icon {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, var(--dh-cinnabar, #c13a2a), var(--dh-earth-yellow, #c89a2e));
  color: var(--dh-shell-white, #f5f0e6);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-family: var(--dh-font-display);
  box-shadow: 0 0 0 1px rgba(201,168,76,0.5);
}
.logo-text { font-size: 20px; font-weight: 700; color: var(--dh-gold-light, #e8d5a3); font-family: var(--dh-font-display); letter-spacing: 2px; }
.nav-menu { display: flex; gap: 28px; }
.nav-menu a { color: var(--dh-gold-light, #d4c5a9); text-decoration: none; font-size: 15px; transition: color .2s; }
.nav-menu a:hover { color: var(--dh-orpiment, #ffc90c); }
.nav-actions { display: flex; align-items: center; gap: 8px; }
.user-btn { display: flex; align-items: center; gap: 6px; border: 1px solid rgba(201,168,76,0.4); background: rgba(250,247,240,0.08); cursor: pointer; padding: 6px 12px; border-radius: 18px; transition: all .2s; color: var(--dh-gold-light); }
.user-btn:hover { background: rgba(201,168,76,0.18); }
.user-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, var(--dh-cinnabar), var(--dh-earth-yellow));
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600;
}
.user-name { color: var(--dh-gold-light, #e8d5a3); font-weight: 600; font-size: 15px; }
.user-arrow { color: var(--dh-sand, #d2a56d); font-size: 12px; }
.user-card { margin: -4px; }
.uc-head { display: flex; align-items: center; gap: 12px; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid var(--dh-border, #d4c5a9); }
.uc-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, var(--dh-cinnabar), var(--dh-earth-yellow));
  color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600;
}
.uc-name { font-size: 16px; font-weight: 600; color: var(--dh-ink, #2c1810); margin-bottom: 4px; }
.uc-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: var(--dh-text-light, #6b5a4a); }
.uc-label { color: var(--dh-ochre, #955539); }
.uc-actions { display: flex; gap: 8px; margin-top: 12px; }
.uc-actions .el-button { flex: 1; }

/* Hero 主视觉：仿敦煌壁画青绿山水 + 朱金璀璨（朱砂/石青/土黄渐变 + 云纹底） */
.hero { position: relative; color: #fff; text-align: center; padding: 90px 20px 70px; overflow: hidden; }
.hero-bg {
  position: absolute; inset: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(232,82,58,0.55) 0%, transparent 55%),
    radial-gradient(ellipse at 80% 70%, rgba(22,133,169,0.55) 0%, transparent 55%),
    linear-gradient(135deg, #5d3f23 0%, #955539 35%, #c89a2e 70%, #c13a2a 100%);
}
.hero-content { position: relative; z-index: 1; }
.hero-content h1 { font-size: 46px; margin-bottom: 16px; font-family: var(--dh-font-display); letter-spacing: 3px; text-shadow: 0 2px 16px rgba(44,24,16,0.6); }
.hero-content p { font-size: 18px; opacity: .94; margin-bottom: 30px; letter-spacing: 1px; }
.hero-actions { display: flex; justify-content: center; gap: 12px; margin-bottom: 44px; }
.hero-stats { display: flex; justify-content: center; gap: 60px; }
.stat .num { font-size: 34px; font-weight: 700; color: var(--dh-orpiment, #ffc90c); text-shadow: 0 2px 8px rgba(44,24,16,0.5); }
.stat .label { font-size: 14px; opacity: .9; margin-top: 4px; color: var(--dh-gold-light, #e8d5a3); }

/* 课程中心区块 */
.section { max-width: 1200px; margin: 0 auto; padding: 56px 20px; }
.section.alt {
  /* 金线山水壁画纹理：米白罩层 88% + 壁画隐约可见 */
  background:
    linear-gradient(180deg, rgba(226, 216, 205, 0.88) 0%, rgba(245, 240, 230, 0.88) 100%),
    url('/texture/bg_gold.jpg') center / cover fixed no-repeat;
  margin: 0 auto; max-width: none;
  padding-left: calc((100% - 1200px)/2 + 20px); padding-right: calc((100% - 1200px)/2 + 20px);
}
.section-head { text-align: center; margin-bottom: 32px; }
.section-head h2 {
  font-size: 30px; color: var(--dh-ink, #2c1810);
  font-family: var(--dh-font-display); letter-spacing: 4px;
  position: relative; display: inline-block;
}
.section-head h2::after {
  content: ''; display: block; width: 60px; height: 3px;
  background: linear-gradient(90deg, var(--dh-cinnabar), var(--dh-earth-yellow), var(--dh-azurite));
  margin: 10px auto 0; border-radius: 2px;
}
.section-head p { color: var(--dh-text-light, #6b5a4a); margin-top: 12px; }

/* 筛选条：米纸底 + 金边 */
.filter-bar { background: var(--dh-card-bg, #faf7f0); border: 1px solid var(--dh-border, #d4c5a9); border-radius: 12px; padding: 16px 20px; margin-bottom: 24px; display: flex; flex-wrap: wrap; align-items: center; gap: 16px; box-shadow: 0 2px 8px rgba(92,63,35,0.05); }
.filter-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.filter-label { color: var(--dh-ochre, #955539); font-size: 14px; font-weight: 600; }
.filter-search { margin-left: auto; }

/* 课程卡片：矿物色封面 + 米纸底 + 金边 */
.course-card { border-radius: 14px; overflow: hidden; height: 100%; border: 1px solid var(--dh-border, #d4c5a9) !important; transition: transform .2s, box-shadow .2s; }
.course-card:hover { transform: translateY(-3px); box-shadow: 0 8px 22px rgba(92,63,35,0.18) !important; }
.course-cover { position: relative; height: 130px; color: #fff; }
.course-cover-icon { position: absolute; left: 18px; top: 18px; font-size: 44px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3)); }
.course-category { position: absolute; right: 12px; top: 12px; background: rgba(250,247,240,0.28); padding: 2px 10px; border-radius: 12px; font-size: 13px; backdrop-filter: blur(2px); }
.course-level { position: absolute; right: 12px; bottom: 12px; background: rgba(44,24,16,0.4); padding: 2px 10px; border-radius: 12px; font-size: 12px; }
.course-body { padding: 16px; background: var(--dh-card-bg, #faf7f0); }
.course-name { font-size: 18px; color: var(--dh-ink, #2c1810); margin-bottom: 10px; font-weight: 600; }
.course-tags { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.course-desc { color: var(--dh-text-light, #6b5a4a); font-size: 13px; line-height: 1.6; height: 40px; overflow: hidden; margin-bottom: 12px; }
.course-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.course-price b { color: var(--dh-vermilion, #d94028); font-size: 20px; }
.course-count { color: var(--dh-text-light, #6b5a4a); font-size: 13px; }
.course-actions { display: flex; gap: 8px; }

/* 我的试听 */
.my-booking { max-width: 860px; margin: 0 auto; background: var(--dh-card-bg, #faf7f0); border: 1px solid var(--dh-border, #d4c5a9); border-radius: 14px; padding: 24px; box-shadow: 0 2px 12px rgba(92,63,35,0.06); }
.not-login { text-align: center; padding: 30px 0; }

/* 关于我们 */
.about-intro { max-width: 760px; margin: 0 auto 28px; text-align: center; color: var(--dh-text-light, #6b5a4a); line-height: 1.9; }
.about-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 20px; margin-bottom: 24px; }
.about-card {
  text-align: center;
  background: var(--dh-card-bg, #faf7f0);
  border: 1px solid var(--dh-border, #d4c5a9);
  border-radius: 14px; padding: 28px 18px;
  transition: transform .2s, box-shadow .2s;
}
.about-card:hover { transform: translateY(-3px); box-shadow: 0 8px 22px rgba(92,63,35,0.12); }
.about-icon { font-size: 40px; margin-bottom: 12px; }
.about-card h4 { color: var(--dh-cinnabar, #c13a2a); margin-bottom: 8px; font-family: var(--dh-font-display); letter-spacing: 1px; }
.about-card p { color: var(--dh-text-light, #6b5a4a); font-size: 13px; line-height: 1.6; }
.org-contact { max-width: 760px; margin: 0 auto 32px; }
.contact-row { display: flex; gap: 16px; padding: 10px 0; border-bottom: 1px dashed var(--dh-border, #d4c5a9); color: var(--dh-text-light, #6b5a4a); }
.contact-row:last-child { border-bottom: none; }
.c-label { width: 120px; color: var(--dh-ink, #2c1810); font-weight: 600; }
.faq { max-width: 760px; margin: 0 auto; }
.faq h4 { text-align: center; color: var(--dh-ink, #2c1810); margin-bottom: 16px; font-family: var(--dh-font-display); }

/* 页脚：深色飞天壁画纹理 + 金边线 */
.portal-footer {
  background:
    linear-gradient(rgba(44, 24, 16, 0.62), rgba(44, 24, 16, 0.62)),
    url('/texture/bg_feitian.jpg') center / cover no-repeat;
  color: var(--dh-sand, #d2a56d); text-align: center; padding: 24px; font-size: 14px;
  border-top: 2px solid rgba(201,168,76,0.4);
}

/* 课程详情抽屉 */
.detail-cover { height: 150px; border-radius: 12px; color: #fff; position: relative; margin-bottom: 16px; }
.detail-icon { position: absolute; left: 18px; top: 18px; font-size: 56px; filter: drop-shadow(0 2px 6px rgba(0,0,0,0.3)); }
.detail-name { color: var(--dh-ink, #2c1810); margin-bottom: 16px; font-family: var(--dh-font-display); }
.detail-desc { margin-top: 20px; }
.detail-desc h4 { color: var(--dh-cinnabar, #c13a2a); margin-bottom: 8px; }
.detail-desc p { color: var(--dh-text-light, #6b5a4a); line-height: 1.7; }

.teacher-line { color: var(--dh-azurite, #1685a9); }
.slot-tip { color: var(--dh-text-light, #6b5a4a); font-size: 12px; margin-top: 6px; }
.login-tip { color: var(--dh-text-light, #6b5a4a); font-size: 13px; text-align: center; margin-bottom: 6px; }
.booking-step { text-align: center; padding: 30px 0; }
.booking-step p { margin-bottom: 14px; color: var(--dh-text-light, #6b5a4a); }
@media (max-width: 768px) { .about-grid { grid-template-columns: repeat(2,1fr); } .hero-content h1 { font-size: 32px; } }
.logo-img { width: 36px; height: 36px; border-radius: 10px; object-fit: cover; }
.hero-carousel { position: absolute; inset: 0; z-index: 0; }
.hero-carousel :deep(.el-carousel__item) { background: #3d2517; }
.hero-img { width: 100%; height: 100%; object-fit: cover; }
.hero .hero-content { z-index: 2; text-shadow: 0 2px 12px rgba(0,0,0,.45); }
</style>
