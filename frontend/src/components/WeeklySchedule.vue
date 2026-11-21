<template>
  <div class="ws-wrap">
    <!-- 顶部周次导航 -->
    <div class="ws-nav">
      <div class="ws-nav-left">
        <el-button size="small" @click="changeWeek(-1)">‹ 上一周</el-button>
        <span class="ws-week-label">{{ weekLabel }}</span>
        <el-button size="small" @click="changeWeek(1)">下一周 ›</el-button>
        <el-button v-if="!isCurrentWeek" size="small" type="primary" @click="backToCurrent">切回本周</el-button>
      </div>
      <div class="ws-nav-tip">45分钟/节 · 课间10分钟</div>
    </div>

    <div v-if="!weekSchedules.length" class="ws-empty-hint">本周暂无课程安排</div>

    <!-- 课表网格（固定每日时段框架） -->
    <div class="ws-grid">
      <div class="ws-corner"></div>
      <div v-for="d in weekDays" :key="d.date" class="ws-day-head">
        <div class="ws-day-label">{{ d.weekLabel }}</div>
        <div class="ws-day-date">{{ d.dateLabel }}</div>
      </div>

      <template v-for="(row, ri) in rows" :key="ri">
        <!-- 休息时段横条 -->
        <div v-if="row.type === 'break'" class="ws-break">
          {{ row.label }}
        </div>
        <!-- 上午/下午/晚上 分段标题 -->
        <div v-else-if="row.type === 'header'" class="ws-section">
          {{ row.label }}
        </div>
        <!-- 上课时段行 -->
        <template v-else>
          <div class="ws-time">
            <div class="ws-time-start">{{ shortTime(row.start) }}</div>
            <div class="ws-time-end">{{ shortTime(row.end) }}</div>
          </div>
          <div v-for="d in weekDays" :key="d.date + row.start" class="ws-cell">
            <div
              v-for="item in itemsOf(row, d.date)"
              :key="item.scheduleId"
              class="ws-course"
              :style="{ backgroundColor: colorOf(item.categoryName) }"
              @click="openDetail(item)"
            >
              <div class="ws-course-name">{{ item.className }}</div>
              <div class="ws-course-room">{{ item.classroomName }}</div>
            </div>
          </div>
        </template>
      </template>
    </div>

    <!-- 课程详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail?.className || '课程详情'" width="420px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="课程">{{ detail.className }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ detail.courseDate }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ shortTime(detail.startTime) }} - {{ shortTime(detail.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="教室">{{ detail.classroomName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="教师">{{ detail.teacherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  schedules: { type: Array, default: () => [] }
})

const WEEK_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const PALETTE = ['#5B8FF9', '#61C0A8', '#F6BD16', '#E8684A', '#6DC8EC', '#9270CA', '#FF9D4D', '#269A99']

// 机构每日标准时段：45分钟/节 + 课间10分钟
const SLOT_SECTIONS = [
  {
    label: '上午',
    slots: [
      { label: '第1节', start: '08:00', end: '08:45' },
      { label: '第2节', start: '08:55', end: '09:40' },
      { label: '第3节', start: '09:50', end: '10:35' },
      { label: '第4节', start: '10:45', end: '11:30' },
      { label: '第5节', start: '11:40', end: '12:25' }
    ]
  },
  { label: '午休 12:25 - 14:30', break: true },
  {
    label: '下午',
    slots: [
      { label: '第6节', start: '14:30', end: '15:15' },
      { label: '第7节', start: '15:25', end: '16:10' },
      { label: '第8节', start: '16:20', end: '17:05' },
      { label: '第9节', start: '17:15', end: '18:00' }
    ]
  },
  { label: '晚饭 18:00 - 19:30', break: true },
  {
    label: '晚上',
    slots: [
      { label: '第10节', start: '19:30', end: '20:15' },
      { label: '第11节', start: '20:25', end: '21:10' },
      { label: '第12节', start: '21:20', end: '22:05' }
    ]
  }
]

// 扁平化所有行：时段行 + 休息横条
const rows = computed(() => {
  const arr = []
  SLOT_SECTIONS.forEach(sec => {
    if (sec.break) {
      arr.push({ type: 'break', label: sec.label })
    } else {
      arr.push({ type: 'header', label: sec.label })
      sec.slots.forEach(s => arr.push({ type: 'slot', start: s.start, end: s.end, label: s.label }))
    }
  })
  return arr
})

const parseDate = (s) => {
  const d = new Date(s + 'T00:00:00')
  return isNaN(d.getTime()) ? null : d
}
const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const mondayOf = (d) => {
  const x = new Date(d)
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7))
  return x
}
const shortTime = (t) => (t ? String(t).slice(0, 5) : '')

// 当前周起点（本周一）
const anchor = ref(mondayOf(new Date()))
const weekStart = computed(() => mondayOf(anchor.value))
const weekEnd = computed(() => {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + 6)
  return d
})
const isCurrentWeek = computed(() => fmt(weekStart.value) === fmt(mondayOf(new Date())))

const weekDays = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart.value)
    d.setDate(d.getDate() + i)
    return {
      date: fmt(d),
      weekLabel: WEEK_LABELS[i],
      dateLabel: `${d.getMonth() + 1}月${d.getDate()}日`
    }
  })
)

// 该周排课
const weekSchedules = computed(() => {
  const startKey = fmt(weekStart.value)
  const endKey = fmt(weekEnd.value)
  return props.schedules.filter(s => s.courseDate >= startKey && s.courseDate <= endKey)
})

const itemsOf = (row, date) =>
  weekSchedules.value.filter(s => s.courseDate === date && shortTime(s.startTime) === row.start)

// 类别配色
const categoryOrder = computed(() => {
  const arr = []
  props.schedules.forEach(s => {
    if (s.categoryName && !arr.includes(s.categoryName)) arr.push(s.categoryName)
  })
  return arr
})
const colorOf = (cat) => {
  const idx = categoryOrder.value.indexOf(cat)
  return PALETTE[idx >= 0 ? idx % PALETTE.length : 0]
}

const changeWeek = (delta) => {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + delta * 7)
  anchor.value = d
}
const backToCurrent = () => {
  anchor.value = mondayOf(new Date())
}

const weekLabel = computed(() => {
  const s = weekStart.value
  const e = weekEnd.value
  const startLabel = `${s.getMonth() + 1}月${s.getDate()}日`
  const endLabel = `${e.getMonth() + 1}月${e.getDate()}日`
  const sameMonth = s.getMonth() === e.getMonth()
  return sameMonth ? `${startLabel} - ${e.getDate()}日` : `${startLabel} - ${endLabel}`
})

// 详情
const detailVisible = ref(false)
const detail = ref(null)
const openDetail = (item) => {
  detail.value = item
  detailVisible.value = true
}
</script>

<style scoped>
.ws-wrap {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4d9c3;
  overflow: hidden;
}
.ws-nav {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f2f5;
}
.ws-nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ws-nav-tip {
  font-size: 12px;
  color: #6b5a4a;
  background: #f1e9d8;
  border-radius: 12px;
  padding: 2px 10px;
}
.ws-week-label {
  font-size: 16px;
  font-weight: 600;
  color: #2c1810;
}
.ws-empty-hint {
  text-align: center;
  color: #6b5a4a;
  padding: 12px;
  font-size: 13px;
}
.ws-grid {
  display: grid;
  grid-template-columns: 64px repeat(7, 1fr);
  grid-auto-rows: minmax(52px, auto);
}
.ws-corner {
  background: #f1e9d8;
  border-right: 1px solid #e4d9c3;
  border-bottom: 1px solid #e4d9c3;
}
.ws-day-head {
  text-align: center;
  padding: 8px 0;
  background: #f1e9d8;
  border-bottom: 1px solid #e4d9c3;
  border-right: 1px solid #e4d9c3;
}
.ws-day-label {
  font-size: 15px;
  font-weight: 600;
  color: #2c1810;
}
.ws-day-date {
  font-size: 12px;
  color: #6b5a4a;
  margin-top: 2px;
}
.ws-break {
  grid-column: 1 / -1;
  background: #fafbfc;
  border-bottom: 1px solid #e4d9c3;
  text-align: center;
  color: #a89880;
  font-size: 12px;
  padding: 4px;
  letter-spacing: 2px;
}
.ws-section {
  grid-column: 1 / -1;
  background: #304156;
  color: #fff;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 6px;
  padding: 5px;
  border-bottom: 1px solid #e4d9c3;
}
.ws-time {
  background: #fafbfc;
  border-right: 1px solid #e4d9c3;
  border-bottom: 1px solid #e4d9c3;
  text-align: center;
  padding-top: 6px;
  color: #6b5a4a;
}
.ws-time-start { font-size: 12px; }
.ws-time-end { font-size: 12px; margin-top: 2px; color: #a89880; }
.ws-cell {
  border-right: 1px solid #e4d9c3;
  border-bottom: 1px solid #e4d9c3;
  padding: 4px;
  min-height: 52px;
}
.ws-cell:nth-child(8n) { border-right: none; }
.ws-course {
  border-radius: 6px;
  padding: 6px 8px;
  color: #fff;
  cursor: pointer;
  height: 100%;
  box-sizing: border-box;
  transition: filter 0.15s;
}
.ws-course:hover { filter: brightness(1.08); }
.ws-course-name { font-size: 13px; font-weight: 600; line-height: 1.3; }
.ws-course-room { font-size: 12px; opacity: 0.9; margin-top: 2px; }
</style>
