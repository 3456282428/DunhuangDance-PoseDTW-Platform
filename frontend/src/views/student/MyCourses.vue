<template>
  <div>
    <PageHeader title="在学课程" subtitle="我报名的所有课程" />

    <el-row :gutter="20" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="course in courses" :key="course.enrollmentId" style="margin-bottom:20px">
        <el-tooltip placement="top" :show-after="200">
          <template #content>
            <div style="max-width:260px;line-height:1.7">
              {{ course.className }}（{{ course.semester || '未排学期' }}）<br/>
              授课教师：{{ course.teacherName || '待定' }} 老师；状态：{{ course.status }}<br/>
              课程进度 = 已完成课时 ÷ 计划总课时（{{ course.attendedCount || 0 }}/{{ course.totalHours || 0 }}）。<br/>
              点击卡片查看我的课表。
            </div>
          </template>
          <el-card class="course-card course-link" shadow="hover" :body-style="{padding:'0'}" @click="goSchedule">
            <div class="course-cover" :style="{background: coverColor(course.className)}">
              <span class="cover-icon">{{ coverIcon(course.categoryName) }}</span>
              <span class="cover-cat">{{ course.categoryName }}</span>
              <span class="cover-status" :class="statusClass(course.status)">{{ course.status }}</span>
              <span class="cover-go">查看课表<el-icon><ArrowRight /></el-icon></span>
            </div>
            <div class="course-body">
              <div class="course-head">
                <h3 class="course-name">{{ course.className }}</h3>
                <span class="course-semester">{{ course.semester }}</span>
              </div>
              <div class="course-info">
                <div class="info-item">
                  <span class="info-label">授课教师</span>
                  <span class="info-value">{{ course.teacherName }} 老师</span>
                </div>
                <div class="info-item">
                  <span class="info-label">学费</span>
                  <span class="info-value price">¥{{ course.tuition }}</span>
                </div>
              </div>
              <div class="progress-block">
                <div class="progress-head">
                  <span>课程进度</span>
                  <span class="progress-num">{{ course.attendedCount }} / {{ course.totalHours }} 课时</span>
                </div>
                <el-progress
                  :percentage="progressPercent(course)"
                  :stroke-width="10"
                  :color="progressColor(course)"
                  :format="() => progressPercent(course) + '%'"
                />
              </div>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !courses.length" description="暂无在学课程" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCourses } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const goSchedule = () => router.push('/student/courses')

const courses = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyCourses()
    courses.value = res.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const progressPercent = (c) => {
  if (!c.totalHours) return 0
  return Math.min(100, Math.round((c.attendedCount || 0) / c.totalHours * 100))
}
const progressColor = (c) => {
  const p = progressPercent(c)
  if (p >= 80) return '#2c9678'
  if (p >= 50) return '#1685a9'
  return '#c89a2e'
}
const statusClass = (s) => {
  const map = { 在读: 'active', 结课: 'finished', 休学: 'suspended' }
  return map[s] || 'active'
}
function coverColor(name) {
  const colors = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b', '#fa709a', '#fcb69f']
  let h = 0
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) % colors.length
  return `linear-gradient(135deg, ${colors[h]}, ${colors[(h + 3) % colors.length]})`
}
function coverIcon(cat) {
  return { 乐器: '🎹', 舞蹈: '💃', 美术: '🎨', 声乐: '🎤' }[cat] || '🎵'
}

onMounted(fetchData)
</script>

<style scoped>
.course-card { border-radius: 14px; overflow: hidden; height: 100%; transition: transform .2s, box-shadow .2s; }
.course-link { cursor: pointer; }
.course-link:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgba(31,45,61,.14); }
.course-cover { position: relative; height: 120px; color: #fff; }
.cover-go {
  position: absolute; right: 12px; bottom: 10px;
  display: flex; align-items: center; gap: 2px;
  font-size: 12px; color: #fff; opacity: 0;
  transition: opacity .2s;
}
.course-link:hover .cover-go { opacity: .95; }
.cover-icon { position: absolute; left: 18px; top: 16px; font-size: 42px; }
.cover-cat { position: absolute; right: 12px; top: 12px; background: rgba(255,255,255,.25); padding: 2px 10px; border-radius: 12px; font-size: 13px; }
.cover-status { position: absolute; left: 12px; bottom: 12px; padding: 2px 10px; border-radius: 12px; font-size: 12px; background: rgba(255,255,255,.92); }
.cover-status.active { color: #1685a9; }
.cover-status.finished { color: #6b5a4a; }
.cover-status.suspended { color: #c89a2e; }
.course-body { padding: 16px; }
.course-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.course-name { font-size: 18px; color: #2c1810; margin: 0; }
.course-semester { color: #6b5a4a; font-size: 13px; }
.course-info { display: flex; justify-content: space-between; margin-bottom: 14px; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { color: #6b5a4a; font-size: 12px; }
.info-value { color: #2c1810; font-size: 15px; font-weight: 500; }
.info-value.price { color: #c89a2e; }
.progress-block { border-top: 1px dashed #e4d9c3; padding-top: 12px; }
.progress-head { display: flex; justify-content: space-between; margin-bottom: 8px; color: #6b5a4a; font-size: 13px; }
.progress-num { color: #6b5a4a; }
</style>
