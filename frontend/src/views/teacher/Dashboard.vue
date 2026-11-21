<template>
  <div>
    <PageHeader title="教师首页" subtitle="今日课表、作业批改与待办事项" />

    <el-row :gutter="16">
      <el-col :span="8" v-for="item in stats" :key="item.title" style="margin-bottom:16px">
        <el-tooltip placement="top" :show-after="200">
          <template #content>
            <div style="max-width:240px;line-height:1.7">{{ item.tip }}<br/>点击进入「{{ item.targetName }}」</div>
          </template>
          <el-card shadow="hover" class="jump-card" :body-style="{ padding: '18px 20px' }" @click="go(item.to)">
            <div class="stat-card">
              <div class="stat-icon" :style="{ background: item.color }">
                <el-icon :size="22" color="#fff"><component :is="item.icon" /></el-icon>
              </div>
              <div>
                <div class="stat-title">{{ item.title }}</div>
                <div class="stat-value">{{ item.value }}<span class="stat-unit">{{ item.unit }}</span></div>
              </div>
              <el-icon class="jump-arrow"><ArrowRight /></el-icon>
            </div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-divider content-position="left">今日课程（{{ todaySchedule.length }}）</el-divider>
    <el-table :data="todaySchedule" stripe class="jump-table" @row-click="go('/teacher/attendance')">
      <el-table-column prop="courseDate" label="日期" width="110" />
      <el-table-column prop="className" label="班级" min-width="130" />
      <el-table-column label="时间" width="140">
        <template #default="{ row }">{{ fmt(row.startTime) }} - {{ fmt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="classroomName" label="教室" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '已完成' ? 'success' : row.status === '已取消' ? 'info' : 'warning'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <template #empty>今日暂无课程</template>
    </el-table>

    <el-divider content-position="left">待办事项（{{ todos.length }}）</el-divider>
    <el-table :data="todos" stripe class="jump-table" @row-click="go('/teacher/homework')">
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="title" label="内容" min-width="180" />
      <el-table-column prop="description" label="说明" min-width="160" />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <template #empty>暂无待办，所有作业已批改完</template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherDashboardSummary, getTeacherTodaySchedule, getTeacherTodos } from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { Calendar, School, Timer, Document, EditPen, CircleCheck, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const go = (path) => router.push(path)

const stats = ref([])
const todaySchedule = ref([])
const todos = ref([])

const fmt = (t) => t ? String(t).slice(0, 5) : ''

onMounted(async () => {
  try {
    const summary = await getTeacherDashboardSummary()
    const d = summary.data || {}
    stats.value = [
      { title: '今日课程', value: d.todayCourseCount ?? 0, unit: ' 节', icon: Calendar, color: '#1685a9', tip: '今天需要授课的课节数量（已取消的不计）', to: '/teacher/schedule', targetName: '课程排班管理' },
      { title: '授课班级', value: d.classCount ?? 0, unit: ' 个', icon: School, color: '#2c9678', tip: '我当前负责授课的班级数量', to: '/teacher/profile', targetName: '个人授课主页' },
      { title: '累计课时', value: Math.round((d.totalHours ?? 0) * 10) / 10, unit: ' 小时', icon: Timer, color: '#955539', tip: '已完成授课的累计课时，已取消排课不计', to: '/teacher/achievement', targetName: '教学成果统计' },
      { title: '布置作业', value: d.homeworkCount ?? 0, unit: ' 次', icon: Document, color: '#c89a2e', tip: '我累计为各班级布置的作业次数', to: '/teacher/homework', targetName: '作业管理' },
      { title: '待批改', value: d.pendingGradeCount ?? 0, unit: ' 份', icon: EditPen, color: '#c13a2a', tip: '学员已提交、但我尚未批改的作业份数', to: '/teacher/homework', targetName: '作业管理' },
      { title: '已批改', value: d.gradedCount ?? 0, unit: ' 份', icon: CircleCheck, color: '#5fae9c', tip: '我已经完成批改的作业份数', to: '/teacher/homework', targetName: '作业管理' }
    ]
    const scheduleRes = await getTeacherTodaySchedule()
    todaySchedule.value = scheduleRes.data || []
    const todosRes = await getTeacherTodos()
    todos.value = todosRes.data || []
  } catch (e) {}
})
</script>

<style scoped>
.stat-card { display: flex; align-items: center; gap: 14px; }
.jump-card {
  cursor: pointer; transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
  border: 1px solid var(--dh-border, #d4c5a9) !important;
}
.jump-card:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(92,63,35,0.12) !important; border-color: var(--dh-gold, #c9a84c) !important; }
.jump-arrow { margin-left: auto; color: var(--dh-sand, #d2a56d); }
.jump-table :deep(.el-table__row) { cursor: pointer; }
.stat-icon {
  width: 46px; height: 46px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(44,24,16,0.15), inset 0 0 0 1px rgba(255,255,255,0.15);
}
.stat-title { color: var(--dh-text-light, #6b5a4a); font-size: 13px; }
.stat-value { font-size: 26px; font-weight: bold; margin-top: 4px; color: var(--dh-ink, #2c1810); font-family: var(--dh-font-display); }
.stat-unit { font-size: 13px; font-weight: normal; color: var(--dh-text-light, #6b5a4a); margin-left: 2px; }
</style>
